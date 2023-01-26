package com.mojo.smsserver.usecase

import android.util.Log
import com.mojo.smsserver.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data that we will be sending to server
 */


class ServerDataUseCase @Inject constructor(private val dataRepository: DataRepository) {

    fun initPendingWork(scope: CoroutineScope) {
        scope.launch {
            dataRepository.getPendingSMSFromPhone().collect {

            }
        }
    }

}