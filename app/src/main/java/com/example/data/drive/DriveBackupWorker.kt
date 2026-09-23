package com.example.data.drive

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.BudgetRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit

class DriveBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "budget_bro_periodic_drive_backup"
        private const val TAG = "DriveBackupWorker"

        fun schedulePeriodicBackup(context: Context, frequency: String) {
            val workManager = WorkManager.getInstance(context)

            val repeatIntervalHours = when (frequency.uppercase()) {
                "DAILY" -> 24L
                "WEEKLY" -> 7 * 24L
                "MONTHLY" -> 30 * 24L
                else -> 24L
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<DriveBackupWorker>(
                repeatIntervalHours, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d(TAG, "Scheduled periodic Drive backup with frequency: $frequency ($repeatIntervalHours hrs)")
        }

        fun cancelPeriodicBackup(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled periodic Drive backup work")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val drivePrefs = DrivePreferences(context)

            if (!drivePrefs.isPeriodicBackupEnabled) {
                Log.d(TAG, "Periodic backup is disabled, skipping")
                return Result.success()
            }

            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null || !GoogleSignIn.hasPermissions(account, GoogleDriveService.DRIVE_SCOPE)) {
                Log.w(TAG, "No Google account with Drive permission signed in")
                return Result.retry()
            }

            val driveService = GoogleDriveService(context)
            val accessToken = driveService.getAccessToken(account)
            if (accessToken == null) {
                Log.w(TAG, "Could not acquire access token for background backup")
                return Result.retry()
            }

            // Read database data
            val db = AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
            val repository = BudgetRepository(db, context)
            val jsonBackup = repository.exportDataToJson()

            val success = driveService.uploadBackup(accessToken, jsonBackup)
            if (success) {
                drivePrefs.lastBackupTime = System.currentTimeMillis()
                Log.i(TAG, "Periodic Drive backup succeeded at ${System.currentTimeMillis()}")
                Result.success()
            } else {
                Log.w(TAG, "Drive upload failed, retrying later")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing background backup", e)
            Result.retry()
        }
    }
}
