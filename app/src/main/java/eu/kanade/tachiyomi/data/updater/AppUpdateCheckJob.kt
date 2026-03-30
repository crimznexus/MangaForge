package eu.kanade.tachiyomi.data.updater

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.util.system.workManager
import java.util.concurrent.TimeUnit

class AppUpdateCheckJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AppUpdateChecker().checkForUpdate(context)
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "AppUpdateCheckJob"

        fun setupTask(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppUpdateCheckJob>(2, TimeUnit.DAYS)
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
    }
}
