package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nawawia.DefaultAppContainer
import com.nawawia.NawawiaApp
import com.nawawia.ui.feature.NawawiaAppRoot
import com.nawawia.ui.feature.NawawiaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as? NawawiaApp)?.container
            ?: DefaultAppContainer(applicationContext)

        setContent {
            val viewModel: NawawiaViewModel = viewModel(
                factory = NawawiaViewModel.provideFactory(appContainer)
            )
            NawawiaAppRoot(viewModel = viewModel)
        }
    }
}
