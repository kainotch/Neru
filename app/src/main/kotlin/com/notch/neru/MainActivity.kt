package com.notch.neru

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.notch.neru.navigation.SleepTimerNavHost
import com.notch.neru.theme.SleepTimerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SleepTimerTheme {
                SleepTimerNavHost()
            }
        }
    }
}
