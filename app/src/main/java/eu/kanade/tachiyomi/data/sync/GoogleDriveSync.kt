package eu.kanade.tachiyomi.data.sync

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.core.net.toUri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

class GoogleDriveSync(private val context: Context) {

    private val preferenceStore: PreferenceStore = Injekt.get()

    val lastSyncTime: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("drive_last_sync"), 0L,
    )

    private val accountEmailPref: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("drive_account_email"), "",
    )

    fun getSignedInEmail(): String = accountEmailPref.get()

    fun isSignedIn(): Boolean =
        GoogleSignIn.getLastSignedInAccount(context)
            ?.grantedScopes
            ?.contains(Scope(DriveScopes.DRIVE_APPDATA)) == true

    fun buildSignInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /**
     * Returns true if sign-in succeeded, false otherwise.
     */
    fun handleSignInResult(result: ActivityResult): Boolean {
        return runCatching {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(Exception::class.java)
            accountEmailPref.set(account?.email.orEmpty())
            true
        }.getOrElse { e ->
            logcat(LogPriority.ERROR, e) { "Google Drive sign-in failed" }
            false
        }
    }

    fun signOut() {
        buildSignInClient().signOut()
        accountEmailPref.set("")
        lastSyncTime.set(0L)
    }

    /**
     * Bidirectional sync using last-modified timestamps.
     *
     * - If the Drive file is newer than our last sync → pull (restore from Drive).
     * - Otherwise → push (upload local backup to Drive).
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext SyncResult.NotSignedIn

        val drive = buildDriveService(account)
        val remoteFile = findBackupFile(drive)
        val localLastSync = lastSyncTime.get()

        if (remoteFile != null && remoteFile.modifiedTimeMs > localLastSync) {
            // Cloud is newer → pull
            logcat { "Drive sync: remote newer (${remoteFile.modifiedTimeMs} > $localLastSync), pulling" }
            val outputStream = ByteArrayOutputStream()
            drive.files().get(remoteFile.id).executeMediaAndDownloadTo(outputStream)

            val tempFile = java.io.File(context.cacheDir, BACKUP_FILENAME)
            FileOutputStream(tempFile).use { it.write(outputStream.toByteArray()) }

            BackupRestoreJob.start(
                context = context,
                uri = tempFile.toUri(),
                options = RestoreOptions(),
            )
            lastSyncTime.set(remoteFile.modifiedTimeMs)
            return@withContext SyncResult.PulledFromDrive
        }

        // Local is newer (or no remote file) → push
        logcat { "Drive sync: pushing local backup" }
        val backupDir = java.io.File(context.cacheDir, "drive_sync_tmp").also { it.mkdirs() }

        try {
            BackupCreator(context, isAutoBackup = true).backup(backupDir.toUri(), BackupOptions())
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Backup creation failed" }
            return@withContext SyncResult.BackupFailed
        }

        val backupFile = backupDir.listFiles()?.maxByOrNull { it.lastModified() }
            ?: return@withContext SyncResult.BackupFailed

        val bytes = backupFile.readBytes()
        backupDir.deleteRecursively()

        uploadBackup(drive, bytes, existingId = remoteFile?.id)
        lastSyncTime.set(System.currentTimeMillis())
        SyncResult.Success
    }

    /** Force-download the latest backup from Drive and queue a restore. */
    suspend fun restoreFromDrive(): SyncResult = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext SyncResult.NotSignedIn

        val drive = buildDriveService(account)
        val remoteFile = findBackupFile(drive) ?: return@withContext SyncResult.NoBackupFound

        val outputStream = ByteArrayOutputStream()
        drive.files().get(remoteFile.id).executeMediaAndDownloadTo(outputStream)

        val tempFile = java.io.File(context.cacheDir, BACKUP_FILENAME)
        FileOutputStream(tempFile).use { it.write(outputStream.toByteArray()) }

        BackupRestoreJob.start(
            context = context,
            uri = tempFile.toUri(),
            options = RestoreOptions(),
        )
        lastSyncTime.set(remoteFile.modifiedTimeMs)
        SyncResult.PulledFromDrive
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
            .apply { selectedAccount = account.account }

        return Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("MangaForge")
            .build()
    }

    private fun uploadBackup(drive: Drive, bytes: ByteArray, existingId: String? = null) {
        val content = ByteArrayContent("application/octet-stream", bytes)
        val id = existingId ?: findBackupFile(drive)?.id
        if (id != null) {
            drive.files().update(id, null, content).execute()
        } else {
            val metadata = File().apply {
                name = BACKUP_FILENAME
                parents = listOf("appDataFolder")
            }
            drive.files().create(metadata, content).execute()
        }
    }

    private fun findBackupFile(drive: Drive): DriveFileInfo? =
        drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILENAME'")
            .setFields("files(id,modifiedTime)")
            .execute()
            .files
            .firstOrNull()
            ?.let { DriveFileInfo(it.id, it.modifiedTime?.value ?: 0L) }

    private data class DriveFileInfo(val id: String, val modifiedTimeMs: Long)

    companion object {
        private const val BACKUP_FILENAME = "mangaforge_backup.proto.gz"
    }

    sealed interface SyncResult {
        data object Success : SyncResult          // pushed local → cloud
        data object PulledFromDrive : SyncResult  // pulled cloud → local (cloud was newer)
        data object NotSignedIn : SyncResult
        data object BackupFailed : SyncResult
        data object NoBackupFound : SyncResult
    }
}
