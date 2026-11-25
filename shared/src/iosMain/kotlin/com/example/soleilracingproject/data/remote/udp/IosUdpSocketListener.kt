package com.example.soleilracingproject.data.remote.udp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSLock
import platform.posix.AF_INET
import platform.posix.EAGAIN
import platform.posix.EINTR
import platform.posix.EWOULDBLOCK
import platform.posix.INADDR_ANY
import platform.posix.IPPROTO_UDP
import platform.posix.MSG_DONTWAIT
import platform.posix.POLLIN
import platform.posix.SOCK_DGRAM
import platform.posix.bind
import platform.posix.close
import platform.posix.errno
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.recv
import platform.posix.sockaddr_in
import platform.posix.socket

@OptIn(ExperimentalForeignApi::class)
internal class IosUdpSocketListener : UdpSocketListener {
    // One hot stream for all observers. Keep the newest packets when collectors lag.
    // With no subscribers, packets are discarded. New collectors get no replay.
    private val packets = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val scope = CoroutineScope(Dispatchers.IO)
    private val stateLock = NSLock()
    private val socketLifetime = Mutex()
    private var receiver: Receiver? = null

    override suspend fun beginListening(port: Int) {
        require(port in 1..65535) { "Invalid UDP port" }
        val callerContext = currentCoroutineContext()
        stateLock.lock()
        try {
            callerContext.ensureActive()
            val previous = receiver
            if (previous?.port == port && previous.job.isActive) return

            previous?.job?.cancel(CancellationException("iOS UDP listener restarting"))
            val job = scope.launch {
                // A replacement waits until the previous owner has closed its descriptor.
                socketLifetime.withLock {
                    ensureActive()
                    listen(port)
                }
            }
            receiver = Receiver(port, job)
        } finally {
            stateLock.unlock()
        }
    }

    override fun stopListening() {
        stateLock.lock()
        try {
            receiver?.job?.cancel(CancellationException("iOS UDP listener stopped"))
            receiver = null
        } finally {
            stateLock.unlock()
        }
    }

    override fun observePackets(): Flow<ByteArray> = packets.asSharedFlow()

    private fun CoroutineScope.listen(port: Int) {
        val fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
        if (fd < 0) {
            logFailure("socket creation", errno)
            return
        }

        try {
            val bindError = memScoped {
                val address = alloc<sockaddr_in>()
                address.sin_len = sizeOf<sockaddr_in>().convert()
                address.sin_family = AF_INET.convert()
                // Darwin's supported iOS targets are little-endian. The port is in network byte order.
                address.sin_port = ((port shl 8) or (port ushr 8)).toUShort()
                address.sin_addr.s_addr = INADDR_ANY
                if (bind(fd, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) < 0) errno else null
            }
            if (bindError != null) {
                logFailure("bind", bindError)
                return
            }
            receivePackets(fd)
        } finally {
            close(fd)
        }
    }

    private fun CoroutineScope.receivePackets(fd: Int) = memScoped {
        val descriptor = alloc<pollfd>()
        descriptor.fd = fd
        descriptor.events = POLLIN.convert()
        val buffer = ByteArray(2048)

        buffer.usePinned { pinned ->
            while (true) {
                ensureActive()
                val ready = poll(descriptor.ptr, 1.convert(), 100)
                if (ready < 0) {
                    val error = errno
                    if (error == EINTR) continue
                    logFailure("receive wait", error)
                    break
                }
                if (ready == 0) continue // An idle socket is normal.
                ensureActive()

                val received = recv(fd, pinned.addressOf(0), buffer.size.convert(), MSG_DONTWAIT)
                if (received < 0) {
                    val error = errno
                    if (error == EINTR) continue
                    if ((error == EAGAIN || error == EWOULDBLOCK) &&
                        (descriptor.revents.toInt() and POLLIN) != 0
                    ) continue
                    logFailure("receive", error)
                    break
                }
                ensureActive()
                packets.tryEmit(buffer.copyOf(received.toInt()))
            }
        }
    }

    private fun logFailure(operation: String, error: Int) {
        println("Soleil iOS UDP $operation failed (errno $error)")
    }

    private data class Receiver(val port: Int, val job: Job)
}
