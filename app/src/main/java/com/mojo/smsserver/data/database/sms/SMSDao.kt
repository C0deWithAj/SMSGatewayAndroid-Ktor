package com.mojo.smsserver.data.database.sms

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import kotlinx.coroutines.flow.Flow


@Dao
interface SMSDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(smsEntity: SMSEntity)

    @Delete
    suspend fun delete(smsEntity: SMSEntity)

    @Query("select * from SMSEntity where deliveryStatus = :deliveryStatus AND source = :src")
    fun getSMSList(deliveryStatus: Int, src: String): Flow<List<SMSEntity>>

    @Update
    suspend fun update(sms: SMSEntity)

}

