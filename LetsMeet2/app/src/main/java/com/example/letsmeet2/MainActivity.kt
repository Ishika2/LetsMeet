package com.example.letsmeet2


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import com.example.letsmeet2.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    // Fill the App ID of your project generated on Agora Console.
    private val appId = "9486fae3780f49dfa33aba81ac475cb4"

    // Fill the channel name.
    private val channelName = "Meeting"

    // Fill the temp token generated on Agora Console.
    private val token = "007eJxTYEivuPD43HEb7c5tc7bMXznh1O+NplesW7h8PkxnSWjaFOmgwGBpYmGWlphqbG5hkGZimZKWaGycmJRoYZiYbGJumpxkEmoxL7khkJHhiH0qKyMDBIL47Ay+qaklmXnpDAwAU3AiHA=="
    //since it's a temporary token it will expire on 18th December 9:44 UTC

    // An integer that identifies the local user.
    private val uid = 0
    private var isJoined = false

    private var agoraEngine: RtcEngine? = null

    //SurfaceView to render local video in a Container.
    private var localSurfaceView: SurfaceView? = null

    //SurfaceView to render Remote video in a Container.
    private var remoteSurfaceView: SurfaceView? = null

    private var btnToggleMute: ImageView? = null

    private var btnToggleCamera: ImageView? = null

    private var btnFlipCamera: ImageView? = null

    private lateinit var binding: ActivityMainBinding

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(R.id.remote_video_view_container)  //remote_video_view_container
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        // Display RemoteSurfaceView.
        remoteSurfaceView!!.setVisibility(View.VISIBLE)
    }

    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(R.id.local_video_view_container)  //local_video_view_container
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(baseContext)
        container.addView(localSurfaceView)
        // Pass the SurfaceView object to Agora so that it renders the local video.
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    fun joinChannel(view: View?) {
        if (checkSelfPermission()) {
            val options = ChannelMediaOptions()

            // For a Video call, set the channel profile as COMMUNICATION.
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            // Display LocalSurfaceView.
            setupLocalVideo()
            localSurfaceView!!.visibility = View.VISIBLE
            // Start local preview.
            agoraEngine!!.startPreview()
            // Join the channel with a temp token.
            // You need to specify the user ID yourself, and ensure that it is unique in the channel.
            agoraEngine!!.joinChannel(token, channelName, uid, options)
        } else {
            Toast.makeText(applicationContext, "Permissions was not granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun leaveChannel(view: View?) {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
        }
    }


    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")

            // Set the remote video view
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")
            runOnUiThread { remoteSurfaceView!!.visibility = View.GONE }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  //activity_main
        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (!checkSelfPermission())
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        setupVideoSDKEngine()

        btnToggleMute = findViewById(R.id.mic_state)

        btnToggleCamera = findViewById(R.id.camera_state)

        btnFlipCamera = findViewById(R.id.flip_camera)

        agoraEngine?.muteLocalAudioStream(false) //by default unmute

        agoraEngine?.muteLocalVideoStream(false) //by default video is on

        val isUnMute: Boolean = true  //by default it's unmute

        val isCamera: Boolean = true  //by default video is on

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        btnToggleMute?.setOnClickListener(
            View.OnClickListener {
                if(isUnMute)
                {
                    agoraEngine?.muteLocalAudioStream(true)
                    binding.micState.setImageResource(R.drawable.ic_baseline_mic_off_24)
                }
                else
                {
                    agoraEngine?.muteLocalAudioStream(false)
                    binding.micState.setImageResource(R.drawable.ic_baseline_mic_24)
                }
            }
        )

        btnToggleCamera?.setOnClickListener(
            View.OnClickListener {
                if(isCamera)
                {
                    agoraEngine?.muteLocalVideoStream(true)
                    binding.cameraState.setImageResource(R.drawable.ic_camera_off)
                }
                else
                {
                    agoraEngine?.muteLocalVideoStream(false)
                    binding.cameraState.setImageResource(R.drawable.ic_camera_on)
                }
            }
        )

        btnFlipCamera?.setOnClickListener(
            View.OnClickListener {
                agoraEngine?.switchCamera()
                binding.flipCamera.rotation = 90f
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        // Destroy the engine in a sub-thread to avoid congestion
        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

//    private fun isMute() {
//
//    }
//    fun Mute(view: View) {}
//    fun flipCamera(view: View) {
//
//    }

}