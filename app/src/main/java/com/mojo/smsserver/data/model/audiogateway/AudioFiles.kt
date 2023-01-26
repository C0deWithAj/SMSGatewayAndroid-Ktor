package com.mojo.smsserver.data.model.audiogateway

import com.google.gson.annotations.SerializedName
import com.mojo.smsserver.data.model.AudioItem


data class AudioFiles(
    @SerializedName("files_r") var filesUrls: ArrayList<AudioItem> = arrayListOf()
)
