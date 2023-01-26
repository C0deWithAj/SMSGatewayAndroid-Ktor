package com.mojo.smsserver.data.database.audio

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mojo.smsserver.data.model.AudioItem
import com.mojo.smsserver.util.AppConstant

@Entity
data class AudioEntity(
    @PrimaryKey
    var url: String,
    var failedAttempts: Int = 0,
    var downloadStatus: Int = AppConstant.AUDIO_STATUS_PENDING
)


fun AudioEntity.asUIModel() = AudioItem(
    url = this.url,
    failedDownloadAttempts = failedAttempts,
    downloadStatus = downloadStatus
)

fun AudioItem.asDatabaseModel() = AudioEntity(
    url = this.url ?: "",
    failedAttempts = failedDownloadAttempts,
    downloadStatus = downloadStatus
)
