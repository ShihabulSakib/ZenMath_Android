package com.example

import android.app.ActivityManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.ZenMathApp

/**
 * ZenMath - Minimalist Mental Arithmetic Trainer
 * Native Android Edition
 *
 * Inspired by Shihabul Sakib's open-source ZenMath:
 * Repository: https://github.com/ShihabulSakib/zenmath
 * Original Creator: Shihabul Sakib
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupTaskDescription()

        setContent {
            ZenMathApp()
        }
    }

    private fun setupTaskDescription() {
        try {
            val primaryColor = Color.parseColor("#09090B")
            val label = getString(R.string.app_name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val td = ActivityManager.TaskDescription.Builder()
                    .setIcon(R.drawable.ic_task_icon)
                    .setLabel(label)
                    .setPrimaryColor(primaryColor)
                    .build()
                setTaskDescription(td)
            } else {
                val bitmap = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ic_task_icon)
                @Suppress("DEPRECATION")
                val td = ActivityManager.TaskDescription(label, bitmap, primaryColor)
                setTaskDescription(td)
            }
        } catch (_: Throwable) {
            // Gracefully ignore if device OEM ROM restricts custom TaskDescription
        }
    }
}
