package com.livesplits.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.livesplits.R
import com.livesplits.data.local.entity.Segment
import com.livesplits.data.settings.SettingsRepository
import com.livesplits.domain.model.ComparisonMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

/**
 * Foreground Service that displays a draggable timer overlay.
 */
@AndroidEntryPoint
class TimerOverlayService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val binder = TimerBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var timerJob: Job? = null

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var timerTextView: TextView? = null
    private var splitNameTextView: TextView? = null
    private var deltaTextView: TextView? = null

    // Timer state
    private var isRunning = false
    private var startTimeMs = 0L
    private var elapsedTimeMs = 0L
    private var countdownMs = 0L
    private var currentTimeMs = 0L
    private var currentSplitIndex = 0
    private var isFinished = false
    private var isCountingDown = false

    // Category and segments
    private var categoryId: Long = 0
    private var segments: List<Segment> = emptyList()
    private var accumulatedSplitTimes = mutableListOf<Long>()

    // Settings
    private var settings: AppSettings = AppSettings()

    // Drag handling
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var dragStartTime = 0L

    inner class TimerBinder : android.os.Binder() {
        fun getService(): TimerOverlayService = this@TimerOverlayService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Load settings
        CoroutineScope(Dispatchers.IO).launch {
            settings = settingsRepository.settings.first()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> startTimerOverlay()
            ACTION_STOP_TIMER -> stopTimerOverlay()
            ACTION_CLOSE_TIMER -> closeTimer()
            else -> startTimerOverlay()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimerUpdates()
        removeOverlay()
    }

    fun startTimerWithCategory(categoryId: Long, segments: List<Segment>) {
        this.categoryId = categoryId
        this.segments = segments.sortedBy { it.position }
        this.currentSplitIndex = 0
        this.elapsedTimeMs = 0L
        this.accumulatedSplitTimes.clear()
        this.isFinished = false
        this.countdownMs = settings.countdownMs
        this.isCountingDown = countdownMs > 0

        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.saveCurrentTimerInfo(
                categoryId = categoryId,
                segments = segments.map { it.id },
                startTimeMs = System.currentTimeMillis()
            )
        }

        if (overlayView == null) {
            startTimerOverlay()
        } else {
            startTimerUpdates()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startTimerOverlay() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Create overlay view
        val layoutInflater = LayoutInflater.from(this)
        overlayView = layoutInflater.inflate(R.layout.overlay_timer, null)

        timerTextView = overlayView?.findViewById(R.id.timerTextView)
        splitNameTextView = overlayView?.findViewById(R.id.splitNameTextView)
        deltaTextView = overlayView?.findViewById(R.id.deltaTextView)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
            
            // Setup touch listener for drag and tap
            setupTouchListener(layoutParams)
            
            // Start timer updates
            startTimerUpdates()
        } catch (e: Exception) {
            // Handle case where overlay permission not granted
            e.printStackTrace()
        }
    }

    private fun getWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(layoutParams: WindowManager.LayoutParams) {
        var isDragging = false

        overlayView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    dragStartTime = System.currentTimeMillis()
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    // Only consider it dragging if moved more than threshold
                    if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        isDragging = true
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(overlayView, layoutParams)
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val touchDuration = System.currentTimeMillis() - dragStartTime
                    val deltaX = abs(event.rawX - lastTouchX)
                    val deltaY = abs(event.rawY - lastTouchY)
                    
                    if (!isDragging && touchDuration < 300 && deltaX < 10 && deltaY < 10) {
                        // Tap - handle timer action
                        handleTimerTap()
                    } else if (!isDragging && touchDuration >= 300) {
                        // Long press
                        handleTimerLongPress()
                    }
                    
                    // Save position
                    saveTimerPosition(layoutParams.x, layoutParams.y)
                    true
                }
                else -> false
            }
        }
    }

    private fun handleTimerTap() {
        if (!isRunning && !isFinished) {
            // Start timer
            startTimer()
        } else if (isRunning) {
            if (segments.isNotEmpty() && currentSplitIndex < segments.size) {
                // Record split
                recordSplit()
            }
            if (currentSplitIndex >= segments.size || segments.isEmpty()) {
                // Stop timer
                stopTimer()
            }
        } else if (isFinished) {
            // Timer finished, do nothing or show summary
        }
    }

    private fun handleTimerLongPress() {
        if (isRunning) {
            // Reset immediately without saving
            resetTimer()
        } else if (isFinished) {
            // Offer to save PB if improved
            offerSavePb()
        }
    }

    private fun startTimer() {
        isRunning = true
        if (isCountingDown) {
            startTimeMs = System.currentTimeMillis() + countdownMs
        } else {
            startTimeMs = System.currentTimeMillis() - elapsedTimeMs
        }
        startTimerUpdates()
        updateOverlay()
    }

    private fun recordSplit() {
        if (currentSplitIndex < segments.size) {
            val splitTime = currentTimeMs - accumulatedSplitTimes.lastOrNull() ?: 0L
            accumulatedSplitTimes.add(currentTimeMs)
            
            // Update delta display
            updateDelta()
            
            currentSplitIndex++
            
            if (currentSplitIndex >= segments.size) {
                finishTimer()
            }
        }
        updateOverlay()
    }

    private fun stopTimer() {
        isRunning = false
        stopTimerUpdates()
        updateOverlay()
    }

    private fun finishTimer() {
        isRunning = false
        isFinished = true
        stopTimerUpdates()
        
        // Check if new PB
        val totalTime = currentTimeMs
        val pbTime = segments.sumOf { it.pbTimeMs }
        
        if (pbTime == 0L || totalTime < pbTime) {
            // New PB!
            updateTimerColor(ColorType.PB)
        }
        
        updateOverlay()
    }

    private fun resetTimer() {
        isRunning = false
        isFinished = false
        elapsedTimeMs = 0L
        currentSplitIndex = 0
        accumulatedSplitTimes.clear()
        isCountingDown = countdownMs > 0
        stopTimerUpdates()
        updateOverlay()
    }

    private fun offerSavePb() {
        val totalTime = elapsedTimeMs
        val pbTime = segments.sumOf { it.pbTimeMs }
        
        if (pbTime == 0L || totalTime < pbTime) {
            // New PB - save it
            CoroutineScope(Dispatchers.Main).launch {
                saveNewPb(totalTime)
            }
        }
    }

    private suspend fun saveNewPb(newTimeMs: Long) {
        // Update category PB
        // This would require repository access - simplified here
        settingsRepository.clearCurrentTimerInfo()
    }

    private fun startTimerUpdates() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                if (isRunning) {
                    if (isCountingDown && currentTimeMs > 0) {
                        currentTimeMs = startTimeMs - System.currentTimeMillis()
                        if (currentTimeMs <= 0) {
                            currentTimeMs = 0
                            isCountingDown = false
                            startTimeMs = System.currentTimeMillis()
                        }
                    } else {
                        currentTimeMs = System.currentTimeMillis() - startTimeMs
                    }
                    elapsedTimeMs = currentTimeMs
                    updateOverlay()
                }
                delay(10) // Update every 10ms
            }
        }
    }

    private fun stopTimerUpdates() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateOverlay() {
        timerTextView?.text = formatTime(currentTimeMs)
        
        // Update split name
        if (settings.showSplitName && currentSplitIndex < segments.size) {
            splitNameTextView?.text = segments[currentSplitIndex].name
            splitNameTextView?.visibility = View.VISIBLE
        } else {
            splitNameTextView?.visibility = View.GONE
        }
        
        // Update delta
        if (settings.showDelta && isRunning) {
            updateDelta()
            deltaTextView?.visibility = View.VISIBLE
        } else {
            deltaTextView?.visibility = View.GONE
        }
        
        // Update colors
        updateTimerColor()
    }

    private fun updateDelta() {
        if (currentSplitIndex < segments.size) {
            val comparison = settings.comparison
            val segment = segments[currentSplitIndex]
            
            val accumulatedTime = accumulatedSplitTimes.lastOrNull() ?: 0L
            val currentSplitTime = currentTimeMs - accumulatedTime
            
            val compareTime = when (comparison) {
                ComparisonMode.PB -> segment.pbTimeMs
                ComparisonMode.BEST_SEGMENTS -> segment.bestTimeMs
            }
            
            if (compareTime > 0) {
                val delta = currentSplitTime - compareTime
                deltaTextView?.text = formatDelta(delta)
                
                // Update color based on delta
                if (delta < 0) {
                    updateTimerColor(ColorType.AHEAD)
                } else {
                    updateTimerColor(ColorType.BEHIND)
                }
            }
        }
    }

    private fun updateTimerColor(type: ColorType? = null) {
        val color = when (type) {
            ColorType.AHEAD -> settings.colorAhead
            ColorType.BEHIND -> settings.colorBehind
            ColorType.PB -> settings.colorPb
            null -> {
                // Auto-determine based on current state
                if (isFinished && segments.isNotEmpty()) {
                    val totalTime = elapsedTimeMs
                    val pbTime = segments.sumOf { it.pbTimeMs }
                    if (pbTime > 0 && totalTime < pbTime) {
                        settings.colorPb
                    } else {
                        settings.colorAhead
                    }
                } else {
                    settings.colorAhead // Default
                }
            }
        }
        timerTextView?.setTextColor(color)
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = kotlin.math.abs(timeMs) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val milliseconds = if (settings.showMilliseconds) {
            (kotlin.math.abs(timeMs) % 1000) / 10
        } else {
            0
        }
        
        val sign = if (timeMs < 0) "-" else ""
        
        return if (settings.showMilliseconds) {
            "${sign}${minutes}:${seconds.toString().padStart(2, '0')}.${milliseconds.toString().padStart(2, '0')}"
        } else {
            "${sign}${minutes}:${seconds.toString().padStart(2, '0')}"
        }
    }

    private fun formatDelta(delta: Long): String {
        val sign = if (delta >= 0) "+" else "-"
        val absDelta = kotlin.math.abs(delta)
        val seconds = absDelta / 1000
        val milliseconds = (absDelta % 1000) / 10
        
        return "${sign}${seconds}.${milliseconds.toString().padStart(2, '0')}"
    }

    private fun saveTimerPosition(x: Int, y: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.saveTimerPosition(categoryId, x.toFloat(), y.toFloat())
        }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        overlayView = null
    }

    private fun stopTimerOverlay() {
        stopTimerUpdates()
        removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun closeTimer() {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.clearCurrentTimerInfo()
        }
        stopTimerOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows timer overlay for speedrun timing"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, TimerOverlayService::class.java).apply {
            action = ACTION_CLOSE_TIMER
        }
        
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LiveSplits Timer")
            .setContentText("Timer is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_foreground, "Close Timer", pendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "timer_service_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_TIMER = "com.livesplits.START_TIMER"
        const val ACTION_STOP_TIMER = "com.livesplits.STOP_TIMER"
        const val ACTION_CLOSE_TIMER = "com.livesplits.CLOSE_TIMER"

        fun startService(context: Context, categoryId: Long, segments: List<Segment>) {
            val intent = Intent(context, TimerOverlayService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra("category_id", categoryId)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerOverlayService::class.java).apply {
                action = ACTION_STOP_TIMER
            }
            context.startService(intent)
        }
    }
}

enum class ColorType {
    AHEAD,
    BEHIND,
    PB
}
