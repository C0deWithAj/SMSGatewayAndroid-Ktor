package com.mojo.smsserver.di

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.telephony.SmsManager
import androidx.room.Room
import androidx.work.WorkManager
import com.google.gson.Gson
import com.mojo.smsserver.data.database.sms.SMSDao
import com.mojo.smsserver.data.database.SMSDatabase
import com.mojo.smsserver.data.database.audio.AudioDao
import com.mojo.smsserver.data.network.SMSServerAPI
import com.mojo.smsserver.data.repository.DataRepository
import com.mojo.smsserver.data.repository.DataRepositoryImp
import com.mojo.smsserver.util.AppConstant.BASE_SERVER_URL
import com.mojo.smsserver.util.AppConstant.DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providesSMSDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, SMSDatabase::class.java, DATABASE_NAME).build()


    @Singleton
    @Provides
    fun providesRetrofit(): SMSServerAPI {
        return Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(
            BASE_SERVER_URL
        ).build().create(SMSServerAPI::class.java)
    }

    @Singleton
    @Provides
    fun providesDataRepository(
        smsDao: SMSDao,
        audioDao: AudioDao,
        smsServerAPI: SMSServerAPI
    ): DataRepository =
        DataRepositoryImp(smsDao, audioDao = audioDao, smsServerAPI)

    @Singleton
    @Provides
    fun providesGson(): Gson = Gson()

    @Singleton
    @Provides
    fun providesWorkManager(@ApplicationContext context: Context) =
        WorkManager.getInstance(context)

    @Singleton
    @Provides
    fun providesSMSDao(smsDatabase: SMSDatabase) = smsDatabase.getSMSDao()

    @Singleton
    @Provides
    fun providesAudioFiles(smsDatabase: SMSDatabase) = smsDatabase.getAudioDao()

    @Singleton
    @Provides
    fun providesSMSManager(@ApplicationContext context: Context): SmsManager =
        SmsManager.getDefault()

//    @Singleton
//    @Provides
//    fun mediaPlayer(@ApplicationContext context: Context): MediaPlayer? {
//        return MediaPlayer().apply {
//            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
//        }
//    }

    @Singleton
    @Provides
    fun wifiLock(@ApplicationContext context: Context): WifiManager.WifiLock? {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.createWifiLock(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            else WifiManager.WIFI_MODE_FULL,
            "mylock"
        )
    }

    @Singleton
    @Provides
    fun providesVolumeManager(@ApplicationContext context: Context): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager


//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
//    context.getSystemService(SmsManager::class.java) else
}


