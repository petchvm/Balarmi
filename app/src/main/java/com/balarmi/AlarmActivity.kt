package com.balarmi

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.balarmi.alarm.AlarmEvent
import com.balarmi.alarm.AlarmEvents
import com.balarmi.service.ChargingMonitorService
import com.balarmi.ui.AlarmScreen
import com.balarmi.ui.theme.BalarmiTheme
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    private var isTest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupShowWhenLocked()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        renderFromIntent(intent)

        // Service owns the unplug grace period and emits Stopped when it kills the alarm
        // (either via STOP action, unplug, or 3-min timeout).
        lifecycleScope.launch {
            AlarmEvents.events.collect { event ->
                if (event is AlarmEvent.Stopped) finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onStopPressed()
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderFromIntent(intent)
    }

    private fun renderFromIntent(intent: Intent) {
        val pct = intent.getIntExtra(EXTRA_PCT, -1)
        val threshold = intent.getIntExtra(EXTRA_THRESHOLD, -1)
        isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)
        setContent {
            BalarmiTheme {
                AlarmScreen(
                    pct = pct,
                    threshold = threshold,
                    isTest = isTest,
                    onStop = ::onStopPressed,
                )
            }
        }
    }

    private fun setupShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
    }

    private fun onStopPressed() {
        ChargingMonitorService.stopAlarm(this)
        finish()
    }

    companion object {
        const val EXTRA_PCT = "pct"
        const val EXTRA_THRESHOLD = "threshold"
        const val EXTRA_IS_TEST = "is_test"
    }
}
