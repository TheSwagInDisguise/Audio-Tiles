package com.sony.xm6controller.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import kotlin.math.roundToInt
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sony.xm6controller.R
import com.sony.xm6controller.data.bluetooth.ConnectionState
import com.sony.xm6controller.data.bluetooth.LogDirection
import com.sony.xm6controller.data.bluetooth.MdrDiagnosticLogEntry
import com.sony.xm6controller.domain.AcousticSealTester
import com.sony.xm6controller.domain.AutoEqCatalogEntry
import com.sony.xm6controller.domain.BatteryState
import com.sony.xm6controller.domain.DeviceCapabilities
import com.sony.xm6controller.domain.DeviceFamily
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
import com.sony.xm6controller.domain.ThemeSkin
import com.sony.xm6controller.domain.TileCycleScheme
import com.sony.xm6controller.domain.UserEqPreset
import com.sony.xm6controller.service.XM6ForegroundService
import com.sony.xm6controller.ui.theme.AmbientBlue
import com.sony.xm6controller.ui.theme.AncGreen
import com.sony.xm6controller.ui.theme.OffGray
import com.sony.xm6controller.ui.theme.XM6ControllerTheme
import kotlinx.coroutines.launch

private val CardBorder: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline

private val SonyCopper: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

private val SonyGold: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

class MainActivity : ComponentActivity() {

    private lateinit var repository: HeadphoneRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = HeadphoneRepository.getInstance(applicationContext)

