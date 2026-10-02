package com.poolaimguide.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.poolaimguide.app.R
import com.poolaimguide.app.ui.OverlayCanvasView

/**
 * Foreground Service responsible for:
 * 1. Managing the floating toggle bubble (can be dragged anywhere)
 * 2. Showing/Hiding the full-screen transparent overlay
 * 3. Toggling touch pass-through (FLAG_NOT_TOUCHABLE) so game underneath receives touches
 */
class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    
    // Floating bubble view (small toggle icon)
    private var floatingBubbleView: View? = null
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null

    // Full screen overlay canvas view
    private var overlayCanvasView: OverlayCanvasView? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null

    private var isOverlayVisible = false
    private var isTouchPassThrough = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        setupFloatingBubble()
        setupOverlayCanvas()
    }

    private fun startForegroundNotification() {
        val channelId = "pool_aim_guide_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pool Aim Guide Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pool Aim Guide is Active")
            .setContentText("Tap floating icon to adjust aim lines")
            .setSmallIcon(R.drawable.ic_pool_ball)
            .setOngoing(true)
            .build()

        startForeground(101, notification)
    }

    /**
     * Initializes the small floating bubble toggle button
     */
    private fun setupFloatingBubble() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        floatingBubbleView = LayoutInflater.from(this).inflate(R.layout.view_floating_bubble, null)
        
        // Setup dragging for the floating bubble
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingBubbleView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleLayoutParams?.x ?: 0
                    initialY = bubbleLayoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    bubbleLayoutParams?.x = initialX + dx
                    bubbleLayoutParams?.y = initialY + dy
                    windowManager.updateViewLayout(floatingBubbleView, bubbleLayoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val clickThreshold = 10
                    val movedX = Math.abs(event.rawX - initialTouchX)
                    val movedY = Math.abs(event.rawY - initialTouchY)
                    if (movedX < clickThreshold && movedY < clickThreshold) {
                        // User tapped bubble: toggle overlay visibility
                        toggleOverlay()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(floatingBubbleView, bubbleLayoutParams)
    }

    /**
     * Initializes the full-screen transparent canvas view for aim lines and control panel
     */
    private fun setupOverlayCanvas() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Full screen transparent overlay
        overlayLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayCanvasView = OverlayCanvasView(this).apply {
            // Callback when user toggles pass-through touches on control panel
            onPassThroughToggleListener = { passThrough ->
                setTouchPassThrough(passThrough)
            }
            onCloseListener = {
                hideOverlay()
            }
        }
    }

    private fun toggleOverlay() {
        if (isOverlayVisible) {
            hideOverlay()
        } else {
            showOverlay()
        }
    }

    private fun showOverlay() {
        if (!isOverlayVisible && overlayCanvasView != null) {
            windowManager.addView(overlayCanvasView, overlayLayoutParams)
            isOverlayVisible = true
        }
    }

    private fun hideOverlay() {
        if (isOverlayVisible && overlayCanvasView != null) {
            windowManager.removeView(overlayCanvasView)
            isOverlayVisible = false
        }
    }

    /**
     * Dynamic touch pass-through configuration:
     * When pass-through is active, FLAG_NOT_TOUCHABLE allows taps to pass directly to the game.
     */
    private fun setTouchPassThrough(passThrough: Boolean) {
        isTouchPassThrough = passThrough
        val params = overlayLayoutParams ?: return
        if (passThrough) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }
        if (isOverlayVisible && overlayCanvasView != null) {
            windowManager.updateViewLayout(overlayCanvasView, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        if (floatingBubbleView != null) {
            windowManager.removeView(floatingBubbleView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}