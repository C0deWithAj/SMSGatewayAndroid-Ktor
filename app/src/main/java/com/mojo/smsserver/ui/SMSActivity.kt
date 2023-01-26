package com.mojo.smsserver.ui

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import com.mojo.smsserver.R

class SMSActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smsactivity)
        makeDefaultMessagingApp()
    }
    
    private fun makeDefaultMessagingApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = applicationContext.getSystemService(
                RoleManager::class.java
            )!!
            val isRoleAvailable = roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)
            if (isRoleAvailable) {
                // check whether your app is already holding the default SMS app role.
                val isRoleHeld = roleManager!!.isRoleHeld(RoleManager.ROLE_SMS)
                if (!isRoleHeld) {
                    Log.i("Test", "Is Role Held false")
                    val roleRequestIntent =
                        roleManager!!.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    startActivityForResult(roleRequestIntent, 500)
                } else {
                    Log.i("Test", "Is Role Held true")

                }
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(
                Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                applicationContext.packageName
            )
            startActivityForResult(intent, 500)
        }
//        if (getDefaultSmsPackage(applicationContext) != null && getDefaultSmsPackage(
//                applicationContext
//            ) != applicationContext.getPackageName()
//        ) {
//            var roleManager: RoleManager? = null
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                roleManager = applicationContext.getSystemService(RoleManager::class.java)
//                if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
//                    if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
//                        Log.d("role", "role")
//                    } else {
//                        val roleRequestIntent = roleManager.createRequestRoleIntent(
//                            RoleManager.ROLE_SMS
//                        )
//                        (applicationContext as Activity).startActivityForResult(
//                            roleRequestIntent,
//                            500
//                        )
//                    }
//                }
//            } else {
//                val intent = Intent(Sms.Intents.ACTION_CHANGE_DEFAULT)
//                intent.putExtra(
//                    Sms.Intents.EXTRA_PACKAGE_NAME,
//                    applicationContext.packageName
//                )
//                (applicationContext as Activity).startActivityForResult(intent, 500)
//            }
//        }


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            Log.i("Test321", "MOre than Q")
//            val roleManager = getSystemService(RoleManager::class.java)
//            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
//                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
//                    // askPermissions()
//                    Log.i("Test321", "Ask Permission ")
//
//                } else {
//                    Log.i("Test321", "Else - Start Activity for ROLE SMS")
//                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
//                    //    startActivity(intent)
//                    startActivityForResult(intent, 120)
//                }
//            } else {
//                Log.i("Test321", "Finish")
//                finish()
//            }
//        } else {
//            if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
//                //askPermissions()
//                Log.i("Test321", "Ask Permission ")
//            } else {
//                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
//                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
//                startActivityForResult(intent, 11)
//            }
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            Log.i("Test321", "Result okay - Result code = ${resultCode}")
        } else {
            Log.i("Test321", "Result code = ${resultCode}")
        }
    }
}