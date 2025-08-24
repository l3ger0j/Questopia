package org.qp.android.ui.video

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.qp.android.R

class VideoPlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    private val requestPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startPlayback()
        else finish() // user denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_video_player)
        playerView = findViewById(R.id.playerView)

        // check permission on Android 13+ (READ_MEDIA_VIDEO)
        if (Build.VERSION.SDK_INT >= 33) {
            val perm = Manifest.permission.READ_MEDIA_VIDEO
            if (ContextCompat.checkSelfPermission(this, perm)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPerm.launch(perm)
                return
            }
        }
        startPlayback()
    }

    private fun startPlayback() {
        val uriStr = intent?.getStringExtra(EXTRA_URI) ?: run { finish(); return }
        val uri = Uri.parse(uriStr)

        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            val item = MediaItem.fromUri(uri)
            exo.setMediaItem(item)
            exo.prepare()
            exo.playWhenReady = true
        }

        playerView.setControllerOnFullScreenModeChangedListener { /* optional */ }
        playerView.setOnClickListener { /* consume to keep controller visible */ }
    }

    override fun onStop() {
        super.onStop()
        playerView.player = null
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_URI = "videoUri"
    }
}
