package com.mojo.smsserver.util

import android.content.Context
import java.io.File

object AppConstant {
    const val DATABASE_NAME = "SMS_DATABASE"

    const val SMS_STATUS_PENDING = 0
    const val SMS_STATUS_DELIVERED = 1
    const val SMS_STATUS_FAILED = 2

    const val AUDIO_STATUS_PENDING = 0
    const val AUDIO_STATUS_DOWNLOADED = 1
    const val AUDIO_STATUS_FAILED = 2

    const val MAX_AUDIO_DOWNLOAD_ATTEMPTS = 3

    const val SMS_SOURCE_SERVER =
        "from_phone" // Received from server - need to be sent on through SMS
    const val SMS_SOURCE_PHONE = "from_phone" //Received through SMS - need to be sent to server
    const val BASE_SERVER_URL =
        "http://dev.teaconcepts.net" //Received through SMS - need to be sent to server
    const val API_TOKEN = "123456"
    const val AUDIO_FOLDER_NAME = "MuFuAudio"
    const val HTTP_SERVER_PORT = 9090


    fun getAudioFolderFilePath(context: Context): String {
        return "${context.getExternalFilesDir(null)}${File.separator}${AppConstant.AUDIO_FOLDER_NAME}${File.separator}"
    }

}


