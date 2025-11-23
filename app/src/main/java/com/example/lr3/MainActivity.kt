package com.example.lr3

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lr3.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var timerAdapter: TimerAdapter

    private var timerService: TimerService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true

            val initialTimers = timerService?.getTimers() ?: emptyList()
            timerAdapter.updateData(initialTimers)
            timerService?.timerUpdateListener = { timerAdapter.updateData(it) }

            timerService?.generalTimerUpdateListener = { timeInMillis ->
                // ИСПРАВЛЕНО: Используем новый ID
                binding.generalTimerTextView.text = "Общее время: ${formatTime(timeInMillis)}"
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startAndBindService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupControlButtons()

        askForNotificationPermission()
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startAndBindService()
            }
        } else {
            startAndBindService()
        }
    }

    private fun startAndBindService() {
        val serviceIntent = Intent(this, TimerService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupRecyclerView() {
        timerAdapter = TimerAdapter(emptyList()) { id, action ->
            when (action) {
                TimerAdapter.TimerAction.START -> timerService?.startTimer(id)
                TimerAdapter.TimerAction.PAUSE -> timerService?.pauseTimer(id)
                TimerAdapter.TimerAction.RESET -> timerService?.resetTimer(id)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = timerAdapter
    }

    private fun setupControlButtons() {
        binding.btnStartAll.setOnClickListener { timerService?.startAllTimers() }
        binding.btnStopAll.setOnClickListener { timerService?.stopAllTimers() }
        binding.btnExit.setOnClickListener {
            if (isBound) {
                stopService(Intent(this, TimerService::class.java))
                unbindService(serviceConnection)
                isBound = false
            }
            finish()
        }
    }

    private fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}