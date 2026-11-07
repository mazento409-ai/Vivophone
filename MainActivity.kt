package com.example.vivophone

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vivophone.data.CallEntry
import com.example.vivophone.data.ContactItem
import com.example.vivophone.data.Repos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

val Green = Color(0xFF34C759)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = intent?.data?.schemeSpecificPart ?: ""
        setContent {
            val scheme = if (isSystemInDarkTheme()) darkColorScheme(primary = Green)
            else lightColorScheme(primary = Green)
            MaterialTheme(colorScheme = scheme) { App(initial) }
        }
    }
}

private val perms = arrayOf(
    Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
    Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG,
    Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.POST_NOTIFICATIONS
)

fun placeCall(ctx: Context, number: String, account: PhoneAccountHandle? = null) {
    if (number.isBlank()) return
    val tm = ctx.getSystemService(TelecomManager::class.java)
    val extras = Bundle().apply {
        if (account != null) putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account)
    }
    try {
        tm.placeCall(Uri.fromParts("tel", number, null), extras)
    } catch (_: SecurityException) {
    }
}

@Composable
fun App(initialNumber: String) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(perms.all { ctx.checkSelfPermission(it) == 0 }) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { r ->
        granted = r.filterKeys { it != Manifest.permission.POST_NOTIFICATIONS }.values.all { it }
    }
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    LaunchedEffect(Unit) {
        if (!granted) permLauncher.launch(perms)
        val rm = ctx.getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    }

    var tab by remember { mutableIntStateOf(if (initialNumber.isNotEmpty()) 2 else 0) }
    var dialNumber by remember { mutableStateOf(initialNumber) }

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Default.History, null) }, label = { Text("Recents") })
            NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Default.Person, null) }, label = { Text("Contacts") })
            NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Default.Dialpad, null) }, label = { Text("Keypad") })
        }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (!granted) {
                Text("Permissions required", Modifier.align(Alignment.Center))
            } else when (tab) {
                0 -> RecentsScreen { dialNumber = it; tab = 2 }
                1 -> ContactsScreen { dialNumber = it; tab = 2 }
                else -> DialerScreen(dialNumber) { dialNumber = it }
            }
        }
    }
}

@Composable
fun RecentsScreen(onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    val log by produceState(emptyList<CallEntry>()) {
        value = withContext(Dispatchers.IO) { Repos.callLog(ctx) }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Text("Recents", fontSize = 32.sp, modifier = Modifier.padding(20.dp)) }
        items(log) { e ->
            val missed = e.type == CallLog.Calls.MISSED_TYPE
            Row(
                Modifier.fillMaxWidth().clickable { onPick(e.number) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (e.type) {
                        CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived
                        CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
                        else -> Icons.Default.CallMissed
                    },
                    null, tint = if (missed) Color.Red else Green
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        e.name ?: e.number,
                        color = if (missed) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(e.date)),
                        fontSize = 12.sp, color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsScreen(onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    val all by produceState(emptyList<ContactItem>()) {
        value = withContext(Dispatchers.IO) { Repos.contacts(ctx) }
    }
    var q by remember { mutableStateOf("") }
    val list = all.filter { q.isBlank() || it.name.contains(q, true) || it.number.contains(q) }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            q, { q = it },
            Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            placeholder = { Text("Search contacts") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(28.dp)
        )
        LazyColumn {
            items(list) { c ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(c.number) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(42.dp).background(Green.copy(alpha = .2f), CircleShape), Alignment.Center) {
                        Text(c.name.take(1).uppercase(), color = Green)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(c.name)
                        Text(c.number, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun DialerScreen(number: String, onChange: (String) -> Unit) {
    val ctx = LocalContext.current
    val accounts = remember {
        try {
            ctx.getSystemService(TelecomManager::class.java).callCapablePhoneAccounts
        } catch (_: SecurityException) {
            emptyList<PhoneAccountHandle>()
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Text(number, fontSize = 36.sp, textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(16.dp))
        listOf("123", "456", "789", "*0#").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(6.dp)) {
                row.forEach { ch ->
                    Box(
                        Modifier.size(76.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .clickable { onChange(number + ch) },
                        Alignment.Center
                    ) { Text(ch.toString(), fontSize = 28.sp) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (accounts.size > 1) {
                accounts.take(2).forEachIndexed { i, acc ->
                    Button({ placeCall(ctx, number, acc) }, shape = RoundedCornerShape(28.dp)) {
                        Icon(Icons.Default.Call, null)
                        Spacer(Modifier.width(6.dp))
                        Text("SIM ${i + 1}")
                    }
                }
            } else {
                Box(
                    Modifier.size(76.dp).background(Green, CircleShape)
                        .clickable { placeCall(ctx, number) },
                    Alignment.Center
                ) {
                    Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            IconButton({ onChange(number.dropLast(1)) }) { Icon(Icons.Default.Backspace, null) }
        }
        Spacer(Modifier.height(16.dp))
    }
}
