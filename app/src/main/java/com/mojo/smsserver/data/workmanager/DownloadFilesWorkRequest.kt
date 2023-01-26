package com.mojo.smsserver.data.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.URLUtil.guessFileName
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.mojo.smsserver.R
import com.mojo.smsserver.data.model.AudioItem
import com.mojo.smsserver.data.repository.DataRepository
import com.mojo.smsserver.util.AppConstant
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Fetching pending download Audio Files from the server and Download them
 */

@HiltWorker
class DownloadFilesWorkRequest @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    private var notificationBuilder: NotificationCompat.Builder? = null
    private val notificationId = 1337

    @Inject
    lateinit var dataRepository: DataRepository

    override suspend fun doWork(): Result {
        val audioList = dataRepository.getPendingForDownloadAudios()
        val deleteExistingFiles = inputData.getBoolean(EXTRA_DELETE_EXISTING_FILES, false)
        if (deleteExistingFiles) {
            deleteExistingFiles(context)
        }

        audioList?.let {
            if (audioList.isNotEmpty()) {
                createFolder(context)
                val downloadedFiles = startDownloadingFiles(downloadFiles = audioList)
                if (downloadedFiles > 0) {
                    updateNotificationMessage("Successfully Downloaded $downloadedFiles Audio Files ")
                }
                delay(6000) //Show Notification for 6 minutes
            }
        }
        return Result.success()
    }

    private fun updateNotificationMessage(msg: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder?.let {
            it.setContentTitle(
                msg
            ).setProgress(100, 100, false)
            notificationManager.notify(notificationId, it.build());
        }
    }


    /**
     * Returns Downloaded Files
     */
    private suspend fun startDownloadingFiles(downloadFiles: List<AudioItem>): Int {
        var downloadedFiles = 0
        for (file in downloadFiles) {
            if (!file.url.isNullOrEmpty()) {
                val call = dataRepository.downloadFile(file.url)
                try {
                    val response = call?.execute()

                    if (response != null && response.isSuccessful) {
                        response.body()?.let {
                            val fileName = guessFileName(file.url, null, null)
                            Log.d(
                                "Test321",
                                "DownloadFileWorker - Fetched Files $fileName from Network Successfully"
                            )
                            updateFileDownloadStatus(
                                writeResponseBodyToDisk(it, fileName),
                                file
                            )
                            downloadedFiles++
                        }
                    } else {
                        //Failure
                        Log.e(
                            "Test321",
                            "DownloadFileWorker - Failure in Saving file -  url ${file.url}"
                        )
                        updateFileDownloadStatus(false, file)
                    }
                } catch (t: Throwable) {
                    //failed
                    Log.e(
                        "Test321",
                        "DownloadFileWorker - Failure in Saving file = ${t.message} - File url ${file.url}"
                    )
                    updateFileDownloadStatus(false, file)
                }
            }
        }
        return downloadedFiles
    }

    private suspend fun updateFileDownloadStatus(isDownloadSuccess: Boolean, file: AudioItem) {
        if (isDownloadSuccess) {
            file.downloadStatus = AppConstant.AUDIO_STATUS_DOWNLOADED
        } else {
            file.failedDownloadAttempts = file.failedDownloadAttempts++
            if (file.failedDownloadAttempts >= AppConstant.MAX_AUDIO_DOWNLOAD_ATTEMPTS) {
                file.downloadStatus == AppConstant.AUDIO_STATUS_FAILED
            }
        }
        dataRepository.updateAudioFile(file)
    }


    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "Audio_Chanel_Mufu",
                "Downloading Audio Files",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationBuilder = NotificationCompat.Builder(context, "Audio_Chanel_Mufu")
        val notificationBuilder = notificationBuilder
        val notification = notificationBuilder?.let {
            it.setSmallIcon(R.drawable.ic_launcher_foreground).setOngoing(true)
                .setAutoCancel(false).setOnlyAlertOnce(false).setProgress(100, 100, true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentTitle(context.getString(R.string.app_name)).setLocalOnly(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentText("Downloading Audio Files...")
                .build()
        }
        return ForegroundInfo(1337, notification!!)
    }


    private fun writeResponseBodyToDisk(body: ResponseBody?, fileName: String): Boolean {
        return try {
            val fileSuffix: String = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
            val file =
                File(AppConstant.getAudioFolderFilePath(context) + fileName)
            Log.i("Test321", "File name ${file.absolutePath}")
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                val fileSize = body?.contentLength()
                var fileSizeDownloaded: Long = 0
                inputStream = body?.byteStream()
                outputStream = FileOutputStream(file)
                if (inputStream != null) {
                    while (true) {
                        val read: Int = inputStream.read(fileReader)
                        if (read == -1) {
                            break
                        }
                        outputStream.write(fileReader, 0, read)
                        fileSizeDownloaded += read.toLong()
                        //Log.d(TAG, "file download: $fileSizeDownloaded of $fileSize")
                    }
                }
                outputStream.flush()
                true
            } catch (e: IOException) {
                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            false
        }
    }


    private fun deleteExistingFiles(context: Context) {
        val file = File(AppConstant.getAudioFolderFilePath(context))
        file.deleteRecursively()
    }

    private fun createFolder(context: Context) {
        val dir = File(AppConstant.getAudioFolderFilePath(context))
        if (!dir.exists())
            dir.mkdir()
    }

    companion object {
        const val EXTRA_DELETE_EXISTING_FILES = "delete_existing"


    }

}