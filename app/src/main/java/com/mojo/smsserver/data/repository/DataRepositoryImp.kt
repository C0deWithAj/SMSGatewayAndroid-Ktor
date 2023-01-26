package com.mojo.smsserver.data.repository

import com.mojo.smsserver.data.database.audio.AudioDao
import com.mojo.smsserver.data.database.audio.AudioEntity
import com.mojo.smsserver.data.database.audio.asDatabaseModel
import com.mojo.smsserver.data.database.sms.SMSDao
import com.mojo.smsserver.data.database.sms.asDatabaseModel
import com.mojo.smsserver.data.model.AudioItem
import com.mojo.smsserver.data.model.SMSItem
import com.mojo.smsserver.data.network.SMSServerAPI
import com.mojo.smsserver.util.AppConstant
import okhttp3.ResponseBody
import retrofit2.Call
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepositoryImp @Inject constructor(
    private val smsDao: SMSDao,
    private val audioDao: AudioDao,
    private val api: SMSServerAPI
) : DataRepository {

    override suspend fun insertSMS(sms: SMSItem) {
        smsDao.insert(sms.asDatabaseModel())
    }

    /**
     * Pending SMS received from Phone - That needs to be sent to Server
     */
    override suspend fun getPendingSMSFromPhone() =
        smsDao.getSMSList(AppConstant.SMS_STATUS_PENDING, AppConstant.SMS_SOURCE_PHONE)
//    override suspend fun getPendingSMSFromPhone() = flow {
//        emit(smsDao.getSMSList(Constant.SMS_STATUS_PENDING, Constant.SMS_SOURCE_PHONE))
//    }

    override suspend fun updateStatus(sms: SMSItem) {
        smsDao.update(sms.asDatabaseModel())
    }

    override suspend fun insertFile(audioItem: AudioItem?) {
        audioItem?.let {
            audioDao.insert(it.asDatabaseModel())
        }
    }

    override suspend fun insertFiles(list: ArrayList<AudioItem>?) {
        if (!list.isNullOrEmpty()) {
            audioDao.insertAll(list.map {
                AudioEntity(
                    it.url ?: "",
                    it.downloadStatus,
                    it.failedDownloadAttempts
                )
            })
        }
    }

    override suspend fun getPendingForDownloadAudios(): List<AudioItem>? {
        //
        val listAudioEntities = audioDao.getPendingAudios(AppConstant.SMS_STATUS_PENDING)
        return listAudioEntities.map { AudioItem(it.url, it.failedAttempts, it.failedAttempts) };
    }

    override suspend fun updateAudioFile(audioItem: AudioItem?) {
        audioItem?.let {
            audioDao.update(it.asDatabaseModel())
        }
    }

    override suspend fun downloadFile(url: String?): Call<ResponseBody?>? {
        url?.let {
            return api.downloadFileWithDynamicUrlSync(url)
        }
        return null
    }

    override suspend fun clearAudioFileDatabase() {
        audioDao.clearAll()
    }

}