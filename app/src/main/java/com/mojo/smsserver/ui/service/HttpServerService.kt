package com.mojo.smsserver.ui.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Process.myPid
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.mojo.smsserver.R
import com.mojo.smsserver.data.model.api.MuFUResponse
import com.mojo.smsserver.data.model.api.MufuResponseGson
import com.mojo.smsserver.data.model.playmedia.PlayMedia
import com.mojo.smsserver.data.model.result.ResultData
import com.mojo.smsserver.usecase.DownloadAudiosUseCase
import com.mojo.smsserver.usecase.PlayAudioUseCase
import com.mojo.smsserver.usecase.ServerDataUseCase
import com.mojo.smsserver.util.*
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import javax.inject.Inject


@AndroidEntryPoint
class HttpServerService : Service() {

    @Inject
    lateinit var smsManager: SmsManager

    @Inject
    lateinit var serverDataUseCase: ServerDataUseCase

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var downloadAudiosUseCase: DownloadAudiosUseCase

    @Inject
    lateinit var playAudioUseCase: PlayAudioUseCase

    //TODO: Inject from Hilt
    private var notificationBuilder: Notification.Builder? = null

    private val HTTPSERVICE_NOTIFICATION_ID = 55
    private val CHANNEL_ID = "SMS Server"

    private var server: WebServer? = null
    private val jobServer = SupervisorJob()
    private val scopeServer = CoroutineScope(Dispatchers.IO + jobServer)

    private val TAG = "HttpService"
    private val mBinder: IBinder = MyBinder()

    private var nettyAppliCationEngine: NettyApplicationEngine? = null

    //Data for Client
    private val _serverConnectivityData = MutableLiveData<ResultData<String>>()
    val serverConnectivityData: LiveData<ResultData<String>> get() = _serverConnectivityData
    private var startId: Int = 0
    private var portNumber: Int? = null

