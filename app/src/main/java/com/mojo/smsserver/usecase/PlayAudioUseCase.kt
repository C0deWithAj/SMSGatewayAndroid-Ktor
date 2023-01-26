package com.mojo.smsserver.usecase

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.net.wifi.WifiManager.WifiLock
import android.os.PowerManager
import android.util.Log
import com.mojo.smsserver.data.model.playmedia.PlayMedia
import com.mojo.smsserver.util.APIConstant
import com.mojo.smsserver.util.AppConstant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.*
import javax.inject.Inject

class PlayAudioUseCase @Inject constructor(
    private val wifiLock: WifiLock?,
    private val audioManager: AudioManager

) : MediaPlayer.OnErrorListener {

    private val TAG = "PlayAudioUseCase"

    private var isSinglePlay = false
    private var mediaPlayer: MediaPlayer? = null
    private var coroutineScope: CoroutineScope? = null
    private val mediaQueue: Queue<PlayMedia?> = LinkedList()
    private var isPlaying = false
    private lateinit var context: Context
    private var jobTimer: Job? = null

    /**
     * Media Player with Wake Lock
     */
    fun invoke(
        playMedia: PlayMedia?,
        context: Context
    ) {
        this.context = context
        if (coroutineScope == null)
            coroutineScope = CoroutineScope(Dispatchers.IO + Job())
        mediaQueue.add(playMedia)
        initOffLoadingPlayList()
    }

    /**
     * Start Off-Loading Media from Play list one by One
     */
    private fun initOffLoadingPlayList() {
        if (isPlaying) {
            Log.i("Test321", "Already Playing - Queue Size ${mediaQueue.size}")
            return
        }

        if (mediaQueue.size > 0) {
            Log.i("Test321", "Start Offloading Queue")
            val mediaFile = mediaQueue.remove()
            mediaFile?.let { mediaFile ->
                mediaFile.playMode?.let {
                    isSinglePlay = it.equals(APIConstant.PLAY_MODE_ONCE, ignoreCase = true)
                    Log.i("Test321", "is Single Play $isSinglePlay")
                }
                prepareFileAndPlay(mediaFile)
            } ?: kotlin.run {
                initOffLoadingPlayList()
            }
        } else {
            Log.i("Test321", "Queue Size ${mediaQueue.size} - Release resources")
            releaseResources()
        }
    }

    private fun prepareFileAndPlay(media: PlayMedia) {
        val file =
            File("${AppConstant.getAudioFolderFilePath(context)}${File.separator}${media.fileName}")
        if (file.exists()) {
            Log.i("Test321", "$TAG File exist..")
            val uri = Uri.fromFile(file)
            wifiLock?.acquire()
            setVolumeLevel(media.volume)
            if (!isSinglePlay)
                startDurationWatcher(media.duration)
            playMedia(uri, context = context)
        } else {
            Log.i("Test321", "file Doesn't exist")
            initOffLoadingPlayList()
        }
    }


    private fun startDurationWatcher(durationSec: Int?) {
        durationSec?.let {
            if (durationSec > 0) {
                jobTimer = coroutineScope?.launch(Dispatchers.IO) {
                    val timer = (0..durationSec)
                        .asSequence()
                        .asFlow()
                        .onEach { delay(1_000) } // specify delay
                    timer.cancellable().collect {
                        if (it == durationSec)
                            finishedPlayingCurrentFile()
                    }
                }
            }
        }
    }

    private fun setVolumeLevel(volumeLevel: Int?) {
        volumeLevel?.let {
            if (volumeLevel > 0) {
                var level = if (it > 100) 100 else it
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                level = (maxVolume * level) / 100
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0);
            }
        }
    }

    private fun playMedia(uri: Uri, context: Context) {
        //TODO: Set Volume Level
        isPlaying = true
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener(playerCompletionListener)
            setOnPreparedListener(playerPreparedListener)
            setDataSource(context, uri)
            prepare() // might take long! (for buffering, etc)
            start()
        }
    }


    private val playerPreparedListener = OnPreparedListener {

    }

    private val playerCompletionListener = OnCompletionListener {
        if (isSinglePlay) {
            Log.d("Test321", "$TAG - CompletionListener - Single File ")

            finishedPlayingCurrentFile()
        } else {
            Log.d("Test321", "$TAG - start playing Again...")
            mediaPlayer?.start() //Repeat
        }
    }

    private fun finishedPlayingCurrentFile() {
        mediaPlayer?.stop()
        isPlaying = false
        jobTimer?.cancel()
        initOffLoadingPlayList()
    }

    /*
    Release wake Locks when finished playing
     */
    private fun releaseResources() {
        wifiLock?.let {
            if (it.isHeld)
                it.release()
        }
        jobTimer?.cancel()
        mediaPlayer?.release()
        coroutineScope?.cancel()
        coroutineScope = null
    }

    fun onDestroy() {
        releaseResources()
    }

    override fun onError(p0: MediaPlayer?, what: Int, extra: Int): Boolean {
        when (what) {
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.e(
                "Test321",
                "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra"
            )
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> Log.e(
                "Test321",
                "MEDIA ERROR SERVER DIED $extra"
            )
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> Log.e(
                "Test321",
                "MEDIA ERROR UNKNOWN $extra"
            )
        }
        Log.e("Test321", "$TAG - On Error...")
        finishedPlayingCurrentFile()
        return true
    }


}