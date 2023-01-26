package com.mojo.smsserver.data.broadcast

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import com.mojo.smsserver.data.model.SMSItem
import com.mojo.smsserver.data.repository.DataRepository
import com.mojo.smsserver.util.AppConstant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SMSReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob())

    @Inject
    lateinit var repository: DataRepository

    private val pdu_type = "pdus"


    @TargetApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context?, intent: Intent) {
        Log.i("Test321", "***Message Received&&&&")
        // Get the SMS message.
        val bundle = intent.extras
        val msgs: Array<SmsMessage?>
        var strMessage = ""
        val format = bundle!!.getString("format")
        // Retrieve the SMS message received.
        val pdus = bundle[pdu_type] as Array<Any>?
        if (pdus != null) {
            // Check the Android version.
            val isVersionM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            // Fill the msgs array.
            msgs = arrayOfNulls<SmsMessage>(pdus.size)
            for (i in msgs.indices) {
                // Check Android version and use appropriate createFromPdu.
                if (isVersionM) {
                    // If Android version M or newer:
                    msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
                } else {
                    // If Android version L or older:
                    msgs[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                }
                // Build the message to show.
                strMessage += "SMS from " + msgs[i]?.originatingAddress
                strMessage += """ :${msgs[i]?.messageBody.toString()}"""
                // Log and display the SMS message.
                Log.i("Test321", "onReceive: $strMessage")
                Toast.makeText(context, strMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Save Message to Database
     */
    private fun processReceivedMessage(msg: String, number: String) {
        val smsItem =
            SMSItem(msg, number, AppConstant.SMS_SOURCE_PHONE, AppConstant.SMS_STATUS_PENDING, 0)
        scope.launch {
            Log.i("Test321", "SMS Inserted")
            repository.insertSMS(smsItem)
        }
    }


}