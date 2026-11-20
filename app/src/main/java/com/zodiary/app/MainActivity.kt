package com.zodiary.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zodiary.app.ui.theme.ZodiaryColors
import com.zodiary.app.ui.theme.ZodiaryTheme

class MainActivity : ComponentActivity() {
    private val zodiaryViewModel: ZodiaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        zodiaryViewModel.handleAuthDeepLink(intent)

        setContent {
            ZodiaryTheme {
                ZodiaryApp(zodiaryViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        zodiaryViewModel.handleAuthDeepLink(intent)
    }
}

@Composable
private fun ZodiaryApp(model: ZodiaryViewModel) {
    val hasMainNavigation = model.destination in setOf(
        Destination.Home,
        Destination.Journal,
        Destination.Calendar,
        Destination.Profile,
        Destination.Reading,
    )

    Scaffold(
        containerColor = if (hasMainNavigation) ZodiaryColors.Purple else ZodiaryColors.Canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (hasMainNavigation) ZodiaryReferenceScreens.Navigation(model.destination, model::navigate)
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (model.destination) {
                Destination.Splash -> ZodiaryReferenceScreens.Splash { model.navigate(Destination.Welcome) }
                Destination.Welcome -> ZodiaryReferenceScreens.Welcome(model)
                Destination.Birthday -> ZodiaryReferenceScreens.Birthday(model)
                Destination.SignReveal -> ZodiaryReferenceScreens.SignReveal(model)
                Destination.Preferences -> ZodiaryReferenceScreens.Preferences(model)
                Destination.Trust -> ZodiaryReferenceScreens.Trust(onContinue = model::finishOnboarding)
                Destination.Home -> ZodiaryReferenceScreens.Home(model)
                Destination.Journal -> ZodiaryReferenceScreens.Journal(model)
                Destination.Calendar -> ZodiaryReferenceScreens.Calendar(model)
                Destination.Profile -> ZodiaryReferenceScreens.Profile(model)
                Destination.Reading -> ZodiaryReferenceScreens.Reading(model, onBack = { model.navigate(Destination.Home) })
                Destination.Login -> ZodiaryReferenceScreens.Login(model)
                Destination.Register -> ZodiaryReferenceScreens.Register(model)
            }
        }
    }
}
