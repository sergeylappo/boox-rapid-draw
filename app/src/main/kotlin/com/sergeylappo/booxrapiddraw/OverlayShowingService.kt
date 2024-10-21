package com.sergeylappo.booxrapiddraw

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import kotlin.system.exitProcess

private const val CHANNEL_ID = "rapid_draw_channel_overlay_01"
private const val STROKE_WIDTH = 3.0f

//TODO need to detect pen-up event to increase responsiveness, so that can start raw drawing while reading that pen is near
//Or would require schedule or timer to clean-up after retrieving a pen-down event
class OverlayShowingService : Service() {

    companion object {
        private const val PREFS_NAME = "MyServicePrefs"
        private const val KEY_IS_RUNNING = "isRunning"
    }

    private val paint = Paint()

    private lateinit var touchHelper: TouchHelper
    private lateinit var wm: WindowManager
    private lateinit var overlayPaintingView: SurfaceView

    @Volatile
    private var enabledLiveWriting: Boolean = false


    override fun onBind(intent: Intent) = null

    override fun onCreate() {
        super.onCreate()

        createForegroundNotification()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        createOverlayPaintingView()

        initPaint()
        initSurfaceView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            exitProcess(0)
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)

        if (isRunning) {
            Toast.makeText(this, "Terminating Rapid Draw Service...", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY // Prevents service from being recreated
        }

        // Set the flag to indicate that the service is now running
        prefs.edit().putBoolean(KEY_IS_RUNNING, true).apply()

        Toast.makeText(this, "Starting Rapid Draw Service", Toast.LENGTH_SHORT).show()
        return START_STICKY // Service will be recreated if killed
    }

    private fun createForegroundNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Boox Rapid draw overlay service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        // add notification intent to finish the service
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, OverlayShowingService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_service_notification_content_title))
            .setContentText(getString(R.string.overlay_service_notification_content))
            .setSmallIcon(R.drawable.rapid_draw)
            .setContentIntent(pendingIntent)
            .build()

        //noinspection InlinedApi (Seems to work, IDK why, maybe older Android versions might not support this)
        ServiceCompat.startForeground(this, 1, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    private fun createOverlayPaintingView() {
        overlayPaintingView = SurfaceView(this)
        overlayPaintingView.setZOrderOnTop(true)
        overlayPaintingView.holder.setFormat(PixelFormat.TRANSPARENT)
        overlayPaintingView.alpha = 1.0f

        val topLeftParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSPARENT
        )
        topLeftParams.alpha = 0.2f
        topLeftParams.gravity = Gravity.START or Gravity.TOP

        //        TODO this is duplicated
        //        TODO actual bottom place is calculated incorrectly due to the status bar...
        val displayMetrics = resources.displayMetrics
        val bounds = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)

        topLeftParams.x = bounds.left
        topLeftParams.y = bounds.top

        topLeftParams.width = bounds.width()
        topLeftParams.height = bounds.height() - 30
        wm.addView(overlayPaintingView, topLeftParams)
    }


    private fun initPaint() {
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = STROKE_WIDTH
    }


    //    TODO fix suppress
    @SuppressLint("ClickableViewAccessibility")
    private fun initSurfaceView() {
        touchHelper = TouchHelper.create(overlayPaintingView, 2, callback)
        touchHelper.setStrokeColor(Color.BLACK)
        touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
        touchHelper.openRawDrawing()

        val displayMetrics = resources.displayMetrics
        val bounds = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)

        overlayPaintingView.addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {

                overlayPaintingView.getLocalVisibleRect(bounds)
                touchHelper.setStrokeWidth(STROKE_WIDTH).setLimitRect(bounds, listOf())
                touchHelper.setRawInputReaderEnable(!touchHelper.isRawDrawingInputEnabled)
                overlayPaintingView.addOnLayoutChangeListener(this)
            }
        })

        overlayPaintingView.setOnTouchListener { _: View?, _: MotionEvent? -> true }
    }

    override fun onDestroy() {
        super.onDestroy()
        wm.removeView(overlayPaintingView)

        // Reset the flag when the service is destroyed
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_RUNNING, false).apply()
    }


    private val callback: RawInputCallback = object : RawInputCallback() {
        override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
            if (!enabledLiveWriting) {
                touchHelper.isRawDrawingRenderEnabled = true
                enabledLiveWriting = true
                disableFingerTouch(applicationContext)
            }
        }

        override fun onEndRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
            enableFingerTouch(applicationContext)
        }

        override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint?) {}

        override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList) {}

        override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint?) {}

        override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint?) {}

        override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint?) {}

        override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList?) {}

        override fun onPenUpRefresh(refreshRect: RectF?) {
            if (enabledLiveWriting) {
                touchHelper.isRawDrawingRenderEnabled = false
                enabledLiveWriting = false
            }
            super.onPenUpRefresh(refreshRect)
        }
    }
}


private fun disableFingerTouch(context: Context) {
    val width = context.resources.displayMetrics.widthPixels
    val height = context.resources.displayMetrics.heightPixels
    val rect = Rect(0, 0, width, height)
    val arrayRect = arrayOf(rect)
    EpdController.setAppCTPDisableRegion(context, arrayRect)
}

private fun enableFingerTouch(context: Context) {
    EpdController.appResetCTPDisableRegion(context)
}