package com.sergeylappo.booxrapiddraw

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
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
    private val paint = Paint()

    private lateinit var touchHelper: TouchHelper
    private lateinit var writingToggleButton: Button
    private lateinit var wm: WindowManager
    private lateinit var overlayPaintingView: SurfaceView

    @Volatile
    private var enabledLiveWriting: Boolean = false


    override fun onBind(intent: Intent) = null

    private val toggleRenderingGestureDetector =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleWriting()
                return true
            }
        }

    private fun toggleWriting() {
        Log.d("OverlayShowingService", "toggleWriting: $enabledLiveWriting")

        if (!enabledLiveWriting) {
            touchHelper.openRawDrawing()
            touchHelper.isRawDrawingRenderEnabled = true
            enabledLiveWriting = true
        } else {
            touchHelper.isRawDrawingRenderEnabled = false
            enabledLiveWriting = false
            touchHelper.closeRawDrawing()
        }

        updateWritingToggleButtonText()
    }

    override fun onCreate() {
        super.onCreate()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        createForegroundNotification()

        createOverlayPaintingView()
        createWritingToggleButton()

        initPaint()
        initSurfaceView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == "STOP") {
            stopSelf()
            exitProcess(0)
            return START_NOT_STICKY
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createForegroundNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rapid Draw Overlay",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        // add notification intent to finish the service
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, OverlayShowingService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rapid Draw Overlay")
            .setContentText("Rapid Draw Overlay is running")
            .setSmallIcon(R.drawable.rapid_draw)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun createWritingToggleButton() {
        val buttonParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = 0
            y = 0
        }

        writingToggleButton = Button(this)
        val gestureDetector = GestureDetector(this, toggleRenderingGestureDetector)
        writingToggleButton.text = getString(R.string.clear)
        writingToggleButton.setTextColor(Color.WHITE)

        writingToggleButton.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (gestureDetector.onTouchEvent(event)) {
                    return true
                }

                if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
                    /* You can play around with the offset to set where you want the users finger to be on the view. Currently it should be centered.*/
                    val xOffset = v.width / 2
                    val yOffset = v.height / 2
                    buttonParams.x = event.rawX.toInt() - xOffset
                    buttonParams.y = event.rawY.toInt() - yOffset

                    wm.updateViewLayout(writingToggleButton, buttonParams)
                    return true
                }
                if (event.action == MotionEvent.ACTION_BUTTON_PRESS) {
                    return false
                }
                return false
            }
        })
        writingToggleButton.setBackgroundColor(Color.BLACK)
        writingToggleButton.background.alpha = 100

        wm.addView(writingToggleButton, buttonParams)
    }

    private fun updateWritingToggleButtonText() {
        writingToggleButton.text = if (enabledLiveWriting) {
            getString(R.string.disable)
        } else {
            getString(R.string.enable)
        }
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
                val displayMetrics = resources.displayMetrics
                val bounds = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)

                if (this@OverlayShowingService::touchHelper.isInitialized != true) {
                    touchHelper = TouchHelper.create(overlayPaintingView, 2, callback).apply {
                        isRawDrawingRenderEnabled = true
                        setStrokeWidth(STROKE_WIDTH)
                        setStrokeColor(Color.BLACK)
                        setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
                        setRawInputReaderEnable(true)
                    }
                    toggleWriting()
//                    overlayPaintingView.postDelayed({
//                        touchHelper.openRawDrawing()
//                    }, 100)
                }
                overlayPaintingView.getLocalVisibleRect(bounds)
                overlayPaintingView.addOnLayoutChangeListener(this)
            }
        })

        overlayPaintingView.setOnTouchListener { _: View?, _: MotionEvent? -> true }
    }

    override fun onDestroy() {
        super.onDestroy()
        wm.removeView(overlayPaintingView)
        Log.d("OverlayShowingService", "onDestroy")
    }


    private val callback: RawInputCallback = object : RawInputCallback() {
        override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
            disableFingerTouch(applicationContext)
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