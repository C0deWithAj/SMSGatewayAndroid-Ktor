package com.mojo.smsserver.usecase

import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.gson.Gson
import com.mojo.smsserver.data.model.AudioItem
import com.mojo.smsserver.data.model.audiogateway.AudioFiles
import com.mojo.smsserver.data.repository.DataRepository
import com.mojo.smsserver.data.workmanager.DownloadFilesWorkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject


class DownloadAudiosUseCase @Inject constructor(
    private val dataRepository: DataRepository,
    private val gson: Gson,
    private val workManager: WorkManager
) {

    lateinit var coroutineScope: CoroutineScope

    fun invoke(
        mediaUrls: String?,
        action: String,
        deleteExistingFiles: Boolean,
        coroutineScope: CoroutineScope,
    ) {
        this.coroutineScope = coroutineScope
        when (action) {
            ACTION_DOWNLOAD_MEDIA_FILES -> {
                val audioFiles = parseJson(mediaUrls)
                Log.d("Test321", "DownloadAudiosUseCase - Invoke")
                audioFiles?.let {
                    if (it.filesUrls.isNotEmpty()) {
                        coroutineScope.launch {
                            if (deleteExistingFiles) {
                                Log.d("Test321", "DownloadAudiosUseCase - clearAudioFileDatabase")
                                dataRepository.clearAudioFileDatabase()
                            }
                            saveFilesInDatabase(it.filesUrls)
                            startAudioDownloadWorker(deleteExistingFiles)
                        }
                    }
                }
            }

            ACTION_DOWNLOAD_PENDING_AUDIO -> {

            }
        }
    }

    private fun startAudioDownloadWorker(deleteExistingFile: Boolean) {
        //TODO: Check if we need to pass a command to Download new files or Retry failed Files
        //TODO: For Failed Files - Check If Failed files with Max Number of retries
        //TODO: Pass Command to delete existing files
        //TODO: In case of failure of any file retry WorkRequest

        val inputData: Data = Data.Builder().apply {
            putBoolean(DownloadFilesWorkRequest.EXTRA_DELETE_EXISTING_FILES, deleteExistingFile)
        }.build()

        val backupWorkRequest = OneTimeWorkRequestBuilder<DownloadFilesWorkRequest>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(inputData)
            .addTag("AUDIO_FILE_DOWNLOADER")
            .build()
        workManager.enqueue(backupWorkRequest)
    }


    private fun parseJson(mediaUrls: String?): AudioFiles? {
        if (!mediaUrls.isNullOrEmpty()) {
            return gson.fromJson(mediaUrls, AudioFiles::class.java)
        }
        return null
    }

    private suspend fun saveFilesInDatabase(arrayList: ArrayList<AudioItem>) {
        dataRepository.insertFiles(arrayList)
    }

    companion object {
        const val ACTION_DOWNLOAD_MEDIA_FILES = "download_media_files"
        const val ACTION_DOWNLOAD_PENDING_AUDIO = "download_media_files"
    }
}


