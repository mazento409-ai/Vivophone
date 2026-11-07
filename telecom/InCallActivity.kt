package com.example.vivophone.telecom

import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InCallScreen { finish() } }
    }
}

@Composable
fun InCallScreen(onDone: () -> Unit) {
    val state by CallManager.state.collectAsState()
    val muted by CallManager.muted.collectAsState()
    val speaker by CallManager.speaker.collectAsState()
    var held by remember { mutableStateOf(false) }
    var showPad by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    val number = remember { CallManager.number }

    LaunchedEffect(state) {
        if (state == Call.STATE_DISCONNECTED) {
            delay(600)
            onDone()
        }
        if (state == Call.STATE_ACTIVE) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    val label = when (state) {
        Call.STATE_RINGING -> "Incoming call"
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
        Call.STATE_HOLDING -> "On hold"
        Call.STATE_ACTIVE -> "%02d:%02d".format(seconds / 60, seconds % 60)
        else -> "Call ended"
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B3A4B), Color(0xFF0B0F14))))
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))
            Box(Modifier.size(112.dp).background(Color(0x33FFFFFF), CircleShape), Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(number, color = Color.White, fontSize = 30.sp)
            Text(label, color = Color(0xCCFFFFFF), fontSize = 16.sp)
            Spacer(Modifier.weight(1f))

            if (state == Call.STATE_RINGING) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    RoundBtn(Icons.Default.CallEnd, Color(0xFFE53935)) { CallManager.reject() }
                    RoundBtn(Icons.Default.Call, Color(0xFF34C759)) { CallManager.answer() }
                }
            } else {
                if (showPad) {
                    listOf("123", "456", "789", "*0#").forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            row.forEach { ch ->
                                Box(
                                    Modifier.size(64.dp)
                                        .background(Color(0x22FFFFFF), CircleShape)
                                        .clickable { CallManager.dtmf(ch) },
                                    Alignment.Center
                                ) { Text(ch.toString(), color = Color.White, fontSize = 24.sp) }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    Toggle(Icons.Default.MicOff, "Mute", muted) { CallManager.mute(!muted) }
                    Toggle(Icons.Default.VolumeUp, "Speaker", speaker) { CallManager.speaker(!speaker) }
                    Toggle(Icons.Default.Pause, "Hold", held) { held = !held; CallManager.hold(held) }
                    Toggle(Icons.Default.Dialpad, "Keypad", showPad) { showPad = !showPad }
                }
                Spacer(Modifier.height(32.dp))
                RoundBtn(Icons.Default.CallEnd, Color(0xFFE53935)) { CallManager.hangup() }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(72.dp).background(color, CircleShape).clickable(onClick = onClick),
        Alignment.Center
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
}

@Composable
private fun Toggle(icon: ImageVector, text: String, on: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(60.dp)
                .background(if (on) Color(0xFF34C759) else Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            Alignment.Center
        ) { Icon(icon, null, tint = Color.White) }
        Text(text, color = Color(0xCCFFFFFF), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
