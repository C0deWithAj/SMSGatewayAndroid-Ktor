package com.mojo.smsserver.ui.activities.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.mojo.smsserver.R
import com.mojo.smsserver.data.model.result.ResultData
import com.mojo.smsserver.ui.service.HttpServerService
import com.mojo.smsserver.ui.theme.SMSServerAppTheme
import com.mojo.smsserver.util.AppConstant
import com.mojo.smsserver.util.Singleton
import com.mojo.smsserver.util.isMyServiceRunning
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import com.mojo.smsserver.util.AppUtil


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainActivityViewModel by viewModels()
    private var mService: HttpServerService? = null
    private var mBound: Boolean = false
    private var serverState = mutableStateOf(HTTP_SERVER_STATUS.SERVER_DISCONNECTED)
    private var serverException = mutableStateOf("")
    private var connectedAddress = mutableStateOf("")

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SMSServerAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Yellow),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(70.dp))
                        Text(
                            text = "MUFU SMS GATEWAY",
                            textAlign = TextAlign.Center,
                            fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.Cyan
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .background(Color.Yellow),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (AppUtil.isWifiConnected(context = applicationContext)) {
                                var portFieldText by remember { mutableStateOf(TextFieldValue("")) }
                                var isConnectBtnEnabled by remember { mutableStateOf(false) }
                                loadMainLayout(
                                    serverState.value,
                                    connectedAddress.value,
                                    portFieldText, isConnectBtnEnabled
                                ) {
                                    portFieldText = it
                                    isConnectBtnEnabled =
                                        portFieldText.text.length >= 4
                                }
                                bindServiceIfRunningAlready()
                            } else {
                                loadErrorMessage("Please Connect to a local Network and Try Again")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindServiceIfRunningAlready() {
        if (isMyServiceRunning(HttpServerService::class.java)) {
            val intent = Intent(
                this, HttpServerService::class.java
            )
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    @Composable
    private fun showIpAddress(ipAddress: String) {
        Text(
            text = "MUFU Server is Active!!",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Text(text = ipAddress, textAlign = TextAlign.Center)
    }

    @Composable
    private fun portTextField(
        textFieldValue: TextFieldValue,
        onValueChanged: (TextFieldValue) -> Unit
    ) {
        TextField(value = textFieldValue,
            onValueChange = {
                if (it.text.length <= 5)
                    onValueChanged(it)
            },
            label = { Text(stringResource(id = R.string.port)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text(text = stringResource(id = R.string.port)) })
    }

    @Composable
    fun loadMainLayout(
        serverState: HTTP_SERVER_STATUS,
        ipAddress: String,
        portFieldText: TextFieldValue,
        isBtnEnabled: Boolean,
        onPortValueChanged: (TextFieldValue) -> Unit
    ) {
        loadErrorMessage(serverException = serverException.value)
        Spacer(modifier = Modifier.height(10.dp))
        if (serverState == HTTP_SERVER_STATUS.SERVER_CONNECTED) {
            showIpAddress(ipAddress = connectedAddress.value)
        } else {
            portTextField(portFieldText, onPortValueChanged)
        }
        Spacer(modifier = Modifier.height(20.dp))
        addServiceStartLayout({
            lifecycleScope.launch {
                val portNum =
                    if (portFieldText.text.isNotEmpty()) portFieldText.text.toInt() else AppConstant.HTTP_SERVER_PORT
                startOrStopHttpServer(portNum)
            }
        }, serverState == HTTP_SERVER_STATUS.SERVER_CONNECTED, isBtnEnabled)
        Spacer(modifier = Modifier.height(20.dp))
    }

    @Composable
    fun addServiceStartLayout(onClick: () -> Unit, isServiceOn: Boolean, isBtnEnabled: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            var btnEnabled = isBtnEnabled
            if (isServiceOn) {
                btnEnabled = true
            }
            Button(onClick = onClick, enabled = btnEnabled) {
                Text(
                    text = if (isServiceOn) stringResource(id = R.string.turn_off_service) else stringResource(
                        id = R.string.turn_on_service
                    )
                )
            }
        }
    }

    @Composable
    private fun loadErrorMessage(serverException: String?) {
        serverException?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = "Error in Configuring Server",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = it,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Preview
    @Composable
    fun PreviewConversation() {
//        Column(
//            modifier = Modifier
//                .fillMaxHeight()
//                .fillMaxWidth()
//        ) {
//            addServiceStartLayout(onClick = {
//
//            }, false)
//        }
    }

    private fun makeDefaultMessagingApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = applicationContext.getSystemService(RoleManager::class.java)
            val isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
            if (isRoleAvailable) {
                val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                if (!isRoleHeld) {
                    val roleRequestIntent =
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    startForResultMakeDefaultSMSApp.launch(roleRequestIntent)
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
            startForResultMakeDefaultSMSApp.launch(intent)
        }
    }

    private val startForResultMakeDefaultSMSApp =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {

            } else if (result.resultCode == Activity.RESULT_CANCELED) {

            }
        }

    private fun startOrStopHttpServer(port: Int) {
        if (!Singleton.isServerRunning) {
            val intent = Intent(
                this, HttpServerService::class.java
            ) // Build the intent for the service
            intent.putExtra(HttpServerService.EXTRA_PORT_NUMBER, port)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            } else {
                applicationContext.startService(intent)
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        } else {
            Log.d("Test321", "Service Running already - Make close request")
            stopHttpService()
        }
    }

    //https://stackoverflow.com/questions/2463175/how-to-have-android-service-communicate-with-activity
    private class ServerUpdatesReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

        }
    }


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // when the service is connected, get its instance
            val binder = service as HttpServerService.MyBinder
            mService = binder.getService()
            mService?.let {
                it.serverConnectivityData.observe(
                    this@MainActivity,
                    httpServerUpdatesObserver
                )
            }
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // service disconnected
            mBound = false
        }
    }

    private val httpServerUpdatesObserver = Observer<ResultData<String>>
    { resultData ->
        when (resultData) {
            is ResultData.Success -> {
                resultData.data?.let { result ->
                    serverState.value = HTTP_SERVER_STATUS.SERVER_CONNECTED
                    connectedAddress.value = result
                    serverException.value = ""
                }
            }

            is ResultData.Failed -> {
                unBindService()
                resultData.message?.let {
                    serverState.value = HTTP_SERVER_STATUS.SERVER_DISCONNECTED
                }
            }

            is ResultData.Exception -> {
                unBindService()
                serverState.value = HTTP_SERVER_STATUS.SERVER_DISCONNECTED
                serverException.value = "Exception: ${resultData.exception?.message}"
                //Show Exception
            }
        }
    }

    private fun stopHttpService() {
        mService?.let {
            it.stopTheService()
            unBindService()
        }
        serverState.value = HTTP_SERVER_STATUS.SERVER_DISCONNECTED
        serverException.value = ""
    }

    private fun unBindService() {
        try {
            mService?.let {
                if (isMyServiceRunning(HttpServerService::class.java))
                    unbindService(connection)
            }
            mBound = false
        } catch (ex: java.lang.Exception) {

        }
    }

    override fun onStop() {
        super.onStop()
        unBindService()
    }
}

enum class HTTP_SERVER_STATUS {
    SERVER_CONNECTED, SERVER_DISCONNECTED
}