    private val scopeSMSSender: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO + Job())
    }

    private val scopeFileDownloader: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO + Job())
    }

    inner class MyBinder : Binder() {
        fun getService(): HttpServerService = this@HttpServerService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder;
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.startId = startId
        portNumber = intent?.getIntExtra(EXTRA_PORT_NUMBER, AppConstant.HTTP_SERVER_PORT)
        Log.i("Test321", "Port Number inside onStartCommand $portNumber")
        val notification = createNotification(context = applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(
                HTTPSERVICE_NOTIFICATION_ID, notification
            )
        }
        intent?.let {
            if (it.action.equals(ACTION_STOP_SERVICE)) {
                Log.i("Test321", "Action Stop Service...")
                updateNotificationText("Turning Off MuFu Server", "MuFu Server")
                stopTheService()
                return START_NOT_STICKY
            }
        }
        Singleton.isServerRunning = true
        startKtor()
        serverDataUseCase.initPendingWork(scopeSMSSender)
        return START_STICKY
    }

    private fun startKtor() {
        scopeServer.launch(Dispatchers.IO) {
            try {
                nettyAppliCationEngine =
                    embeddedServer(Netty, portNumber ?: AppConstant.HTTP_SERVER_PORT) {
                        routing {
                            install(ContentNegotiation) {
                                gson {}
                            }

                            val connectedAddress =
                                "${AppUtil.getIpAddress(context = applicationContext)}:${portNumber ?: AppConstant.HTTP_SERVER_PORT}"
                            _serverConnectivityData.postValue(ResultData.Success(connectedAddress))
                            updateNotificationText("MuFu", "MuFu Server is Active!..")
                            /*Download Audio Files Route*/
                            route("/tea_api/api-automations.php", HttpMethod.Post) {
                                handle {
                                    val formParameters = call.receiveParameters()
                                    val action = formParameters["act"].toString()
                                    val token = formParameters["ak"].toString()
                                    call.respondText(getOkayResponse(action))
                                    if (isTokenValid(token)) {
                                        val mediaUrls = formParameters["media_urls"].toString()
                                        val deleteExisting =
                                            formParameters["reset"].toString() != "0"
                                        Log.i("Test321", "Act = $action")
                                        Log.i("Test321", "Media Urls = $mediaUrls")
                                        downloadAudiosUseCase.invoke(
                                            mediaUrls,
                                            action = action,
                                            deleteExisting,
                                            scopeFileDownloader
                                        )
                                    }
                                }
                            }

                            route("/tea_api/api-automations.php", HttpMethod.Get) {
                                handle {
                                    val action = call.parameters["act"].toString()
                                    val token = call.parameters["ak"].toString()
                                    call.respondText(getOkayResponse(action))
                                    if (isTokenValid(token)) {
                                        when (action) {
                                            APIConstant.GET_ACTION_PLAY_MEDIA -> {
                                                routePlayMedia(call.parameters)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        install(StatusPages) {
                            status(HttpStatusCode.NotFound) { call, status ->
                                call.respondText(text = "404: Page Not Found", status = status)
                            }

                            exception<Throwable> { call, cause ->
                                Log.e("Test321", "Ktor - Exception Block ")
                                if (cause is AuthorizationException) {
                                    call.respondText(
                                        text = "403: $cause",
                                        status = HttpStatusCode.Forbidden
                                    )
                                } else {
                                    call.respondText(
                                        text = "500: $cause",
                                        status = HttpStatusCode.InternalServerError
                                    )
                                }
                            }
                        }

                    }.start(wait = true)

            } catch (e: java.lang.Exception) {
                stopTheService()
                _serverConnectivityData.postValue(ResultData.Exception(e))
                updateNotificationText(
                    "MuFu Configuration Failed",
                    "Please Try Again with Correct configuration"
                )
                Log.e("Test321", "Exception in starting Ktor - ${e.message}")
            }
        }
    }

    fun stopTheService() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            stopForeground(STOP_FOREGROUND_DETACH)
//        } else {
//
//        }
        stopForeground(true)
        stopSelf();
        stopSelfResult(startId)
        releaseResources()
        Singleton.isServerRunning = false
        NotificationManagerCompat.from(applicationContext).cancel(HTTPSERVICE_NOTIFICATION_ID)
    }

    private fun getOkayResponse(action: String): String {
        val response = MufuResponseGson(MuFUResponse("command received: $action", "ok"))
        return gson.toJson(response)
    }

    private fun routePlayMedia(parameters: Parameters) {
        val fileName = parameters["file"]
        val volume = parameters["volume"]?.toInt()
        val playMode = parameters["play_mode"]
        val duration = parameters["duration"]?.toInt()
        playAudioUseCase.invoke(
            PlayMedia(fileName, volume, playMode, duration),
            applicationContext
        )
    }

    private fun isTokenValid(tokenReceived: String): Boolean {
        return tokenReceived == AppConstant.API_TOKEN
    }

    //TODO: SMS Length can be a problem
    private fun onMessageReceived(msg: String?, number: String?, smsManager: SmsManager) {
        scopeSMSSender.launch {
            smsManager.sendTextMessage(
                number, null, msg, null, null
            )
        }
    }

    private fun createNotification(context: Context): Notification {
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this, CHANNEL_ID
            ) else Notification.Builder(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(getChannel())
            builder.setChannelId(CHANNEL_ID)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val notBuilder = builder.setContentTitle("MuFu")
            .setContentText("Configuring MuFu Server")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setAutoCancel(false)
            .setTicker("MuFu").build()
        notificationBuilder = builder
        return notBuilder
    }

    private fun updateNotificationText(msg: String, title: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder?.let {
            it.setContentTitle(
                msg
            ).setContentText(title)
            notificationManager.notify(HTTPSERVICE_NOTIFICATION_ID, it.build());
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getChannel() = NotificationChannel(
        CHANNEL_ID, "Http Server", NotificationManager.IMPORTANCE_HIGH
    )

    class AuthorizationException : Exception() {
        val status = HttpStatusCode.Forbidden
    }

    private fun releaseResources() {
        nettyAppliCationEngine?.stop()
        server = null
        scopeServer.cancel()
        scopeFileDownloader.cancel()
        scopeSMSSender.cancel()
        playAudioUseCase.onDestroy()
    }

    override fun onDestroy() {
        Singleton.isServerRunning = false
        Log.i("Test321", "onDestroy Called")
        super.onDestroy()
    }


    companion object {
        const val ACTION_STOP_SERVICE = "Stop_Service"
        const val EXTRA_PORT_NUMBER = "PortNumber"
    }
}