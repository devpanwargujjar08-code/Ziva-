package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MitraBottomNavigation
import com.example.ui.components.MitraTopBar
import com.example.ui.screens.CallingScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.InstagramScreen
import com.example.ui.screens.WhatsAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel()
                MitraApp(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun MitraApp(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isVoiceSpeechEnabled by viewModel.voiceManager.isSpeechEnabled.collectAsState()

    // BackHandler: If on non-chat tab, return to chat tab on back press
    BackHandler(enabled = selectedTab != 0) {
        viewModel.setSelectedTab(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MitraTopBar(
                isSpeaking = isSpeaking,
                isListening = isListening,
                isVoiceSpeechEnabled = isVoiceSpeechEnabled,
                onToggleSpeech = { viewModel.toggleSpeechVoice() },
                onClearChat = { viewModel.clearChat() }
            )
        },
        bottomBar = {
            MitraBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.setSelectedTab(it) }
            )
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            label = "tab_crossfade",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                0 -> ChatScreen(viewModel = viewModel)
                1 -> CallingScreen(viewModel = viewModel)
                2 -> WhatsAppScreen(viewModel = viewModel)
                3 -> InstagramScreen(viewModel = viewModel)
            }
        }
    }
}
