package com.mojo.smsserver.data.database.audio

import androidx.room.*

@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audioEntity: AudioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(audioEntity: List<AudioEntity>)

    @Delete
    suspend fun delete(smsEntity: AudioEntity)

    @Query("select * from AudioEntity where  downloadStatus = :status")
    suspend fun getPendingAudios(status: Int): List<AudioEntity>

    @Query("Delete from AudioEntity")
    suspend fun clearAll()

    @Update
    suspend fun update(list: List<AudioEntity>)

    @Update
    suspend fun update(list: AudioEntity)
}