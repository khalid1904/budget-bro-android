package com.example.data.drive

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DriveBackupFileInfo(
    val id: String,
    val name: String,
    val modifiedTime: String,
    val sizeBytes: Long
)

class GoogleDriveService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val driveScope = "https://www.googleapis.com/auth/drive.file"

    companion object {
        private const val TAG = "GoogleDriveService"
        const val BACKUP_FILENAME = "budget_bro_backup.json"
        val DRIVE_SCOPE = Scope("https://www.googleapis.com/auth/drive.file")

        fun getGoogleSignInOptions(): GoogleSignInOptions {
            return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(DRIVE_SCOPE)
                .build()
        }
    }

    /**
     * Gets a fresh OAuth access token in background for the signed in Google account.
     */
    suspend fun getAccessToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val scopeString = "oauth2:$driveScope"
            // Use GoogleAuthUtil to retrieve an OAuth2 token for drive.file
            val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                context,
                account.account ?: return@withContext null,
                scopeString
            )
            token
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining access token", e)
            null
        }
    }

    /**
     * Finds the existing backup file in Google Drive if one exists.
     */
    suspend fun findExistingBackupFile(accessToken: String): DriveBackupFileInfo? = withContext(Dispatchers.IO) {
        try {
            val query = "name = '$BACKUP_FILENAME' and trashed = false"
            val url = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,modifiedTime,size)&spaces=drive"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed finding backup file: ${response.code} $body")
                return@withContext null
            }

            val json = JSONObject(body)
            val files: JSONArray = json.optJSONArray("files") ?: return@withContext null
            if (files.length() > 0) {
                val fileObj = files.getJSONObject(0)
                return@withContext DriveBackupFileInfo(
                    id = fileObj.getString("id"),
                    name = fileObj.getString("name"),
                    modifiedTime = fileObj.optString("modifiedTime", ""),
                    sizeBytes = fileObj.optLong("size", 0L)
                )
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching for backup file", e)
            null
        }
    }

    /**
     * Uploads the backup JSON data to Google Drive.
     * If a backup file already exists, it updates it via PATCH /upload/drive/v3/files/{fileId}?uploadType=media
     * If no backup exists, it creates it via multipart upload.
     */
    suspend fun uploadBackup(accessToken: String, jsonData: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = findExistingBackupFile(accessToken)

            if (existing != null) {
                // Update existing file content
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/${existing.id}?uploadType=media"
                val mediaBody = jsonData.toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(updateUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .patch(mediaBody)
                    .build()

                val response = client.newCall(request).execute()
                val isSuccess = response.isSuccessful
                if (!isSuccess) {
                    Log.e(TAG, "Failed to update backup file: ${response.code} ${response.body?.string()}")
                }
                return@withContext isSuccess
            } else {
                // Create new file using multipart upload
                val metadata = JSONObject().apply {
                    put("name", BACKUP_FILENAME)
                    put("description", "Budget Bro Offline-First Financial Backup")
                    put("mimeType", "application/json")
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(
                        metadata.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
                    )
                    .addPart(
                        jsonData.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
                    )
                    .build()

                // Google Drive upload URI for multipart
                val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val request = Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(multipartBody)
                    .build()

                val response = client.newCall(request).execute()
                val isSuccess = response.isSuccessful
                if (!isSuccess) {
                    Log.e(TAG, "Failed to create backup file: ${response.code} ${response.body?.string()}")
                }
                return@withContext isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during backup upload", e)
            false
        }
    }

    /**
     * Downloads the backup JSON string from Google Drive.
     */
    suspend fun downloadBackup(accessToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val existing = findExistingBackupFile(accessToken) ?: return@withContext null
            val downloadUrl = "https://www.googleapis.com/drive/v3/files/${existing.id}?alt=media"

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext response.body?.string()
            } else {
                Log.e(TAG, "Failed downloading backup file: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading backup", e)
            null
        }
    }
}
