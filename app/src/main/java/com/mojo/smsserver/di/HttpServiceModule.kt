package com.mojo.smsserver.di

import android.content.Context
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat.getSystemService
import com.mojo.smsserver.data.database.SMSDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton


@Module
@InstallIn(ServiceComponent::class)
object HttpServiceModule {



}