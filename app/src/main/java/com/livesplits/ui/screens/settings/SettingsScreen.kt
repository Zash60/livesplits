package com.livesplits.ui.screens.settings

import android.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.livesplits.data.settings.AppSettings
import com.livesplits.data.settings.SettingsRepository
import com.livesplits.domain.model.ComparisonMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val showColorPicker: Boolean = false,
    val colorPickerType: ColorPickerType? = null,
    val currentColor: Int = Color.GREEN
)

enum class ColorPickerType {
    AHEAD,
    BEHIND,
    PB
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }

    fun updateLaunchGames(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLaunchGames(enabled)
        }
    }

    fun showColorPicker(type: ColorPickerType, currentColor: Int) {
        _uiState.value = _uiState.value.copy(
            showColorPicker = true,
            colorPickerType = type,
            currentColor = currentColor
        )
    }

    fun hideColorPicker() {
        _uiState.value = _uiState.value.copy(
            showColorPicker = false,
            colorPickerType = null
        )
    }

    fun updateColor(type: ColorPickerType, color: Int) {
        viewModelScope.launch {
            when (type) {
                ColorPickerType.AHEAD -> settingsRepository.updateColorAhead(color)
                ColorPickerType.BEHIND -> settingsRepository.updateColorBehind(color)
                ColorPickerType.PB -> settingsRepository.updateColorPb(color)
            }
        }
        hideColorPicker()
    }

    fun updateComparison(mode: ComparisonMode) {
        viewModelScope.launch {
            settingsRepository.updateComparison(mode)
        }
    }

    fun updateCountdownMs(ms: Long) {
        viewModelScope.launch {
            settingsRepository.updateCountdownMs(ms)
        }
    }

    fun updateShowSplitName(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowSplitName(show)
        }
    }

    fun updateShowDelta(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowDelta(show)
        }
    }

    fun updateShowMilliseconds(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowMilliseconds(show)
        }
    }

    fun updateTimerSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.updateTimerSize(size)
        }
    }

    fun updateShowBackground(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowBackground(show)
        }
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // App Behavior Section
            SettingsSection(title = "App Behavior") {
                SettingsSwitchItem(
                    label = "Launch Games",
                    description = "Try to open the game when starting timer",
                    checked = uiState.settings.launchGames,
                    onCheckedChange = { viewModel.updateLaunchGames(it) }
                )
            }

            // Timer Colors Section
            SettingsSection(title = "Timer Colors") {
                ColorPickerItem(
                    label = "Color Ahead",
                    description = "Color when ahead of PB",
                    color = uiState.settings.colorAhead,
                    onClick = { viewModel.showColorPicker(ColorPickerType.AHEAD, uiState.settings.colorAhead) }
                )
                ColorPickerItem(
                    label = "Color Behind",
                    description = "Color when behind PB",
                    color = uiState.settings.colorBehind,
                    onClick = { viewModel.showColorPicker(ColorPickerType.BEHIND, uiState.settings.colorBehind) }
                )
                ColorPickerItem(
                    label = "Color PB",
                    description = "Color when achieving new PB",
                    color = uiState.settings.colorPb,
                    onClick = { viewModel.showColorPicker(ColorPickerType.PB, uiState.settings.colorPb) }
                )
            }

            // Timing Settings Section
            SettingsSection(title = "Timing Settings") {
                // Comparison mode
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Comparison",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "What to compare against during runs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.settings.comparison == ComparisonMode.PB,
                                onClick = { viewModel.updateComparison(ComparisonMode.PB) },
                                label = { Text("PB") }
                            )
                            FilterChip(
                                selected = uiState.settings.comparison == ComparisonMode.BEST_SEGMENTS,
                                onClick = { viewModel.updateComparison(ComparisonMode.BEST_SEGMENTS) },
                                label = { Text("Best Segments") }
                            )
                        }
                    }
                }

                // Countdown
                var countdownText by remember { mutableStateOf(uiState.settings.countdownMs.toString()) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Countdown (ms)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Time to countdown before starting (0 = no countdown)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = countdownText,
                            onValueChange = { 
                                countdownText = it
                                it.toLongOrNull()?.let { ms ->
                                    viewModel.updateCountdownMs(ms)
                                }
                            },
                            label = { Text("Milliseconds") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Display Section
            SettingsSection(title = "Display") {
                SettingsSwitchItem(
                    label = "Show Split Name",
                    description = "Display current split name under timer",
                    checked = uiState.settings.showSplitName,
                    onCheckedChange = { viewModel.updateShowSplitName(it) }
                )
                SettingsSwitchItem(
                    label = "Show Delta",
                    description = "Show time difference from PB",
                    checked = uiState.settings.showDelta,
                    onCheckedChange = { viewModel.updateShowDelta(it) }
                )
                SettingsSwitchItem(
                    label = "Show Milliseconds",
                    description = "Display milliseconds in timer",
                    checked = uiState.settings.showMilliseconds,
                    onCheckedChange = { viewModel.updateShowMilliseconds(it) }
                )
                SettingsSwitchItem(
                    label = "Show Background",
                    description = "Show semi-transparent background",
                    checked = uiState.settings.showBackground,
                    onCheckedChange = { viewModel.updateShowBackground(it) }
                )

                // Timer Size
                var timerSizeText by remember { mutableStateOf(uiState.settings.timerSize.toString()) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Timer Size",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Timer text size in sp (points)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = timerSizeText,
                            onValueChange = { 
                                timerSizeText = it
                                it.toIntOrNull()?.let { size ->
                                    viewModel.updateTimerSize(size)
                                }
                            },
                            label = { Text("Size (sp)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Color Picker Dialog
    if (uiState.showColorPicker) {
        ColorPickerDialog(
            currentColor = uiState.currentColor,
            onColorSelected = { color ->
                uiState.colorPickerType?.let { type ->
                    viewModel.updateColor(type, color)
                }
            },
            onDismiss = { viewModel.hideColorPicker() }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        Divider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun SettingsSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun ColorPickerItem(
    label: String,
    description: String,
    color: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(32.dp)
                ) {
                    drawCircle(androidx.compose.ui.graphics.Color(color))
                }
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Edit, contentDescription = "Change color")
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Color preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(androidx.compose.ui.graphics.Color(selectedColor))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Predefined colors
                val colors = listOf(
                    Color.GREEN,
                    Color.RED,
                    Color.BLUE,
                    Color.YELLOW,
                    Color.CYAN,
                    Color.MAGENTA,
                    Color.WHITE,
                    Color.parseColor("#00FF88"),
                    Color.parseColor("#FF8800"),
                    Color.parseColor("#FF00FF"),
                    Color.parseColor("#88FF00"),
                    Color.parseColor("#00FFFF")
                )
                
                ColorGrid(
                    colors = colors,
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selectedColor) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorGrid(
    colors: List<Int>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    val columns = 4
    val rows = (colors.size + columns - 1) / columns

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < colors.size) {
                        val color = colors[index]
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .combinedClickable(
                                    onClick = { onColorSelected(color) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val radius = size.minDimension / 2
                                drawCircle(
                                    androidx.compose.ui.graphics.Color(color),
                                    radius = if (color == selectedColor) radius - 4f else radius
                                )
                                if (color == selectedColor) {
                                    drawCircle(
                                        androidx.compose.ui.graphics.Color(color),
                                        radius = radius,
                                        style = Stroke(width = 6f)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}
