package eu.kanade.tachiyomi.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.util.system.workManager
import java.util.concurrent.TimeUnit

class DriveSyncJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sync = GoogleDriveSync(context)
        if (!sync.isSignedIn()) return Result.success() // silently skip if not signed in
        return when (sync.sync()) {
            GoogleDriveSync.SyncResult.Success -> Result.success()
            else -> Result.retry()
        }
    }

    companion object {
        private const val TAG = "DriveSyncJob"

        fun setupTask(context: Context) {
            val request = PeriodicWorkRequestBuilder<DriveSyncJob>(2, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            context.workManager.enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancelTask(context: Context) {
            context.workManager.cancelUniqueWork(TAG)
        }
    }
}