        setContent {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val skinName = prefs.getString("theme_skin", ThemeSkin.BLACK_COPPER.name)
            var currentSkin by remember {
                mutableStateOf(ThemeSkin.entries.find { it.name == skinName } ?: ThemeSkin.BLACK_COPPER)
            }

            XM6ControllerTheme(themeSkin = currentSkin) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        repository = repository,
                        currentSkin = currentSkin,
                        onSkinChanged = { skin ->
                            currentSkin = skin
                            prefs.edit().putString("theme_skin", skin.name).apply()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: HeadphoneRepository,
    currentSkin: ThemeSkin,
    onSkinChanged: (ThemeSkin) -> Unit
) {
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
    val deviceCapabilities by repository.deviceCapabilities.collectAsState()
    val isEqBypassed by repository.isEqBypassed.collectAsState()
    val userPresets by repository.userPresets.collectAsState()
    val diagnosticLogs by repository.diagnosticLogs.collectAsState()
    val diagnosticEntries by repository.diagnosticEntries.collectAsState()

    val iso226Enabled by repository.dynamicBassManager.isEnabled.collectAsState()
    val iso226Offset by repository.dynamicBassManager.currentCompensationOffset.collectAsState()

    var showEqSection by remember { mutableStateOf(false) }
    var showFeaturesSection by remember { mutableStateOf(false) }
    var showSmartAudioSection by remember { mutableStateOf(false) }
    var showDiagnosticsSection by remember { mutableStateOf(false) }
    var settingsDialogVisible by remember { mutableStateOf(false) }

    var foregroundServiceEnabled by remember { mutableStateOf(false) }
    var connectionErrorDialog by remember { mutableStateOf<String?>(null) }
    var pastePeqDialogVisible by remember { mutableStateOf(false) }
    var autoEqCatalogVisible by remember { mutableStateOf(false) }
    var savePresetDialogVisible by remember { mutableStateOf(false) }
    var acousticSealDialogVisible by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

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
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sony Quick Settings & Sound Hub",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { settingsDialogVisible = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

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

            // Always-visible Header Card: Device Status Dashboard with Animated Waveform & Battery Rings
            item {
                DeviceStatusCard(
                    connectionState = connectionState,
                    targetDevice = targetDevice,
                    batteryState = batteryState,
                    deviceCapabilities = deviceCapabilities,
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

            // Primary Noise Control & Clear Bass Card (Core Daily Controls)
            item {
                NoiseControlCard(
                    currentMode = noiseControlState.mode,
                    ambientLevel = noiseControlState.ambientLevel,
                    focusOnVoice = noiseControlState.focusOnVoice,
                    clearBass = equalizerState.clearBass,
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
                    },
                    onLevelChange = { level -> scope.launch { repository.setAmbientLevel(level) } },
                    onVoiceFocusToggle = { enabled -> scope.launch { repository.setFocusOnVoice(enabled) } },
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

            // 🔽 POWER USER ACCORDION 1: 10-Band Graphic Equalizer & AutoEq Studio
            item {
                ExpandableCard(
                    title = "10-Band EQ & AutoEq Studio",
                    subtitle = "Touch-draggable spline, 26 AutoEq profiles & A/B bypass",
                    icon = Icons.Default.Tune,
                    isExpanded = showEqSection,
                    onToggle = { showEqSection = !showEqSection }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        TenBandEqualizerCard(
                            tenBandState = tenBandEqState,
                            equalizerState = equalizerState,
                            isConnected = connectionState.isConnected,
                            isEqBypassed = isEqBypassed,
                            userPresets = userPresets,
                            onToggleBypass = {
                                repository.toggleEqBypass()
                                Toast.makeText(
                                    context,
                                    if (!isEqBypassed) "A/B Audition: EQ Bypassed (Flat 0dB sent)" else "A/B Audition: EQ Curve Restored",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onSaveCurrentPreset = { savePresetDialogVisible = true },
                            onDeleteUserPreset = { presetId ->
                                repository.deleteUserPreset(presetId)
                                Toast.makeText(context, "Preset removed", Toast.LENGTH_SHORT).show()
                            },
                            onSelectUserPreset = { preset ->
                                scope.launch {
                                    repository.applyTenBandEq(preset.toTenBandEqState().copy(profileName = preset.name))
                                    Toast.makeText(context, "Applied preset: ${preset.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenAutoEqCatalog = { autoEqCatalogVisible = true },
                            onUpdateBand = { index, gainDb ->
                                val next = tenBandEqState.withBand(index, gainDb)
                                scope.launch { repository.applyTenBandEq(next) }
                            },
                            onUpdateClearBass = { cb ->
                                val next = tenBandEqState.withClearBass(cb)
                                scope.launch { repository.applyTenBandEq(next) }
                            },
                            onResetFlat = {
                                val flat = TenBandEqState(profileName = "Flat (0 dB)")
                                scope.launch { repository.applyTenBandEq(flat) }
                            },
                            onImportFile = { filePickerLauncher.launch("*/*") },
                            onPastePeq = { pastePeqDialogVisible = true },
                            onSelectTargetProfile = { profile ->
                                scope.launch {
                                    repository.importAndApplyPeqProfile(profile)
                                    Toast.makeText(context, "Applied Target: ${profile.name}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

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
            }

            // 🔽 POWER USER ACCORDION 2: Sound Connect Hardware Features
            item {
                ExpandableCard(
                    title = "Headphone Hardware Features",
                    subtitle = "Speak-to-Chat, DSEE, wear detection & LDAC",
                    icon = Icons.Default.Layers,
                    isExpanded = showFeaturesSection,
                    onToggle = { showFeaturesSection = !showFeaturesSection }
                ) {
                    SoundFeaturesCard(
                        soundFeatures = soundFeaturesState,
                        deviceCapabilities = deviceCapabilities,
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

            // 🔽 POWER USER ACCORDION 3: Smart Audio & Automations
            item {
                ExpandableCard(
                    title = "Smart Audio & Dynamic Bass",
                    subtitle = "ISO 226 equal-loudness bass & seal test",
                    icon = Icons.Default.GraphicEq,
                    isExpanded = showSmartAudioSection,
                    onToggle = { showSmartAudioSection = !showSmartAudioSection }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DynamicBassCard(
                            isEnabled = iso226Enabled,
                            activeCompensationOffset = iso226Offset,
                            onToggle = { enabled -> repository.dynamicBassManager.setEnabled(enabled) }
                        )

                        AcousticSealCard(
                            onOpenTest = { acousticSealDialogVisible = true }
                        )
                    }
                }
            }

            // 🔽 POWER USER ACCORDION 4: MDR Protocol & Live Diagnostics
            item {
                ExpandableCard(
                    title = "MDR Protocol & Live Diagnostics",
                    subtitle = "Real-time RFCOMM packet monitor & report",
                    icon = Icons.Default.BugReport,
                    isExpanded = showDiagnosticsSection,
                    onToggle = { showDiagnosticsSection = !showDiagnosticsSection }
                ) {
                    DiagnosticsCard(
                        diagnosticEntries = diagnosticEntries,
                        onCopyReport = {
                            val report = repository.exportDiagnosticReport()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Sony Audio Tiles Diagnostic Report", report))
                            Toast.makeText(context, "Diagnostic report copied to clipboard!", Toast.LENGTH_LONG).show()
                        },
                        onClearLogs = {
                            repository.clearDiagnosticLogs()
                            Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Settings Modal Dialog (Theme, Tile Scheme, ColorOS)
        if (settingsDialogVisible) {
            SettingsDialog(
                currentSkin = currentSkin,
                onSelectSkin = onSkinChanged,
                tileCycleScheme = tileCycleScheme,
                onSelectScheme = { scheme -> repository.setTileCycleScheme(scheme) },
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
                },
                onDismissRequest = { settingsDialogVisible = false }
            )
        }

        // AutoEq Catalog Search Modal Dialog
        if (autoEqCatalogVisible) {
            AutoEqCatalogDialog(
                catalogEntries = repository.autoEqCatalog,
                connectedModel = deviceCapabilities.modelName,
                onDismiss = { autoEqCatalogVisible = false },
                onApplyProfile = { profile ->
                    scope.launch {
                        repository.importAndApplyPeqProfile(profile)
                        Toast.makeText(context, "Applied AutoEq: ${profile.name}", Toast.LENGTH_SHORT).show()
                    }
                    autoEqCatalogVisible = false
                }
            )
        }

        // Acoustic Seal Fit Test Modal Dialog
        if (acousticSealDialogVisible) {
            AcousticSealDialog(
                onDismissRequest = { acousticSealDialogVisible = false }
            )
        }

        // Save Custom Preset Dialog
        if (savePresetDialogVisible) {
            AlertDialog(
                onDismissRequest = { savePresetDialogVisible = false },
                title = { Text("Save Custom Preset", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column {
                        Text(
                            "Save current 10-band EQ frequencies & Clear Bass settings to your local device library:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            placeholder = { Text("e.g. My Bassline Curve", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPresetName.isNotBlank()) {
                                repository.saveUserPreset(newPresetName, tenBandEqState)
                                Toast.makeText(context, "Saved preset \"$newPresetName\"", Toast.LENGTH_SHORT).show()
                                newPresetName = ""
                                savePresetDialogVisible = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SonyCopper)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { savePresetDialogVisible = false }) {
                        Text("Cancel")
                    }
                }
            )
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
    isEqBypassed: Boolean,
    userPresets: List<UserEqPreset>,
    onToggleBypass: () -> Unit,
    onSaveCurrentPreset: () -> Unit,
    onDeleteUserPreset: (String) -> Unit,
    onSelectUserPreset: (UserEqPreset) -> Unit,
    onOpenAutoEqCatalog: () -> Unit,
    onUpdateBand: (Int, Float) -> Unit,
    onUpdateClearBass: (Int) -> Unit,
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
                        text = if (isEqBypassed) "A/B BYPASS ACTIVE (Flat 0 dB)" else "Curve: ${tenBandState.profileName}",
                        fontSize = 12.sp,
                        fontWeight = if (isEqBypassed) FontWeight.Bold else FontWeight.Normal,
                        color = if (isEqBypassed) Color(0xFFF59E0B) else SonyGold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Instant A/B Compare / Bypass Toggle Button
                    if (isEqBypassed) {
                        Button(
                            onClick = onToggleBypass,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bypass", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onToggleBypass,
                            enabled = isConnected,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(15.dp), tint = SonyCopper)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("A/B Audition", fontSize = 11.sp, color = SonyCopper)
                        }
                    }

                    OutlinedButton(
                        onClick = onImportFile,
                        enabled = isConnected,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            var selectedOverlayTarget by remember { mutableStateOf<String?>("Harman Over-Ear") }
            val overlayGains = remember(selectedOverlayTarget) {
                when (selectedOverlayTarget) {
                    "Harman Over-Ear" -> listOf(5.5f, 4.8f, 2.5f, 0.0f, -0.8f, 0.5f, 2.8f, 6.5f, 1.5f, -1.0f)
                    "Harman In-Ear" -> listOf(6.0f, 5.2f, 3.0f, -0.5f, -1.2f, 0.8f, 3.5f, 7.2f, 2.0f, -1.5f)
                    "Oratory Hi-Fi" -> listOf(4.2f, 3.8f, 1.8f, -0.5f, -0.8f, 1.2f, 2.8f, 5.5f, 1.0f, -0.5f)
                    "Rtings Neutral" -> listOf(3.5f, 2.8f, 1.2f, 0.0f, 0.0f, 0.5f, 2.0f, 4.5f, 0.5f, 0.0f)
                    else -> null
                }
            }

            // Reference Target Overlay Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reference Overlay:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                listOf("None", "Harman Over-Ear", "Harman In-Ear", "Oratory Hi-Fi", "Rtings Neutral").forEach { opt ->
                    val isSel = (if (opt == "None") selectedOverlayTarget == null else selectedOverlayTarget == opt)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) Color(0xFF00E5FF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF00E5FF) else CardBorder),
                        modifier = Modifier.clickable { selectedOverlayTarget = if (opt == "None") null else opt }
                    ) {
                        Text(
                            text = opt,
                            fontSize = 9.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Live Frequency Response Curve Canvas (Touch-Draggable Spline + Target Overlay)
            InteractiveFrequencyResponseSplineCanvas(
                bands = tenBandState.bands,
                referenceTargetGains = overlayGains,
                referenceTargetLabel = selectedOverlayTarget,
                onUpdateBand = onUpdateBand,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF07090E))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: AutoEq Catalog, Save Current Preset, Paste Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenAutoEqCatalog,
                    colors = ButtonDefaults.buttonColors(containerColor = SonyCopper),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AutoEq Database", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onSaveCurrentPreset,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = SonyCopper)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Preset", fontSize = 11.sp, color = SonyCopper)
                    }

                    TextButton(
                        onClick = onPastePeq,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Paste Text", fontSize = 11.sp, color = SonyCopper)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User Saved Presets Row
            Text("Preset Library:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SonyGold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                userPresets.forEach { preset ->
                    val isSel = (tenBandState.profileName == preset.name)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) SonyCopper else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) SonyGold else CardBorder),
                        modifier = Modifier.clickable(enabled = isConnected) { onSelectUserPreset(preset) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset.name,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                            if (!preset.id.startsWith("default_")) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Delete preset",
                                    tint = if (isSel) Color.Black else Color.Gray,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onDeleteUserPreset(preset.id) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Audiophile Target Profile Quick Chips
            Text("Reference Target Curves:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SonyCopper)
            Spacer(modifier = Modifier.height(4.dp))
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
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sony Clear Bass Sub-woofer Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Clear Bass (Sub-Woofer)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SonyCopper)
                    Text("Hardware dynamic sub-frequency boost (-10 to +10)", fontSize = 10.sp, color = Color.Gray)
                }
                Text(
                    text = "${if (tenBandState.clearBass > 0) "+" else ""}${tenBandState.clearBass}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SonyGold
                )
            }
            Slider(
                value = tenBandState.clearBass.toFloat(),
                onValueChange = { onUpdateClearBass(it.toInt()) },
                valueRange = -10f..10f,
                steps = 19,
                enabled = isConnected,
                colors = SliderDefaults.colors(thumbColor = SonyGold, activeTrackColor = SonyCopper)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Octave Band Sliders (-12 dB to +12 dB):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onResetFlat, enabled = isConnected) {
                    Text("Reset Flat (0 dB)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

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
 * Interactive Live Spline Frequency Response Visualizer Canvas.
 * Supports direct touch-dragging and tapping on filter frequency nodes with tactile haptic feedback,
 * plus optional reference target curve dotted overlays (Harman Over-Ear, Harman In-Ear, Oratory Hi-Fi, Rtings).
 */
@Composable
fun InteractiveFrequencyResponseSplineCanvas(
    bands: List<Float>,
    referenceTargetGains: List<Float>? = null,
    referenceTargetLabel: String? = null,
    onUpdateBand: ((Int, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val copperColor = SonyCopper
    val goldColor = SonyGold
    var activeDraggingIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bands) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val pointCount = bands.size
                            if (pointCount > 0) {
                                val stepX = size.width / (pointCount + 1)
                                val idx = ((offset.x / stepX) - 0.5f).toInt().coerceIn(0, pointCount - 1)
                                activeDraggingIndex = idx
                            }
                        },
                        onDragEnd = { activeDraggingIndex = null },
                        onDragCancel = { activeDraggingIndex = null },
                        onDrag = { change, _ ->
                            change.consume()
                            val idx = activeDraggingIndex ?: return@detectDragGestures
                            val midY = size.height / 2f
                            val rawGain = ((midY - change.position.y) / (size.height * 0.45f) * 12f).coerceIn(-12f, 12f)
                            val steppedGain = (rawGain * 2).roundToInt() / 2.0f
                            if (steppedGain == 0.0f || (bands.indices.contains(idx) && steppedGain != bands[idx])) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onUpdateBand?.invoke(idx, steppedGain)
                        }
                    )
                }
                .pointerInput(bands) {
                    detectTapGestures { offset ->
                        val pointCount = bands.size
                        if (pointCount > 0) {
                            val stepX = size.width / (pointCount + 1)
                            val idx = ((offset.x / stepX) - 0.5f).toInt().coerceIn(0, pointCount - 1)
                            val midY = size.height / 2f
                            val rawGain = ((midY - offset.y) / (size.height * 0.45f) * 12f).coerceIn(-12f, 12f)
                            val steppedGain = (rawGain * 2).roundToInt() / 2.0f
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpdateBand?.invoke(idx, steppedGain)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            // Draw dB Grid lines (-10dB, 0dB, +10dB)
            val gridYp10 = midY - (height * 0.4f)
            val gridYm10 = midY + (height * 0.4f)

            drawLine(color = Color(0xFF1E2433), start = Offset(0f, gridYp10), end = Offset(width, gridYp10), strokeWidth = 1f)
            drawLine(color = Color(0xFF333E55), start = Offset(0f, midY), end = Offset(width, midY), strokeWidth = 1.5f)
            drawLine(color = Color(0xFF1E2433), start = Offset(0f, gridYm10), end = Offset(width, gridYm10), strokeWidth = 1f)

            val pointCount = bands.size
            val stepX = width / (pointCount + 1)

            // 1. Draw Dotted Reference Target Curve (if selected)
            if (referenceTargetGains != null && referenceTargetGains.size == pointCount) {
                val refPoints = referenceTargetGains.mapIndexed { i, g ->
                    val px = stepX * (i + 1)
                    val py = midY - (g.coerceIn(-12f, 12f) / 12f) * (height * 0.45f)
                    Offset(px, py)
                }
                val refPath = Path()
                refPath.moveTo(0f, midY)
                for (i in refPoints.indices) {
                    val pt = refPoints[i]
                    if (i == 0) {
                        refPath.cubicTo(pt.x * 0.5f, midY, pt.x * 0.5f, pt.y, pt.x, pt.y)
                    } else {
                        val prev = refPoints[i - 1]
                        val cx = (prev.x + pt.x) / 2f
                        refPath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                    }
                }
                val last = refPoints.last()
                refPath.cubicTo((last.x + width) / 2f, last.y, (last.x + width) / 2f, midY, width, midY)

                drawPath(
                    path = refPath,
                    color = Color(0xFF00E5FF).copy(alpha = 0.65f),
                    style = Stroke(
                        width = 2.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                )
            }

            // 2. Interpolate active points across the 10 frequency centers
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

                for (i in points.indices) {
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

                fillPath.lineTo(width, height)
                fillPath.lineTo(0f, height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(copperColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(goldColor, copperColor, AncGreen)
                    ),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                for (i in points.indices) {
                    val pt = points[i]
                    val isBeingDragged = (i == activeDraggingIndex)
                    val rOuter = if (isBeingDragged) 8.5f else 5.5f
                    val rInner = if (isBeingDragged) 5.5f else 3.5f
                    drawCircle(color = Color.Black, radius = rOuter, center = pt)
                    drawCircle(color = if (isBeingDragged) Color.White else goldColor, radius = rInner, center = pt)
                }
            }
        }

        // Overlay Legend
        if (!referenceTargetLabel.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color(0xFF0F1420).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(7.dp).background(SonyGold, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Active DSP", fontSize = 9.sp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(7.dp).background(Color(0xFF00E5FF), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text(referenceTargetLabel, fontSize = 9.sp, color = Color(0xFF00E5FF))
            }
        }
    }
}

/**
 * Backward compatibility alias for FrequencyResponseSplineCanvas.
 */
@Composable
fun FrequencyResponseSplineCanvas(
    bands: List<Float>,
    modifier: Modifier = Modifier
) {
    InteractiveFrequencyResponseSplineCanvas(
        bands = bands,
        modifier = modifier
    )
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
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("On-Headset DSP Controls", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Hardware register write (Opcode 0x58)", fontSize = 11.sp, color = SonyCopper)
                }
                Surface(
                    color = SonyCopper.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SonyCopper.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Clear Bass: ${if (equalizerState.clearBass > 0) "+" else ""}${equalizerState.clearBass}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SonyGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
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
    deviceCapabilities: DeviceCapabilities,
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
                            text = "${deviceCapabilities.modelName} • ${deviceCapabilities.family.displayName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
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
                if (batteryState.isTws || deviceCapabilities.hasMultiBattery) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Earbuds & Charging Case Telemetry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = onRefreshBattery,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularBatteryGauge(
                                label = "Left Earbud",
                                level = batteryState.levelLeft,
                                isCharging = batteryState.isLeftCharging,
                                color = AncGreen
                            )
                            CircularBatteryGauge(
                                label = "Right Earbud",
                                level = batteryState.levelRight,
                                isCharging = batteryState.isRightCharging,
                                color = SonyCopper
                            )
                            CircularBatteryGauge(
                                label = "Charging Case",
                                level = batteryState.levelCase,
                                isCharging = batteryState.isCaseCharging,
                                color = SonyGold
                            )
                        }
                    }
                } else {
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
}

// -------------------------------------------------------------------------------------------------
// SOUND CONNECT HARDWARE FEATURES COMPOSABLE
// -------------------------------------------------------------------------------------------------

@Composable
fun SoundFeaturesCard(
    soundFeatures: SoundFeaturesState,
    deviceCapabilities: DeviceCapabilities,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hardware Features",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Surface(
                    color = SonyCopper.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SonyCopper.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = deviceCapabilities.modelName,
                        fontSize = 11.sp,
                        color = SonyCopper,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // 1. Speak-to-Chat (Hardware Dependent)
            if (deviceCapabilities.hasSpeakToChat) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
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
            }

            // 2. DSEE Extreme (Hardware Dependent)
            if (deviceCapabilities.hasDsee) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
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
            }

            // 3. Pause when taken off (Hardware Dependent)
            if (deviceCapabilities.hasWearingDetection) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
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
            }

            // 4. Touch Sensor Control Panel (Hardware Dependent)
            if (deviceCapabilities.hasTouchSensorControl) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
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
            }

            // 5. Sound Quality Priority Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
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
            Text("Auto Power-Off Timer (When Not Worn)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val autoOffLabels = listOf("Never", "5 min", "30 min", "60 min", "180 min")
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
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1
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
    ambientLevel: Int,
    focusOnVoice: Boolean,
    clearBass: Int,
    isConnected: Boolean,
    onSelectMode: (NoiseControlMode) -> Unit,
    onCycleMode: () -> Unit,
    onLevelChange: (Int) -> Unit,
    onVoiceFocusToggle: (Boolean) -> Unit,
    onUpdateClearBass: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
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
                    text = "Noise Control & Sound",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "MDR Wireless",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    modifier = Modifier.padding(14.dp),
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
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = currentMode.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

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

            // If Ambient is active, show the ambient level slider and voice focus
            if (currentMode == NoiseControlMode.AMBIENT_SOUND) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ambient Sound Level:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Level $ambientLevel / 20", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmbientBlue)
                    }
                    Slider(
                        value = ambientLevel.toFloat(),
                        onValueChange = { onLevelChange(it.toInt()) },
                        valueRange = 1f..20f,
                        steps = 18,
                        enabled = isConnected,
                        colors = SliderDefaults.colors(thumbColor = AmbientBlue, activeTrackColor = AmbientBlue)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Focus on Voice (speech passthrough)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Switch(
                            checked = focusOnVoice,
                            onCheckedChange = onVoiceFocusToggle,
                            enabled = isConnected,
                            colors = SwitchDefaults.colors(checkedThumbColor = AmbientBlue, checkedTrackColor = AmbientBlue.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Clear Bass Slider (Everyday subwoofer punch)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Clear Bass:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${if (clearBass > 0) "+" else ""}$clearBass dB",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("Subwoofer Boost", fontSize = 10.sp, color = Color.Gray)
                }
                Slider(
                    value = clearBass.toFloat(),
                    onValueChange = { onUpdateClearBass(it.toInt()) },
                    valueRange = -10f..10f,
                    steps = 19,
                    enabled = isConnected,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cycle Button
            Button(
                onClick = onCycleMode,
                enabled = isConnected,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cycle Mode (Simulate Quick Settings Tile Tap)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
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
fun DiagnosticsCard(
    diagnosticEntries: List<MdrDiagnosticLogEntry>,
    onCopyReport: () -> Unit,
    onClearLogs: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(diagnosticEntries, searchQuery) {
        if (searchQuery.isBlank()) diagnosticEntries
        else diagnosticEntries.filter {
            it.opcodeName.contains(searchQuery, ignoreCase = true) ||
            it.payloadHex.contains(searchQuery, ignoreCase = true) ||
            it.note.contains(searchQuery, ignoreCase = true) ||
            it.direction.name.contains(searchQuery, ignoreCase = true)
        }
    }

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
                        text = "MDR Diagnostics & Packet Stream",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                Row {
                    IconButton(onClick = onCopyReport, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Report", tint = SonyCopper, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onClearLogs, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter by opcode, hex, or direction (TX, RX)...", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                color = Color(0xFF07090E),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B202E))
            ) {
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No packets match filter" else "Awaiting Bluetooth RFCOMM session...",
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
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered.reversed(), key = { it.id }) { entry ->
                            val (dirColor, dirBg) = when (entry.direction) {
                                LogDirection.TX -> Pair(Color(0xFF0A0C10), SonyCopper)
                                LogDirection.RX -> Pair(Color(0xFF032115), AncGreen)
                                LogDirection.ACK_TX, LogDirection.ACK_RX -> Pair(Color(0xFF042436), AmbientBlue)
                                LogDirection.ERROR -> Pair(Color.White, Color(0xFFDC2626))
                                LogDirection.WARN -> Pair(Color.Black, SonyGold)
                                LogDirection.INFO -> Pair(Color(0xFFCBD5E1), Color(0xFF1E2433))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F131D), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = dirBg,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = entry.direction.name,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dirColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = entry.timeFormatted,
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (entry.opcodeName.isNotEmpty()) {
                                        Text(
                                            text = entry.opcodeName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SonyGold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (entry.payloadHex.isNotEmpty()) {
                                        Text(
                                            text = entry.payloadHex,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (entry.note.isNotEmpty()) {
                                        Text(
                                            text = entry.note,
                                            fontSize = 10.sp,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutoEqCatalogDialog(
    catalogEntries: List<AutoEqCatalogEntry>,
    connectedModel: String? = null,
    onDismiss: () -> Unit,
    onApplyProfile: (PeqProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val models = remember(catalogEntries) {
        listOf("ALL") + catalogEntries.map { it.modelName }.distinct()
    }

    var selectedModelFilter by remember(connectedModel) {
        val initial = if (!connectedModel.isNullOrBlank() && models.any { it.equals(connectedModel, ignoreCase = true) }) {
            models.first { it.equals(connectedModel, ignoreCase = true) }
        } else {
            "ALL"
        }
        mutableStateOf(initial)
    }

    val filtered = remember(searchQuery, selectedModelFilter, catalogEntries) {
        catalogEntries.filter { entry ->
            val matchModel = (selectedModelFilter == "ALL" || entry.modelName.equals(selectedModelFilter, ignoreCase = true))
            val matchQuery = searchQuery.isBlank() ||
                entry.modelName.contains(searchQuery, ignoreCase = true) ||
                entry.targetName.contains(searchQuery, ignoreCase = true) ||
                entry.labSource.contains(searchQuery, ignoreCase = true) ||
                entry.description.contains(searchQuery, ignoreCase = true)
            matchModel && matchQuery
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = SonyCopper)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AutoEq Direct Headphone Catalog", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select your exact Sony headphone model to load lab-calibrated Harman and Oratory1990 target profiles directly to hardware DSP.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search model or target (e.g. XM5, Crinacle...)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Model filter chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    models.forEach { model ->
                        val isSel = (selectedModelFilter == model)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) SonyCopper else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) SonyGold else CardBorder),
                            modifier = Modifier.clickable { selectedModelFilter = model }
                        ) {
                            Text(
                                text = model,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F131D)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = entry.modelName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SonyGold)
                                        Text(text = entry.targetName, fontSize = 11.sp, color = SonyCopper, fontWeight = FontWeight.Medium)
                                        Text(text = "Source: ${entry.labSource} • Preamp: ${entry.profile.preampDb} dB", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                    Button(
                                        onClick = { onApplyProfile(entry.profile) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SonyCopper),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = entry.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// TWS CIRCULAR BATTERY GAUGE
// -------------------------------------------------------------------------------------------------

@Composable
fun CircularBatteryGauge(
    label: String,
    level: Int,
    isCharging: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    style = Stroke(width = strokeWidth)
                )
                val sweep = (level.coerceIn(0, 100) / 100f) * 360f
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (level >= 0) "$level%" else "--",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCharging) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = "Charging",
                        tint = SonyGold,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// POWER-USER EXPANDABLE ACCORDION CARD
// -------------------------------------------------------------------------------------------------

@Composable
fun ExpandableCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SETTINGS MODAL DIALOG (THEME PALETTE, TILE CYCLE SCHEME, COLOROS)
// -------------------------------------------------------------------------------------------------

@Composable
fun SettingsDialog(
    currentSkin: ThemeSkin,
    onSelectSkin: (ThemeSkin) -> Unit,
    tileCycleScheme: TileCycleScheme,
    onSelectScheme: (TileCycleScheme) -> Unit,
    foregroundServiceEnabled: Boolean,
    onToggleForegroundService: (Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("App & Theming Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Predefined Color Palette & Themes
                Text(
                    text = "Theme & Color Palette",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Select from curated color schemes matching Sony finishes & OLED displays:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeSkin.entries.forEach { skin ->
                        val isSelected = (skin == currentSkin)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = skin.surfaceDark,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) skin.primaryGold else skin.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSkin(skin) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Palette Swatch preview: Background outer circle + Accent inner circle
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(skin.backgroundObsidian, CircleShape)
                                            .border(1.dp, skin.cardBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(skin.primaryGold, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = skin.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = skin.subtitle,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = skin.primaryGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Quick Settings Tile Scheme
                Text(
                    text = "Quick Settings Tile Cycling",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Choose which modes the Quick Settings tile switches between:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TileCycleScheme.entries.forEach { scheme ->
                    val isSel = (scheme == tileCycleScheme)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectScheme(scheme) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSel,
                            onClick = { onSelectScheme(scheme) },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = scheme.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = scheme.description, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Background Service & ColorOS
                Text(
                    text = "ColorOS & Background Execution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Prevent background suspension by aggressive OEM task managers:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Persistent Background Service", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = foregroundServiceEnabled,
                        onCheckedChange = onToggleForegroundService,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Battery Optimization Settings", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.height(12.dp))

                // Section 4: About
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sony Audio Tiles", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("v2.0.2 (Build 20002)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// ISO 226 EQUAL-LOUDNESS DYNAMIC BASS CARD
// -------------------------------------------------------------------------------------------------

@Composable
fun DynamicBassCard(
    isEnabled: Boolean,
    activeCompensationOffset: Int,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SonyCopper)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ISO 226 Dynamic Bass Compensation",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isEnabled) "Active: +$activeCompensationOffset dB Clear Bass at current volume" else "Disabled (Manual Clear Bass)",
                            fontSize = 11.sp,
                            color = if (isEnabled) AncGreen else Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = SonyGold, checkedTrackColor = SonyCopper)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Based on ISO 226 / Fletcher-Munson equal-loudness contours: human ear sensitivity to sub-bass decreases drastically at low playback volumes. This dynamically offsets Clear Bass by +1 to +4 dB at 10%–40% volume, gradually tapering to zero at higher listening levels.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                lineHeight = 15.sp
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// ACOUSTIC SEAL TEST CARD & MODAL DIALOG
// -------------------------------------------------------------------------------------------------

@Composable
fun AcousticSealCard(
    onOpenTest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Acoustic Seal & Fit Verification",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Verify silicone/foam eartip acoustic canal seal with calibrated low-frequency sweeps.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Button(
                onClick = onOpenTest,
                colors = ButtonDefaults.buttonColors(containerColor = SonyCopper),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Fit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
fun AcousticSealDialog(
    onDismissRequest: () -> Unit
) {
    val tester = remember { AcousticSealTester }
    var activeFreq by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            tester.release()
        }
    }

    AlertDialog(
        onDismissRequest = {
            tester.stop()
            onDismissRequest()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hearing, contentDescription = null, tint = SonyCopper)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Acoustic Seal & Fit Check", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column {
                Text(
                    "Play calibrated test tones to verify if your ear tips achieve an airtight sub-bass seal:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    Pair(40, "40 Hz Deep Sub-Bass (Seal Leakage Test)"),
                    Pair(100, "100 Hz Punch & Mid-Bass Impact"),
                    Pair(250, "250 Hz Low-Mid Body & Warmth"),
                    Pair(1000, "1000 Hz Midrange Reference (0 dB)")
                ).forEach { (freq, desc) ->
                    val isPlaying = (activeFreq == freq)
                    Button(
                        onClick = {
                            if (isPlaying) {
                                tester.stop()
                                activeFreq = 0
                            } else {
                                tester.playTone(freq, 2500)
                                activeFreq = freq
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) SonyGold else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "Playing $freq Hz Tone..." else desc,
                            fontSize = 11.sp,
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPlaying) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Fit Evaluation:\n• If 40 Hz is faint or barely audible, acoustic seal is compromised. Try larger silicone or memory foam tips.\n• If 40 Hz produces deep, tactile sub-bass vibration, optimal acoustic seal is confirmed.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    tester.stop()
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SonyCopper)
            ) {
                Text("Done")
            }
        }
    )
}
