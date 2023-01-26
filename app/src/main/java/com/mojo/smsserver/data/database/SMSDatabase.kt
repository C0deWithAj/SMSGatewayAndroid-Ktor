package com.mojo.smsserver.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mojo.smsserver.data.database.audio.AudioDao
import com.mojo.smsserver.data.database.audio.AudioEntity
import com.mojo.smsserver.data.database.sms.SMSDao
import com.mojo.smsserver.data.database.sms.SMSEntity


@Database(entities = [SMSEntity::class, AudioEntity::class], version = 1)
abstract class SMSDatabase : RoomDatabase() {
    abstract fun getSMSDao(): SMSDao
    abstract fun getAudioDao(): AudioDao
}