package com.finguia

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.ComponentActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private var jaAvancou = false
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutSeguranca = Runnable { irParaMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK

            val frame = FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            val videoView = VideoView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            }
            frame.addView(videoView)
            setContentView(frame)

            val uri = Uri.parse("android.resource://${packageName}/${R.raw.logo}")
            videoView.setVideoURI(uri)

            videoView.setOnPreparedListener { mp ->
                try { mp.isLooping = false } catch (_: Throwable) {}
            }
            videoView.setOnCompletionListener { irParaMain() }
            videoView.setOnErrorListener { _, _, _ ->
                irParaMain()
                true
            }

            videoView.start()

            // Fallback: se nada acontecer em 6s, avança mesmo assim
            handler.postDelayed(timeoutSeguranca, 6000)
        } catch (_: Throwable) {
            irParaMain()
        }
    }

    private fun irParaMain() {
        if (jaAvancou) return
        jaAvancou = true
        handler.removeCallbacks(timeoutSeguranca)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeoutSeguranca)
        super.onDestroy()
    }
}
