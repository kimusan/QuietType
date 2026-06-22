package dk.schulz.voiceme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dk.schulz.voiceme.R
import dk.schulz.voiceme.ui.theme.VoiceMeTheme

@Composable
fun VoiceMeApp() {
    VoiceMeTheme {
        VoiceMeHomeScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMeHomeScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Private voice dictation that stays out of the way.",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "VoiceMe will guide you through microphone access, local model setup, and the floating dictation control.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(onClick = { /* Onboarding navigation will be added next. */ }) {
                Text("Start setup")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceMeHomeScreenPreview() {
    VoiceMeTheme(dynamicColor = false) {
        VoiceMeHomeScreen()
    }
}
