package com.twingo.fithub

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.twingo.fithub.core.ble.BleControllerProvider
import com.twingo.fithub.data.FitHubDatabase
import com.twingo.fithub.data.repository.FitnessRepository
import com.twingo.fithub.ui.FitHubApp
import com.twingo.fithub.ui.theme.FitHUBTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )

        setContent {
            FitHUBTheme {
                val context = LocalContext.current
                val repository = remember {
                    val database = FitHubDatabase.getInstance(context.applicationContext)
                    FitnessRepository(
                        profileDao = database.profileDao(),
                        workoutDao = database.workoutDao(),
                    )
                }
                val bleController = remember {
                    BleControllerProvider.get(context.applicationContext)
                }

                FitHubApp(
                    repository = repository,
                    bleController = bleController,
                )
            }
        }
    }
}
