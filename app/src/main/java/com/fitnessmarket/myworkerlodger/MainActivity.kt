package com.fitnessmarket.myworkerlodger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fitnessmarket.myworkerlodger.ui.navigation.WorkerLodgerNavHost
import com.fitnessmarket.myworkerlodger.ui.theme.MyWorkerLodgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyWorkerLodgerTheme {
                WorkerLodgerNavHost()
            }
        }
    }
}
