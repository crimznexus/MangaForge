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
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
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

    fun handleSignInResult(result: ActivityResult) {
        runCatching {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(Exception::class.java)
            accountEmailPref.set(account?.email.orEmpty())
        }
    }

    fun signOut() {
        buildSignInClient().signOut()
        accountEmailPref.set("")
        lastSyncTime.set(0L)
    }

    /** Create a backup and upload it to Google Drive app-data folder. */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext SyncResult.NotSignedIn

        val drive = buildDriveService(account)

        val tempFile = java.io.File(context.cacheDir, BACKUP_FILENAME)
        val tempUri: Uri = tempFile.toUri()

        try {
            BackupCreator(context, isAutoBackup = true).backup(tempUri, BackupOptions())
        } catch (e: Exception) {
            return@withContext SyncResult.BackupFailed
        }

        val bytes = tempFile.readBytes()
        uploadBackup(drive, bytes)

        lastSyncTime.set(System.currentTimeMillis())
        SyncResult.Success
    }

    /** Download the latest backup from Drive and queue a restore. */
    suspend fun restoreFromDrive(): SyncResult = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: return@withContext SyncResult.NotSignedIn

        val drive = buildDriveService(account)
        val fileId = findBackupFileId(drive) ?: return@withContext SyncResult.NoBackupFound

        val outputStream = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)

        val tempFile = java.io.File(context.cacheDir, BACKUP_FILENAME)
        FileOutputStream(tempFile).use { it.write(outputStream.toByteArray()) }

        BackupRestoreJob.start(
            context = context,
            uri = tempFile.toUri(),
            options = RestoreOptions(),
        )
        SyncResult.Success
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

    private fun uploadBackup(drive: Drive, bytes: ByteArray) {
        val content = ByteArrayContent("application/octet-stream", bytes)
        val existingId = findBackupFileId(drive)
        if (existingId != null) {
            drive.files().update(existingId, null, content).execute()
        } else {
            val metadata = File().apply {
                name = BACKUP_FILENAME
                parents = listOf("appDataFolder")
            }
            drive.files().create(metadata, content).execute()
        }
    }

    private fun findBackupFileId(drive: Drive): String? =
        drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILENAME'")
            .setFields("files(id)")
            .execute()
            .files
            .firstOrNull()
            ?.id

    companion object {
        private const val BACKUP_FILENAME = "mangaforge_backup.proto.gz"
    }

    sealed interface SyncResult {
        data object Success : SyncResult
        data object NotSignedIn : SyncResult
        data object BackupFailed : SyncResult
        data object NoBackupFound : SyncResult
    }
}
