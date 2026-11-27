package com.example.soleilracingproject.features.analysis.data.backup

import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.model.analysis.Session
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.options
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.job
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.serializer
import kotlin.time.Clock

enum class SessionBackupStatus { PENDING, UPLOADING, CONFIRMED, FAILED }

internal interface BackupAccount {
    fun currentUid(): String?
    val uidChanges: Flow<String?>
}

internal class FirebaseBackupAccount : BackupAccount {
    override fun currentUid(): String? = Firebase.auth.currentUser?.uid
    override val uidChanges = flow { emitAll(Firebase.auth.authStateChanged.map { it?.uid }) }
}

internal interface BackupTransport {
    suspend fun awaitConnection(): Boolean
    suspend fun read(path: String): String?
    // Returns only after a server acknowledgement.
    suspend fun write(path: String, value: String)
}

internal class FirebaseBackupTransport : BackupTransport {
    private fun database() = Firebase.database(
        Firebase.options.databaseUrl?.takeIf { it.isNotBlank() }
            ?: error("Firebase Realtime Database URL is missing from the app configuration")
    )

    override suspend fun awaitConnection(): Boolean = withTimeoutOrNull(5_000) {
        database().reference(".info/connected").valueEvents.first {
            val connected = it.value
            connected == true || connected?.toString() == "1"
        }
        true
    } ?: false

    override suspend fun read(path: String): String? =
        database().reference(path).get().let { snapshot ->
            if (snapshot.exists) snapshot.value as String else null
        }

    // GitLive's inline overload targets JVM 17; keep Soleil's Android target at JVM 11.
    @Suppress("DEPRECATION")
    override suspend fun write(path: String, value: String) {
        database().reference(path).setValue(String.serializer(), value, encodeDefaults = false)
    }
}

class SessionBackupRepository internal constructor(
    private val local: AnalysisRepository,
    private val account: BackupAccount,
    private val remote: BackupTransport,
    retryIntervalMs: Long = 30_000,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _statuses = MutableStateFlow<Map<String, SessionBackupStatus>>(emptyMap())
    val statuses = _statuses.asStateFlow()

    init {
        scope.launch {
            account.uidChanges.retryWhen { _, _ -> delay(retryIntervalMs); true }
                .distinctUntilChanged().collectLatest { uid ->
                    _statuses.value = emptyMap()
                    if (uid != null) {
                        val pending = local.observePendingBackupIds(SessionBackupFormat.prefix(uid)).distinctUntilChanged()
                        val retryTicks = flow {
                            while (true) {
                                emit(Unit)
                                delay(retryIntervalMs)
                            }
                        }
                        combine(pending, retryTicks) { ids, _ -> ids }.conflate().collect { ids ->
                            backUpPending(uid, ids)
                        }
                    }
                }
        }
    }

    private suspend fun backUpPending(uid: String, ids: List<String>) {
        val pending = ids.toSet()
        _statuses.update { statuses ->
            statuses.filter { (id, status) -> id in pending || status == SessionBackupStatus.CONFIRMED }
        }
        if (ids.isEmpty()) return
        // Bind unowned local rows even when the logger's Wi-Fi has no Internet.
        val paths = ids.mapNotNull { id ->
            try {
                checkAccount(uid)
                val path = SessionBackupFormat.path(uid, id)
                local.claimBackup(id, path)
                _statuses.update { if (id in it) it else it + (id to SessionBackupStatus.PENDING) }
                id to path
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setStatus(id, SessionBackupStatus.FAILED)
                null
            }
        }
        if (paths.isEmpty()) return
        val connected = try {
            remote.awaitConnection()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            paths.forEach { (id, _) -> setStatus(id, SessionBackupStatus.FAILED) }
            return
        }
        if (!connected) return

        for ((id, path) in paths) {
            try {
                checkAccount(uid)
                val session = local.readPendingBackup(id, path) ?: continue
                setStatus(id, SessionBackupStatus.UPLOADING)
                upload(uid, path, session)
                local.confirmBackup(id, path)
                setStatus(id, SessionBackupStatus.CONFIRMED)
            } catch (_: TimeoutCancellationException) {
                setStatus(id, SessionBackupStatus.FAILED)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setStatus(id, SessionBackupStatus.FAILED)
            }
        }
    }

    private suspend fun upload(uid: String, path: String, session: Session) {
        val completePath = "$path/complete"
        read(uid, completePath)?.let { existing ->
            readManifest(existing, session.id)
            // Firebase get() can return cached or locally queued data. Re-acknowledge the
            // identical immutable marker before changing the durable local sync state.
            write(uid, completePath, existing)
            return
        }

        session.laps.forEachIndexed { lapIndex, lap ->
            lap.telemetryPoints.asSequence().chunked(SessionBackupFormat.CHUNK_SIZE).forEachIndexed { index, points ->
                write(uid, "$path/chunks/$lapIndex/$index", SessionBackupFormat.encodeChunk(points))
            }
        }
        val manifest = BackupManifest(
            SessionBackupFormat.VERSION, session.id, session.title, session.vehicleId, session.track,
            session.dateEpoch, Clock.System.now().toEpochMilliseconds(), session.laps.map(::BackupLap),
        )
        write(uid, completePath, Json.encodeToString(manifest))
    }

    /** Reads a complete backup for the current account. The caller decides whether to import it. */
    suspend fun readBackedUpSession(sessionId: String): Session? {
        val uid = account.currentUid() ?: return null
        val path = SessionBackupFormat.path(uid, sessionId)
        val completePath = "$path/complete"
        val marker = read(uid, completePath) ?: return null
        val manifest = readManifest(marker, sessionId)
        val laps = manifest.laps.mapIndexed { lapIndex, lap ->
            check(lap.sampleCount >= 0) { "Invalid backup sample count" }
            val points = buildList {
                var remaining = lap.sampleCount
                var chunkIndex = 0
                while (remaining > 0) {
                    val data = checkNotNull(read(uid, "$path/chunks/$lapIndex/$chunkIndex")) {
                        "Missing backup telemetry chunk"
                    }
                    val chunk = SessionBackupFormat.decodeChunk(data)
                    check(chunk.size == minOf(SessionBackupFormat.CHUNK_SIZE, remaining)) {
                        "Invalid backup telemetry chunk size"
                    }
                    addAll(chunk)
                    remaining -= chunk.size
                    chunkIndex++
                }
            }
            lap.toLap(manifest.sessionId, points)
        }
        // get() may serve a cached marker. Verify completion with the same idempotent server acknowledgement.
        write(uid, completePath, marker)
        checkAccount(uid)
        return manifest.toSession(laps)
    }

    private fun readManifest(value: String, sessionId: String): BackupManifest =
        Json.decodeFromString<BackupManifest>(value).also {
            check(it.version == SessionBackupFormat.VERSION && it.sessionId == sessionId) {
                "Unsupported or mismatched session backup"
            }
        }

    private fun checkAccount(uid: String) {
        if (account.currentUid() != uid) throw CancellationException("Backup account changed")
    }

    private suspend fun read(uid: String, path: String): String? = withTimeout(15_000) {
        checkAccount(uid)
        remote.read(path)
    }

    private suspend fun write(uid: String, path: String, value: String) = withTimeout(15_000) {
        checkAccount(uid)
        check(value.encodeToByteArray().size <= SessionBackupFormat.MAX_NODE_BYTES) { "Backup node too large" }
        remote.write(path, value)
    }

    private fun setStatus(id: String, status: SessionBackupStatus) {
        _statuses.update { it + (id to status) }
    }

    internal fun close(): Job {
        scope.cancel()
        return scope.coroutineContext.job
    }
}
