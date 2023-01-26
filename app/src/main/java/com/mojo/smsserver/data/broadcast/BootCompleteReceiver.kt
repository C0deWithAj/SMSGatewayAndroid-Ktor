package com.mojo.smsserver.data.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, intent: Intent?) {
        val action: String? = intent?.action
        action?.let {
            if (action == Intent.ACTION_BOOT_COMPLETED) {
                Log.i("Test321", "BOOT COMPLETED...")
                Toast.makeText(p0, "BOOT COMPLETE RECEIVER", Toast.LENGTH_LONG).show()
            }
        }
    }
}

