package com.sony.xm6controller.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sony.xm6controller.R
import com.sony.xm6controller.data.bluetooth.ConnectionState
import com.sony.xm6controller.domain.HeadphoneRepository
import com.sony.xm6controller.domain.NoiseControlMode
import com.sony.xm6controller.service.XM6ForegroundService
import com.sony.xm6controller.ui.theme.AmbientBlue
import com.sony.xm6controller.ui.theme.AncGreen
import com.sony.xm6controller.ui.theme.CardBorder
import com.sony.xm6controller.ui.theme.OffGray
import com.sony.xm6controller.ui.theme.SonyCopper
import com.sony.xm6controller.ui.theme.SonyGold
import com.sony.xm6controller.ui.theme.XM6ControllerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: HeadphoneRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = HeadphoneRepository.getInstance(applicationContext)

        setContent {
            XM6ControllerTheme {
                MainScreen(repository = repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repository: HeadphoneRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val connectionState by repository.connectionState.collectAsState()
    val noiseControlState by repository.noiseControlState.collectAsState()
    val targetDevice by repository.targetDevice.collectAsState()
    val diagnosticLogs by repository.diagnosticLogs.collectAsState()

    var foregroundServiceEnabled by remember { mutableStateOf(false) }

    // Permission handling
    var hasPermissions by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val connectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else true
        hasPermissions = connectGranted
        if (connectGranted) {
            repository.refreshTargetDevice()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            val needed = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                needed.add(Manifest.permission.BLUETOOTH_CONNECT)
                needed.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (needed.isNotEmpty()) {
                permissionLauncher.launch(needed.toTypedArray())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_headphones),
                            contentDescription = null,
                            tint = SonyGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sony WH-1000XM6",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Noise Control Tile Hub",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Permission Card (if missing)
            if (!hasPermissions) {
                item {
                    PermissionRequestCard(
                        onRequestPermissions = {
                            val needed = mutableListOf<String>()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                needed.add(Manifest.permission.BLUETOOTH_CONNECT)
                                needed.add(Manifest.permission.BLUETOOTH_SCAN)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                needed.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(needed.toTypedArray())
                        }
                    )
                }
            }

            // 2. Device Status Dashboard
            item {
                DeviceStatusCard(
                    connectionState = connectionState,
                    targetDevice = targetDevice,
                    repository = repository,
                    onConnect = { dev ->
                        scope.launch {
                            val result = repository.connect(dev)
                            if (result.isFailure) {
                                Toast.makeText(
                                    context,
                                    "Connect failed: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onDisconnect = {
                        scope.launch { repository.disconnect() }
                    }
                )
            }

            // 3. Noise Control Mode Card & Manual Toggles
            item {
                NoiseControlCard(
                    currentMode = noiseControlState.mode,
                    isConnected = connectionState.isConnected,
                    onSelectMode = { mode ->
                        scope.launch {
                            val res = repository.setMode(mode)
                            if (res.isFailure) {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onCycleMode = {
                        scope.launch {
                            val res = repository.cycleMode()
                            if (res.isFailure) {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // 4. ColorOS & Oppo Find X9 Ultra Background Optimization Card
            item {
                ColorOSOptimizationCard(
                    foregroundServiceEnabled = foregroundServiceEnabled,
                    onToggleForegroundService = { enabled ->
                        foregroundServiceEnabled = enabled
                        if (enabled) {
                            XM6ForegroundService.start(context)
                            Toast.makeText(context, "Persistent background service started", Toast.LENGTH_SHORT).show()
                        } else {
                            XM6ForegroundService.stop(context)
                            Toast.makeText(context, "Persistent background service stopped", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 5. Live MDR Protocol Diagnostics
            item {
                DiagnosticsCard(logs = diagnosticLogs)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun PermissionRequestCard(onRequestPermissions: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bluetooth Permissions Required",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Android 14+ requires Nearby Devices permission (BLUETOOTH_CONNECT) to communicate with your Sony WH-1000XM6 over RFCOMM SPP.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusCard(
    connectionState: ConnectionState,
    targetDevice: BluetoothDevice?,
    repository: HeadphoneRepository,
    onConnect: (BluetoothDevice?) -> Unit,
    onDisconnect: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val pairedDevices = remember { repository.getPairedDevices() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Headphone Connection",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                // Connection Status Pill
                StatusPill(connectionState = connectionState)
            }

            Spacer(modifier = Modifier.height(12.dp))

            @SuppressLint("MissingPermission")
            val currentName = targetDevice?.name ?: "WH-1000XM6 (Searching...)"
            @SuppressLint("MissingPermission")
            val currentAddress = targetDevice?.address ?: "No Paired Device"

            Text(
                text = currentName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = currentAddress,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Paired devices dropdown
            if (pairedDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = targetDevice?.name ?: "Select Paired Device",
                        onValueChange = {},
                        label = { Text("Selected Device") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        pairedDevices.forEach { dev ->
                            @SuppressLint("MissingPermission")
                            val devName = dev.name ?: "Unknown Device"
                            DropdownMenuItem(
                                text = { Text("$devName (${dev.address})") },
                                onClick = {
                                    repository.refreshTargetDevice(dev.address)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConnect(targetDevice) },
                    enabled = !connectionState.isConnected && !connectionState.isConnecting,
                    colors = ButtonDefaults.buttonColors(containerColor = SonyCopper)
                ) {
                    if (connectionState.isConnecting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect")
                    }
                }

                OutlinedButton(
                    onClick = onDisconnect,
                    enabled = connectionState.isConnected || connectionState.isConnecting
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
fun StatusPill(connectionState: ConnectionState) {
    val (bgColor, textColor, label) = when (connectionState) {
        is ConnectionState.Connected -> Triple(AncGreen.copy(alpha = 0.2f), AncGreen, "Connected")
        is ConnectionState.Connecting -> Triple(SonyGold.copy(alpha = 0.2f), SonyGold, "Connecting")
        is ConnectionState.Error -> Triple(Color.Red.copy(alpha = 0.2f), Color.Red, "Error")
        is ConnectionState.Disconnected -> Triple(OffGray.copy(alpha = 0.2f), OffGray, "Disconnected")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NoiseControlCard(
    currentMode: NoiseControlMode,
    isConnected: Boolean,
    onSelectMode: (NoiseControlMode) -> Unit,
    onCycleMode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Noise Control Modes",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Sony MDR SPP",
                    fontSize = 11.sp,
                    color = SonyGold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Current Mode Indicator
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconRes = when (currentMode) {
                        NoiseControlMode.NOISE_CANCELLING -> R.drawable.ic_tile_nc
                        NoiseControlMode.AMBIENT_SOUND -> R.drawable.ic_tile_ambient
                        NoiseControlMode.OFF -> R.drawable.ic_tile_off
                    }
                    val accentColor = when (currentMode) {
                        NoiseControlMode.NOISE_CANCELLING -> AncGreen
                        NoiseControlMode.AMBIENT_SOUND -> AmbientBlue
                        NoiseControlMode.OFF -> OffGray
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = currentMode.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = accentColor
                        )
                        Text(
                            text = currentMode.tileSubtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Mode Selection Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeButton(
                    title = "NC On",
                    isSelected = currentMode == NoiseControlMode.NOISE_CANCELLING,
                    selectedColor = AncGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(NoiseControlMode.NOISE_CANCELLING) }
                )
                ModeButton(
                    title = "Ambient",
                    isSelected = currentMode == NoiseControlMode.AMBIENT_SOUND,
                    selectedColor = AmbientBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(NoiseControlMode.AMBIENT_SOUND) }
                )
                ModeButton(
                    title = "Off",
                    isSelected = currentMode == NoiseControlMode.OFF,
                    selectedColor = OffGray,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(NoiseControlMode.OFF) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Settings Simulation Button
            Button(
                onClick = onCycleMode,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate Quick Settings Tile Tap (Cycle Mode)")
            }
        }
    }
}

@Composable
fun ModeButton(
    title: String,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = selectedColor)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text = title, fontSize = 13.sp)
        }
    }
}

@Composable
fun ColorOSOptimizationCard(
    foregroundServiceEnabled: Boolean,
    onToggleForegroundService: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isOppoDevice = remember { ColorOSHelper.isColorOSDevice() }
    val isIgnored = remember { ColorOSHelper.isBatteryOptimizationIgnored(context) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = SonyGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ColorOS / Oppo Find X9 Ultra Guardrails",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ColorOS aggressively freezes background Bluetooth sockets. To prevent Quick Settings Tile freezes, configure these three system guardrails:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 1: Battery optimization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1. Battery Optimization: Don't Optimize",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isIgnored) "Status: Unrestricted (Configured)" else "Status: Optimized (May freeze tile)",
                        fontSize = 11.sp,
                        color = if (isIgnored) AncGreen else Color.Red
                    )
                }
                OutlinedButton(onClick = { ColorOSHelper.requestIgnoreBatteryOptimization(context) }) {
                    Text("Configure")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step 2: Auto-launch / Background activity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2. ColorOS Auto-launch / Startup",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Enable 'Allow background activity' in Security Center",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                OutlinedButton(onClick = { ColorOSHelper.openColorOSAutoLaunchSettings(context) }) {
                    Text("Open")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step 3: Persistent Background Service Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3. Keep Persistent Link Active",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Instant tile response (<50ms) + live headset button sync",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = foregroundServiceEnabled,
                    onCheckedChange = onToggleForegroundService
                )
            }
        }
    }
}

@Composable
fun DiagnosticsCard(logs: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = SonyGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MDR Protocol Live Console",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "${logs.size} frames",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                color = Color(0xFF0A0A0C),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Awaiting Bluetooth RFCOMM session...",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        reverseLayout = true
                    ) {
                        items(logs.reversed()) { logLine ->
                            Text(
                                text = logLine,
                                color = if (logLine.contains("error", true)) Color.Red else Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
