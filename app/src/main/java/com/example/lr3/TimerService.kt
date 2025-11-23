package com.example.lr3

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = TimerBinder()
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val NOTIFICATION_ID = 101

    private val timers = mutableMapOf<Int, StudentTimer>()
    private var generalTimerMillis = 0L
    private var isGeneralTimerRunning = false // Флаг для управления общим таймером

    var timerUpdateListener: ((List<StudentTimer>) -> Unit)? = null
    var generalTimerUpdateListener: ((Long) -> Unit)? = null

    private val timerHandler = Handler(Looper.getMainLooper())
    private var isHandlerRunning = false

    private val timerRunnable: Runnable = object : Runnable {
        override fun run() {
            var anythingRunning = false

            // 1. Проверяем, должен ли работать общий таймер
            if (isGeneralTimerRunning) {
                generalTimerMillis += 1000L
                generalTimerUpdateListener?.invoke(generalTimerMillis)
                anythingRunning = true
            }

            // 2. Проверяем, работают ли таймеры студентов
            val studentTimersRunning = timers.values.any { it.isRunning }
            if (studentTimersRunning) {
                timers.values.forEach {
                    if (it.isRunning) {
                        it.currentTimeMillis += 1000L
                    }
                }
                timerUpdateListener?.invoke(timers.values.toList())
                anythingRunning = true
            }

            // 3. Если хоть что-то работает, продолжаем цикл
            if (anythingRunning) {
                timerHandler.postDelayed(this, 1000L)
            } else {
                // Если все остановлено, останавливаем цикл и убираем уведомление
                isHandlerRunning = false
                stopForeground(true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (timers.isEmpty()) {
            timers[1] = StudentTimer(1, "Студент №1")
            timers[2] = StudentTimer(2, "Студент №2")
            timers[3] = StudentTimer(3, "Студент №3")
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Убрали запуск таймера отсюда. Теперь он запускается только по команде.
        return START_NOT_STICKY
    }

    fun getTimers(): List<StudentTimer> = timers.values.toList()

    fun startTimer(id: Int) {
        timers[id]?.isRunning = true
        startTimerUpdates()
    }

    fun pauseTimer(id: Int) {
        timers[id]?.isRunning = false
    }

    fun resetTimer(id: Int) {
        timers[id]?.apply {
            isRunning = false
            currentTimeMillis = 0L
        }
        timerUpdateListener?.invoke(timers.values.toList())
    }

    fun startAllTimers() {
        // "ОБЩ ПУСК" теперь запускает и таймеры студентов, и общий таймер
        isGeneralTimerRunning = true
        timers.values.forEach { it.isRunning = true }
        startTimerUpdates()
    }

    fun stopAllTimers() {
        // "ОБЩ СТОП" останавливает и таймеры студентов, и общий таймер
        isGeneralTimerRunning = false
        timers.values.forEach { it.isRunning = false }
        timerUpdateListener?.invoke(timers.values.toList())
    }

    private fun startTimerUpdates() {
        if (!isHandlerRunning) {
            isHandlerRunning = true
            startForegroundService() // Переводим службу в foreground перед запуском
            timerHandler.post(timerRunnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Timer Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Таймеры запущены")
            .setContentText("Идет подсчет времени...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}
