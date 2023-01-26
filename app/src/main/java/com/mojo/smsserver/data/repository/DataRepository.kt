package com.mojo.smsserver.data.repository

import com.mojo.smsserver.data.database.sms.SMSEntity
import com.mojo.smsserver.data.model.AudioItem
import com.mojo.smsserver.data.model.SMSItem
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody
import retrofit2.Call

interface DataRepository {
    suspend fun insertSMS(sms: SMSItem)
    suspend fun getPendingSMSFromPhone(): Flow<List<SMSEntity>>
    suspend fun updateStatus(sms: SMSItem)
    suspend fun downloadFile(url: String?): Call<ResponseBody?>?
    suspend fun updateAudioFile(audioItem: AudioItem?)
    suspend fun getPendingForDownloadAudios(): List<AudioItem>?
    suspend fun insertFile(audioItem: AudioItem?)
    suspend fun insertFiles(list: ArrayList<AudioItem>?)
    suspend fun clearAudioFileDatabase()
}