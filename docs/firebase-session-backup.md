# Firebase session backup

Room is the primary store. The app-scoped backup worker observes committed Room sessions, including recordings and CSV imports. In Analysis, the selected session shows whether its backup is pending, uploading, confirmed, or eligible for retry. Firebase outages leave local sessions available.

## Configure the Firebase project

1. Open the Soleil project in Firebase Console. Under **Build → Realtime Database**, select **Create Database** if there is no intended instance. Choose its location and start in **Locked mode**. If an instance already contains other application data, review its rules before publishing and merge the session subtree without granting access at an ancestor.
2. In the instance's **Rules** tab, publish the rules in [database.rules.json](../database.rules.json). The rules grant access only to the matching authenticated UID under `users/{uid}/sessions`. They allow an identical completion-marker retry and seal the completed chunks.
3. Under **Project settings → Your apps**, download the updated Android configuration to `androidApp/google-services.json` and, for the registered Apple bundle identifier, the Apple configuration to `iosApp/iosApp/GoogleService-Info.plist`. Confirm that Android's `project_info.firebase_url` and Apple's `DATABASE_URL` match the intended Realtime Database instance URL. Include the plist in the iOS app target exactly once. Both files are ignored by Git.
4. On macOS, open `iosApp/iosApp.xcodeproj`. Add the `FirebaseDatabase` product from the same Firebase Apple SDK Swift package as `FirebaseCore` and `FirebaseAuth` to the `iosApp` target. The GitLive 2.7.0 native binding baseline is Firebase Apple SDK 11.8.0. Keep the existing direct `Shared` framework build phase and select one integration method for the Firebase libraries.
5. Sign in with a test account, record or import a session, and check the status in Analysis. In the console, verify the authenticated user's session and its `complete` marker. In **Rules → Rules Playground**, simulate a read of that session as the owner's UID, a second UID, and an unauthenticated client. Only the owner should pass. Simulate a write to its `complete` child as the second UID and confirm it is denied. Test on logger Wi-Fi without Internet, then reconnect and confirm the backup advances without another recording.

For a local, isolated rules test, run this from the repository root with Firebase CLI, Java, and Node.js installed.

```powershell
firebase emulators:exec --only auth,database --project demo-soleil-backup "node scripts/test-session-backup-rules.mjs"
```

## Data and recovery

Each session is stored at `users/{uid}/sessions/{hexUtf8SessionId}`. Its original ID is in the versioned `complete` manifest, along with title, vehicle, track name, start and completion times, and ordered lap metadata with sample counts. Telemetry is stored at `chunks/{zeroBasedLapIndex}/{zeroBasedChunkIndex}`, up to 128 samples per chunk. Each leaf is a JSON-encoded string. A telemetry row is the original signed timestamp followed by Float32 bit patterns for acceleration X/Y/Z, latitude/longitude, MPH speed, quaternion W/I/J/K, and altitude. Lap metadata preserves durations, sectors, canonical MPH top speed, and the DNF flag. The backup does not include local file paths or credentials.

The existing Room `remoteStorageUrl` field stores the owner path, and `isSynced` records server acknowledgement. New authenticated saves are bound to that UID when committed. Unowned local sessions, including legacy and signed-out saves, are assigned to the first authenticated account that claims them on this installation, even if that account is offline. Room has no historical account provenance for those rows. Switching accounts cannot reassign an already-bound row. Pending rows are retried while the app is executing. The background scheduler does not run while the process is stopped.

A backup represents the session at upload time. Later local edits and local deletion do not modify its cloud copy. Retries reuse deterministic chunk keys and seal the backup only after the server accepts every chunk. A cached or previously submitted completion marker must be acknowledged by Firebase before the local row is marked synced.

`SessionBackupRepository.readBackedUpSession(sessionId)` reconstructs a domain session from the signed-in UID's completion manifest and validated chunks. It returns no session for an incomplete upload, reports missing or malformed chunks, and re-acknowledges the identical marker so an offline cache alone cannot verify recovery. The returned value is ready for a caller to review before importing. There is no automatic restore to Room or restore UI.
