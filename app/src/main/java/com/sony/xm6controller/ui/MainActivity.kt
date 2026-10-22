package com.sony.xm6controller.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sony.xm6controller.R
import com.sony.xm6controller.data.bluetooth.ConnectionState
import com.sony.xm6controller.domain.BatteryState
import com.sony.xm6controller.domain.DeviceType
import com.sony.xm6controller.domain.EqPreset
import com.sony.xm6controller.domain.EqualizerState
import com.sony.xm6controller.domain.HeadphoneRepository
import com.sony.xm6controller.domain.NoiseControlMode
import com.sony.xm6controller.domain.NoiseControlState
import com.sony.xm6controller.domain.PeqParser
import com.sony.xm6controller.domain.PeqProfile
import com.sony.xm6controller.domain.SoundFeaturesState
import com.sony.xm6controller.domain.TEN_BAND_FREQUENCIES
import com.sony.xm6controller.domain.TEN_BAND_LABELS
import com.sony.xm6controller.domain.TenBandEqState
import com.sony.xm6controller.domain.TileCycleScheme
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
    val batteryState by repository.batteryState.collectAsState()
    val equalizerState by repository.equalizerState.collectAsState()
    val tenBandEqState by repository.tenBandEqState.collectAsState()
    val soundFeaturesState by repository.soundFeaturesState.collectAsState()
    val tileCycleScheme by repository.tileCycleScheme.collectAsState()
    val targetDevice by repository.targetDevice.collectAsState()
    val diagnosticLogs by repository.diagnosticLogs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var foregroundServiceEnabled by remember { mutableStateOf(false) }
    var connectionErrorDialog by remember { mutableStateOf<String?>(null) }
    var pastePeqDialogVisible by remember { mutableStateOf(false) }

    // File picker launcher for .peqdb, .json, and .txt files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (!content.isNullOrEmpty()) {
                    val profile = PeqParser.parse(content, fallbackName = "Imported PEQ")
                    if (profile != null) {
                        scope.launch {
                            repository.importAndApplyPeqProfile(profile)
                            Toast.makeText(context, "Loaded PEQ: ${profile.name}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Could not parse PEQ file structure", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isSongpalInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.sony.songpal.mdr", 0)
            true
        } catch (_: Exception) {
            try {
                context.packageManager.getPackageInfo("jp.co.sony.songpal.mdr", 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

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
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                @SuppressLint("MissingPermission")
                                val devTitle = targetDevice?.name ?: "Sony Audio Tiles"
                                Text(
                                    text = devTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Universal Sound Connect & PEQ Hub",
                                    fontSize = 11.sp,
                                    color = SonyCopper
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // 4 Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = SonyCopper,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SonyCopper
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Controls & ANC", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("10-Band EQ & PEQ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Sound Features", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("System", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
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

            // Permission Card (if missing)
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

            // Always-visible Header Card: Device Status Dashboard with Animated Waveform
            item {
                DeviceStatusCard(
                    connectionState = connectionState,
                    targetDevice = targetDevice,
                    batteryState = batteryState,
                    repository = repository,
                    onConnect = { dev ->
                        scope.launch {
                            val result = repository.connect(dev)
                            if (result.isFailure) {
                                val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                                if (msg.contains("read ret: -1") || msg.contains("closed") || msg.contains("timeout") || msg.contains("Failed to establish")) {
                                    connectionErrorDialog = msg
                                } else {
                                    Toast.makeText(context, "Connect failed: $msg", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    onDisconnect = { scope.launch { repository.disconnect() } },
                    onRefreshBattery = { scope.launch { repository.queryBattery() } }
                )
            }

            // Notice card for official Sony Sound Connect conflict if installed
            if (isSongpalInstalled && !connectionState.isConnected) {
                item {
                    SonySoundConnectNoticeCard(
                        onOpenAppSettings = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:com.sony.songpal.mdr")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:jp.co.sony.songpal.mdr")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
            }

            // TAB 0: Controls & ANC
            if (selectedTab == 0) {
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

                item {
                    AmbientSoundControlsCard(
                        noiseControlState = noiseControlState,
                        isConnected = connectionState.isConnected,
                        onLevelChange = { level -> scope.launch { repository.setAmbientLevel(level) } },
                        onVoiceFocusToggle = { enabled -> scope.launch { repository.setFocusOnVoice(enabled) } }
                    )
                }

                item {
                    WindNoiseReductionCard(
                        windNoiseReduction = noiseControlState.windNoiseReduction,
                        isConnected = connectionState.isConnected,
                        onToggle = { enabled -> scope.launch { repository.setWindNoiseReduction(enabled) } }
                    )
                }
            }

            // TAB 1: 10-Band Graphic Equalizer & PEQDB Import
            if (selectedTab == 1) {
                item {
                    TenBandEqualizerCard(
                        tenBandState = tenBandEqState,
                        equalizerState = equalizerState,
                        isConnected = connectionState.isConnected,
                        onUpdateBand = { index, gainDb ->
                            val next = tenBandEqState.withBand(index, gainDb)
                            scope.launch { repository.applyTenBandEq(next) }
                        },
                        onResetFlat = {
                            val flat = TenBandEqState(profileName = "Flat (0 dB)")
                            scope.launch { repository.applyTenBandEq(flat) }
                        },
                        onImportFile = {
                            filePickerLauncher.launch("*/*")
                        },
                        onPastePeq = {
                            pastePeqDialogVisible = true
                        },
                        onSelectTargetProfile = { profile ->
                            scope.launch {
                                repository.importAndApplyPeqProfile(profile)
                                Toast.makeText(context, "Applied Target: ${profile.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item {
                    HardwareSonyEqCard(
                        equalizerState = equalizerState,
                        isConnected = connectionState.isConnected,
                        onSelectPreset = { preset ->
                            scope.launch { repository.setEqualizerPreset(preset) }
                        },
                        onUpdateClearBass = { cb ->
                            scope.launch {
                                repository.setEqualizerBands(
                                    preset = EqPreset.CUSTOM_1,
                                    clearBass = cb,
                                    bands = equalizerState.bands
                                )
                            }
                        }
                    )
                }
            }

            // TAB 2: Sound Features (Speak-to-Chat, DSEE, Wearing Pause)
            if (selectedTab == 2) {
                item {
                    SoundFeaturesCard(
                        soundFeatures = soundFeaturesState,
                        isConnected = connectionState.isConnected,
                        onToggleSpeakToChat = { enabled ->
                            scope.launch {
                                repository.setSpeakToChat(
                                    enabled = enabled,
                                    sensitivity = soundFeaturesState.speakToChat.sensitivity,
                                    timeout = soundFeaturesState.speakToChat.timeout
                                )
                            }
                        },
                        onToggleDsee = { enabled -> scope.launch { repository.setDsee(enabled) } },
                        onToggleWearingPause = { enabled -> scope.launch { repository.setWearingPause(enabled) } },
                        onSelectAutoPowerOff = { index -> scope.launch { repository.setAutoPowerOff(index) } },
                        onToggleTouchSensor = { enabled -> scope.launch { repository.setTouchSensor(enabled) } },
                        onToggleSoundQuality = { quality -> scope.launch { repository.setSoundQualityPriority(quality) } }
                    )
                }
            }

            // TAB 3: System, ColorOS & Diagnostics
            if (selectedTab == 3) {
                item {
                    TileCustomizationCard(
                        activeScheme = tileCycleScheme,
                        onSelectScheme = { scheme -> repository.setTileCycleScheme(scheme) }
                    )
                }

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

                item {
                    DiagnosticsCard(logs = diagnosticLogs)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Paste PEQ Text Modal Dialog
        if (pastePeqDialogVisible) {
            var rawText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { pastePeqDialogVisible = false },
                title = { Text("Paste PEQ / AutoEq Curve", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column {
                        Text(
                            "Paste parametric filter text from AutoEq, EqualizerAPO, Peace, or Squiglink (e.g. 'Filter 1: ON PK Fc 105 Hz Gain -3.2 dB Q 1.41'):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            placeholder = { Text("Preamp: -4.0 dB\nFilter 1: ON PK Fc 105 Hz Gain -3.2 dB Q 1.41\nFilter 2: ON LSC Fc 80 Hz Gain 4.5 dB Q 0.71...", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val profile = PeqParser.parse(rawText, fallbackName = "Pasted Curve")
                            if (profile != null) {
                                scope.launch {
                                    repository.importAndApplyPeqProfile(profile)
                                    Toast.makeText(context, "Applied curve: ${profile.name}", Toast.LENGTH_SHORT).show()
                                }
                                pastePeqDialogVisible = false
                            } else {
                                Toast.makeText(context, "Could not parse filters from text", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SonyCopper)
                    ) {
                        Text("Apply to Headphone")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pastePeqDialogVisible = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Connection Error Dialog
        if (connectionErrorDialog != null) {
            AlertDialog(
                onDismissRequest = { connectionErrorDialog = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connection Rejected", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Headphone rejected or closed the RFCOMM socket.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sony headphones only permit ONE active app connection at a time.\n\n" +
                                    "1. Force stop Sony Sound Connect / Headphones Connect if running.\n" +
                                    "2. Ensure device is connected for phone audio in Bluetooth settings.\n" +
                                    "3. Tap 'Try Again'.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val dev = targetDevice
                            connectionErrorDialog = null
                            scope.launch {
                                val res = repository.connect(dev)
                                if (res.isFailure) {
                                    connectionErrorDialog = res.exceptionOrNull()?.message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SonyCopper)
                    ) {
                        Text("Try Again")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { connectionErrorDialog = null }) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 10-BAND EQUALIZER & PEQDB IMPORT COMPOSABLE WITH INTERACTIVE SPLINE CANVAS
// -------------------------------------------------------------------------------------------------

@Composable
fun TenBandEqualizerCard(
    tenBandState: TenBandEqState,
    equalizerState: EqualizerState,
    isConnected: Boolean,
    onUpdateBand: (Int, Float) -> Unit,
    onResetFlat: () -> Unit,
    onImportFile: () -> Unit,
    onPastePeq: () -> Unit,
    onSelectTargetProfile: (PeqProfile) -> Unit
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
                Column {
                    Text(
                        text = "10-Band Graphic & Parametric EQ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Curve: ${tenBandState.profileName}",
                        fontSize = 12.sp,
                        color = SonyGold
                    )
                }
                Row {
                    OutlinedButton(
                        onClick = onImportFile,
                        enabled = isConnected,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import PEQ", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Live Frequency Response Curve Canvas
            FrequencyResponseSplineCanvas(
                bands = tenBandState.bands,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF07090E))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Audiophile Target Profile Quick Chips
            Text("Audiophile Target Curves:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SonyCopper)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PeqParser.TARGET_PROFILES.forEach { target ->
                    val isSel = (tenBandState.profileName == target.name)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) SonyCopper else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) SonyGold else CardBorder),
                        modifier = Modifier.clickable(enabled = isConnected) { onSelectTargetProfile(target) }
                    ) {
                        Text(
                            text = target.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPastePeq, enabled = isConnected) {
                    Text("Paste AutoEq Text", fontSize = 11.sp, color = SonyCopper)
                }
                TextButton(onClick = onResetFlat, enabled = isConnected) {
                    Text("Reset to Flat (0 dB)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 10 Compact Sliders Grid
            Text("Octave Band Sliders (-12 dB to +12 dB):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            TEN_BAND_FREQUENCIES.forEachIndexed { idx, freqHz ->
                val gain = tenBandState.bands.getOrElse(idx) { 0.0f }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${TEN_BAND_LABELS[idx]} Hz",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(52.dp)
                    )
                    Slider(
                        value = gain,
                        onValueChange = { onUpdateBand(idx, it) },
                        valueRange = -12f..12f,
                        steps = 23,
                        enabled = isConnected,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = SonyCopper,
                            activeTrackColor = SonyCopper,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    Text(
                        text = "${if (gain > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", gain)} dB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (gain > 0) SonyGold else if (gain < 0) AmbientBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.width(58.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

/**
 * Live Spline Frequency Response Visualizer Canvas.
 */
@Composable
fun FrequencyResponseSplineCanvas(
    bands: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Draw dB Grid lines (-10dB, 0dB, +10dB)
        val gridYp10 = midY - (height * 0.4f)
        val gridYm10 = midY + (height * 0.4f)

        drawLine(color = Color(0xFF1E2433), start = Offset(0f, gridYp10), end = Offset(width, gridYp10), strokeWidth = 1f)
        drawLine(color = Color(0xFF333E55), start = Offset(0f, midY), end = Offset(width, midY), strokeWidth = 1.5f)
        drawLine(color = Color(0xFF1E2433), start = Offset(0f, gridYm10), end = Offset(width, gridYm10), strokeWidth = 1f)

        // Interpolate points across the 10 frequency centers
        val pointCount = bands.size
        val stepX = width / (pointCount + 1)
        val points = mutableListOf<Offset>()

        for (i in 0 until pointCount) {
            val gain = bands[i].coerceIn(-12f, 12f)
            val px = stepX * (i + 1)
            val py = midY - (gain / 12f) * (height * 0.45f)
            points.add(Offset(px, py))
        }

        if (points.size >= 2) {
            val path = Path()
            val fillPath = Path()

            path.moveTo(0f, midY)
            fillPath.moveTo(0f, midY)

            // Connect using cubic bezier curves
            for (i in 0 until points.size) {
                val pt = points[i]
                if (i == 0) {
                    val cx1 = pt.x * 0.5f
                    path.cubicTo(cx1, midY, cx1, pt.y, pt.x, pt.y)
                    fillPath.cubicTo(cx1, midY, cx1, pt.y, pt.x, pt.y)
                } else {
                    val prev = points[i - 1]
                    val cx1 = (prev.x + pt.x) / 2f
                    path.cubicTo(cx1, prev.y, cx1, pt.y, pt.x, pt.y)
                    fillPath.cubicTo(cx1, prev.y, cx1, pt.y, pt.x, pt.y)
                }
            }

            val last = points.last()
            path.cubicTo((last.x + width) / 2f, last.y, (last.x + width) / 2f, midY, width, midY)
            fillPath.cubicTo((last.x + width) / 2f, last.y, (last.x + width) / 2f, midY, width, midY)

            // Close fill path
            fillPath.lineTo(width, height)
            fillPath.lineTo(0f, height)
            fillPath.close()

            // Draw glowing ambient fill under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(SonyCopper.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw primary curve stroke
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(SonyGold, SonyCopper, AncGreen)
                ),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            // Draw control nodes
            for (pt in points) {
                drawCircle(color = Color.Black, radius = 5.5f, center = pt)
                drawCircle(color = SonyGold, radius = 3.5f, center = pt)
            }
        }
    }
}

/**
 * On-Device Hardware Sony 5-Band + Clear Bass Direct DSP Tuning Card.
 */
@Composable
fun HardwareSonyEqCard(
    equalizerState: EqualizerState,
    isConnected: Boolean,
    onSelectPreset: (EqPreset) -> Unit,
    onUpdateClearBass: (Int) -> Unit
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
                Column {
                    Text("Sony On-Headset DSP Hardware Controls", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Direct hardware register write (Opcode 0x58)", fontSize = 11.sp, color = SonyCopper)
                }
                Text("Clear Bass: ${if (equalizerState.clearBass > 0) "+" else ""}${equalizerState.clearBass}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SonyGold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Clear Bass Slider
            Slider(
                value = equalizerState.clearBass.toFloat(),
                onValueChange = { onUpdateClearBass(it.toInt()) },
                valueRange = -10f..10f,
                steps = 19,
                enabled = isConnected,
                colors = SliderDefaults.colors(thumbColor = SonyCopper, activeTrackColor = SonyCopper)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preset Quick Row
            Text("Hardware Presets:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EqPreset.entries.forEach { p ->
                    val isSel = (p == equalizerState.preset)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) SonyCopper else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) SonyGold else CardBorder),
                        modifier = Modifier.clickable(enabled = isConnected) { onSelectPreset(p) }
                    ) {
                        Text(
                            text = p.displayName,
                            color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// DEVICE CONNECTION CARD WITH PULSING WAVEFORM ANIMATION
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceStatusCard(
    connectionState: ConnectionState,
    targetDevice: BluetoothDevice?,
    batteryState: BatteryState,
    repository: HeadphoneRepository,
    onConnect: (BluetoothDevice?) -> Unit,
    onDisconnect: () -> Unit,
    onRefreshBattery: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val pairedDevices = remember { repository.getPairedDevices() }

    @SuppressLint("MissingPermission")
    val currentName = targetDevice?.name ?: "Sony Device (Searching...)"
    @SuppressLint("MissingPermission")
    val currentAddress = targetDevice?.address ?: "No Paired Device"
    val deviceType = remember(targetDevice?.name) { DeviceType.fromName(targetDevice?.name) }

    // Pulsing audio animation when connected
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

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
                    Column {
                        Text(
                            text = "Audio Device",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = deviceType.displayName,
                            fontSize = 11.sp,
                            color = SonyCopper
                        )
                    }
                }
                StatusPill(connectionState = connectionState)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
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
                }

                // Pulsing vital wave indicator if connected
                if (connectionState.isConnected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(3.dp, (12 * pulseAlpha).dp).background(AncGreen, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.size(3.dp, (20 * (1.4f - pulseAlpha)).dp).background(SonyCopper, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.size(3.dp, (16 * pulseAlpha).dp).background(SonyGold, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.size(3.dp, (8 * (1.2f - pulseAlpha)).dp).background(AncGreen, RoundedCornerShape(2.dp)))
                    }
                }
            }

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
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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

            // Universal Battery Telemetry (Over-Ear & TWS)
            if (connectionState.isConnected) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val battText = if (batteryState.isAvailable) {
                            "Battery: ${batteryState.displayString}"
                        } else {
                            "Battery: Querying telemetry..."
                        }
                        Text(
                            text = battText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onRefreshBattery,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Battery", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SOUND CONNECT HARDWARE FEATURES COMPOSABLE
// -------------------------------------------------------------------------------------------------

@Composable
fun SoundFeaturesCard(
    soundFeatures: SoundFeaturesState,
    isConnected: Boolean,
    onToggleSpeakToChat: (Boolean) -> Unit,
    onToggleDsee: (Boolean) -> Unit,
    onToggleWearingPause: (Boolean) -> Unit,
    onSelectAutoPowerOff: (Int) -> Unit,
    onToggleTouchSensor: (Boolean) -> Unit,
    onToggleSoundQuality: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sound Connect Hardware Features",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Speak-to-Chat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Speak-to-Chat", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("Auto ambient sound passthrough when user speaks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Switch(
                    checked = soundFeatures.speakToChat.enabled,
                    onCheckedChange = onToggleSpeakToChat,
                    enabled = isConnected
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. DSEE Extreme
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DSEE Extreme (AI Upscaling)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("Restores high frequencies in compressed Bluetooth audio", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Switch(
                    checked = soundFeatures.dseeExtreme,
                    onCheckedChange = onToggleDsee,
                    enabled = isConnected
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Pause when taken off
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pause on Removal", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("Pauses playback via optical/capacitive wearing sensor", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Switch(
                    checked = soundFeatures.wearingPause,
                    onCheckedChange = onToggleWearingPause,
                    enabled = isConnected
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Touch Sensor Control Panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Touch Sensor Control Panel", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("Enables tap/swipe volume and media gestures on earcup", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Switch(
                    checked = soundFeatures.touchSensorEnabled,
                    onCheckedChange = onToggleTouchSensor,
                    enabled = isConnected
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Sound Quality Priority Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Priority on Sound Quality (LDAC)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("High-resolution 990kbps LDAC audio transmission", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Switch(
                    checked = soundFeatures.soundQualityPriority,
                    onCheckedChange = onToggleSoundQuality,
                    enabled = isConnected
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Auto Power-Off Timer
            Text("Auto Power-Off Timer (When not worn)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val autoOffLabels = listOf("Do not turn off", "5 min", "30 min", "60 min", "180 min")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                autoOffLabels.forEachIndexed { i, label ->
                    val isSel = (soundFeatures.autoPowerOffIndex == i)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) SonyCopper else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) SonyGold else CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = isConnected) { onSelectAutoPowerOff(i) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// NOISE CONTROL, AMBIENT & SYSTEM CARDS
// -------------------------------------------------------------------------------------------------

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
                    color = SonyCopper
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Active Mode Indicator Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = when (currentMode) {
                    NoiseControlMode.NOISE_CANCELLING -> AncGreen.copy(alpha = 0.15f)
                    NoiseControlMode.AMBIENT_SOUND -> AmbientBlue.copy(alpha = 0.15f)
                    NoiseControlMode.OFF -> OffGray.copy(alpha = 0.15f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    when (currentMode) {
                        NoiseControlMode.NOISE_CANCELLING -> AncGreen
                        NoiseControlMode.AMBIENT_SOUND -> AmbientBlue
                        NoiseControlMode.OFF -> OffGray
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            id = when (currentMode) {
                                NoiseControlMode.NOISE_CANCELLING -> R.drawable.ic_tile_nc
                                NoiseControlMode.AMBIENT_SOUND -> R.drawable.ic_tile_ambient
                                NoiseControlMode.OFF -> R.drawable.ic_tile_off
                            }
                        ),
                        contentDescription = null,
                        tint = when (currentMode) {
                            NoiseControlMode.NOISE_CANCELLING -> AncGreen
                            NoiseControlMode.AMBIENT_SOUND -> AmbientBlue
                            NoiseControlMode.OFF -> OffGray
                        },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = currentMode.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
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

            // Mode Selector Buttons (NC On, Ambient, Off)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoiseControlButton(
                    label = "NC On",
                    isSelected = currentMode == NoiseControlMode.NOISE_CANCELLING,
                    selectedColor = AncGreen,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(NoiseControlMode.NOISE_CANCELLING) }
                )
                NoiseControlButton(
                    label = "Ambient",
                    isSelected = currentMode == NoiseControlMode.AMBIENT_SOUND,
                    selectedColor = AmbientBlue,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(NoiseControlMode.AMBIENT_SOUND) }
                )
                NoiseControlButton(
                    label = "Off",
                    isSelected = currentMode == NoiseControlMode.OFF,
                    selectedColor = OffGray,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(NoiseControlMode.OFF) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cycle Button
            Button(
                onClick = onCycleMode,
                enabled = isConnected,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simulate Quick Settings Tile Tap (Cycle Mode)",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun NoiseControlButton(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) selectedColor else CardBorder
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AmbientSoundControlsCard(
    noiseControlState: NoiseControlState,
    isConnected: Boolean,
    onLevelChange: (Int) -> Unit,
    onVoiceFocusToggle: (Boolean) -> Unit
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
                    text = "Ambient Sound Tuning",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Level ${noiseControlState.ambientLevel} / 20",
                    fontWeight = FontWeight.Bold,
                    color = SonyCopper,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Adjust external passthrough volume when in Ambient Sound mode.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = noiseControlState.ambientLevel.toFloat(),
                onValueChange = { onLevelChange(it.toInt()) },
                valueRange = 1f..20f,
                steps = 18,
                enabled = isConnected,
                colors = SliderDefaults.colors(
                    thumbColor = SonyCopper,
                    activeTrackColor = SonyCopper,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Min (1)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("Balanced (10)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("Max Awareness (20)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Focus on Voice Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Focus on Voice",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Suppresses background hum while letting vocal frequencies through",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = noiseControlState.focusOnVoice,
                    onCheckedChange = onVoiceFocusToggle,
                    enabled = isConnected
                )
            }
        }
    }
}

@Composable
fun WindNoiseReductionCard(
    windNoiseReduction: Boolean,
    isConnected: Boolean,
    onToggle: (Boolean) -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatic Wind Noise Reduction",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Activates physical microphone baffling during outdoor breezes",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = windNoiseReduction,
                    onCheckedChange = onToggle,
                    enabled = isConnected
                )
            }
        }
    }
}

@Composable
fun TileCustomizationCard(
    activeScheme: TileCycleScheme,
    onSelectScheme: (TileCycleScheme) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Settings Tile Behavior",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose which modes the notification tile cycles through on each tap:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            TileCycleScheme.entries.forEach { scheme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectScheme(scheme) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (activeScheme == scheme),
                        onClick = { onSelectScheme(scheme) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = scheme.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(text = scheme.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOSOptimizationCard(
    foregroundServiceEnabled: Boolean,
    onToggleForegroundService: (Boolean) -> Unit
) {
    val context = LocalContext.current

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
                    text = "ColorOS / Oppo Battery Guardrails",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "To guarantee the Quick Settings Tile responds with zero latency when your screen is locked or idle, configure these background guardrails:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("1. Battery Optimization: Don't Optimize", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("Settings > Battery > Background Usage Limits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Configure", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("2. Persistent Connection Service", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("Keeps RFCOMM socket warm for sub-50ms instant switching", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
fun StatusPill(connectionState: ConnectionState) {
    val (bgColor, textColor, label) = when (connectionState) {
        is ConnectionState.Connected -> Triple(Color(0xFF1E3A2F), AncGreen, "Connected")
        is ConnectionState.Connecting -> Triple(Color(0xFF332B1A), SonyGold, "Connecting")
        is ConnectionState.Disconnected -> Triple(Color(0xFF2B2B2B), OffGray, "Disconnected")
        is ConnectionState.Error -> Triple(Color(0xFF3B1F1F), Color(0xFFFF6B6B), "Error")
    }

    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SonySoundConnectNoticeCard(onOpenAppSettings: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SonyCopper.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = SonyCopper, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sony Sound Connect Detected", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sony headphones only allow one active app on their control channel. If connection times out, please force stop Sony Sound Connect.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.height(36.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("App Info / Force Stop", fontSize = 12.sp)
            }
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
                Text("Bluetooth Permissions Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Android 14+ requires Nearby Devices permission (BLUETOOTH_CONNECT) to communicate with your Sony headphones over RFCOMM SPP.",
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
