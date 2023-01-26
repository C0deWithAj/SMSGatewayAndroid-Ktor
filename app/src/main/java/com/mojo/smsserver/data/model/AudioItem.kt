package com.mojo.smsserver.data.model

import com.google.gson.annotations.SerializedName
import com.mojo.smsserver.util.AppConstant

data class AudioItem(
    @SerializedName("url")
    var url: String?,
    var failedDownloadAttempts: Int = 0,
    var downloadStatus: Int = AppConstant.AUDIO_STATUS_PENDING
)