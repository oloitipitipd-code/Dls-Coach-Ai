package com.dlscoachai.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * User-authorised, local-only screen capture foundation.
 * Frames are sampled and immediately closed; no recording, upload, audio capture,
 * game input, or tactical interpretation is performed in this stage.
 */
class LiveCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var workerThread: HandlerThread? = null
    private var frameCount = 0
    private var lastSampleMs = 0L
    private var lastAverageBrightness = -1
    private var lastNotificationUpdateMs = 0L
    private val sampleIntervalMs = 1_000L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture("Capture stopped by user")
            setCaptureState(false, "Capture stopped")
            stopSelf()
            return START_NOT_STICKY
        }
        if (projection != null) return START_NOT_STICKY

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent
        }
        if (resultData == null) {
            setCaptureState(false, "Capture permission data was unavailable")
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildNotification("Waiting for the first frame…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, resultData)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture("Capture stopped by Android or user", stopProjection = false)
                    stopSelf()
                }
            }, Handler(mainLooper))

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            // Keep capture lightweight on entry-level phones while preserving aspect ratio.
            val sourceWidth = metrics.widthPixels.coerceAtLeast(1)
            val sourceHeight = metrics.heightPixels.coerceAtLeast(1)
            val scale = minOf(1f, 960f / maxOf(sourceWidth, sourceHeight).toFloat())
            val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
            val height = (sourceHeight * scale).toInt().coerceAtLeast(1)
            val density = metrics.densityDpi

            frameCount = 0
            lastSampleMs = 0L
            lastAverageBrightness = -1
            lastNotificationUpdateMs = 0L
            workerThread = HandlerThread("DlsCoachFrameSampler").also { it.start() }
            val handler = Handler(workerThread!!.looper)
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).also { reader ->
                reader.setOnImageAvailableListener({ source ->
                    val image = source.acquireLatestImage()
                    if (image != null) {
                        try {
                            // Sample a small pixel grid at most once per second; never retain the frame.
                            val now = SystemClock.elapsedRealtime()
                            if (lastSampleMs == 0L || now - lastSampleMs >= sampleIntervalMs) {
                                lastSampleMs = now
                                lastAverageBrightness = measureAverageBrightness(image)
                                frameCount++
                                persistFrameDiagnostics(image.width, image.height)
                            }
                            if (now - lastNotificationUpdateMs >= 10_000L) {
                                lastNotificationUpdateMs = now
                                setCaptureState(true, "Capture active · local diagnostics only")
                                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                                    .notify(NOTIFICATION_ID, buildNotification("Active · $frameCount diagnostic samples"))
                            }
                        } finally {
                            image.close()
                        }
                    }
                }, handler)
            }

            virtualDisplay = projection?.createVirtualDisplay(
                "DLS Coach AI live session",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )
            if (virtualDisplay == null) error("Android did not create the capture display.")
            setCaptureState(true, "Capture active · local frame sampling")
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, buildNotification("Capture active · frames are processed locally only"))
        } catch (error: Exception) {
            stopCapture("Capture failed: ${error.localizedMessage ?: "unknown error"}")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live coaching capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Visible status for user-authorised DLS screen capture"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: String): Notification {
        val stopIntent = Intent(this, LiveCaptureService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this, 71, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("DLS Coach AI — screen capture")
            .setContentText(status)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop capture", stopPendingIntent).build())
            .build()
    }



    private fun measureAverageBrightness(image: android.media.Image): Int {
        val plane = image.planes.firstOrNull() ?: return -1
        return CaptureFrameMetrics.averageBrightness(
            buffer = plane.buffer,
            width = image.width,
            height = image.height,
            rowStride = plane.rowStride,
            pixelStride = plane.pixelStride
        )
    }

    private fun persistFrameDiagnostics(width: Int, height: Int) {
        getSharedPreferences("dls_coach_data", Context.MODE_PRIVATE).edit()
            .putInt("capture_frame_width", width)
            .putInt("capture_frame_height", height)
            .putInt("capture_average_brightness", lastAverageBrightness)
            .putLong("capture_frames", frameCount.toLong())
            .apply()
    }

    private fun setCaptureState(active: Boolean, status: String) {
        getSharedPreferences("dls_coach_data", Context.MODE_PRIVATE).edit()
            .putBoolean("capture_active", active)
            .putString("capture_status", status)
            .putLong("capture_frames", frameCount.toLong())
            .apply()
    }

    private fun stopCapture(
        status: String = "Capture stopped",
        stopProjection: Boolean = true
    ) {
        val hadResources = projection != null || virtualDisplay != null || imageReader != null || workerThread != null
        if (!hadResources) return
        setCaptureState(false, status)
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        val activeProjection = projection
        projection = null
        if (stopProjection) activeProjection?.stop()
        workerThread?.quitSafely()
        workerThread = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        stopCapture("Capture service ended")
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.dlscoachai.app.action.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "capture_result_code"
        const val EXTRA_RESULT_DATA = "capture_result_data"
        private const val CHANNEL_ID = "dls_live_capture"
        private const val NOTIFICATION_ID = 2601
    }
}
