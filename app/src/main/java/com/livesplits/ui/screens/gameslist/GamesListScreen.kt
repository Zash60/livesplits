package com.livesplits.ui.screens.gameslist

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.livesplits.domain.model.GameDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GamesListUiState(
    val games: List<GameDomain> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val selectedGame: GameDomain? = null,
    val showImportInstalledGames: Boolean = false,
    val selectedGameForAction: GameDomain? = null // For long press actions
)

@HiltViewModel
class GamesListViewModel @Inject constructor(
    private val getGamesUseCase: com.livesplits.domain.usecase.game.GetGamesUseCase,
    private val insertGameUseCase: com.livesplits.domain.usecase.game.InsertGameUseCase,
    private val updateGameUseCase: com.livesplits.domain.usecase.game.UpdateGameUseCase,
    private val deleteGameUseCase: com.livesplits.domain.usecase.game.DeleteGameUseCase
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(GamesListUiState())
    val uiState: StateFlow<GamesListUiState> = _uiState

    init {
        loadGames()
    }

    private fun loadGames() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getGamesUseCase().collect { games ->
                _uiState.value = _uiState.value.copy(
                    games = games,
                    isLoading = false
                )
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            selectedGame = null
        )
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun showEditDialog(game: GameDomain) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedGame = game
        )
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false)
    }

    fun showDeleteConfirm(game: GameDomain) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            selectedGame = game
        )
    }

    fun hideDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun addGame(name: String) {
        viewModelScope.launch {
            insertGameUseCase(name)
            hideAddDialog()
        }
    }

    fun updateGame(game: GameDomain, newName: String) {
        viewModelScope.launch {
            updateGameUseCase(game.copy(name = newName))
            hideEditDialog()
        }
    }

    fun deleteGame(game: GameDomain) {
        viewModelScope.launch {
            deleteGameUseCase(game.id)
            hideDeleteConfirm()
        }
    }

    fun showImportInstalledGames() {
        _uiState.value = _uiState.value.copy(showImportInstalledGames = true)
    }

    fun hideImportInstalledGames() {
        _uiState.value = _uiState.value.copy(showImportInstalledGames = false)
    }

    fun importInstalledGames(context: android.content.Context) {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val packageManager = context.packageManager
                val installedApps = packageManager.getInstalledApplications(0)
                
                installedApps.forEach { appInfo ->
                    val appName = appInfo.loadLabel(packageManager).toString()
                    // Filter out system apps and add only user-friendly apps
                    if (!appInfo.packageName.startsWith("com.android") &&
                        !appInfo.packageName.startsWith("android") &&
                        appName.length < 50
                    ) {
                        insertGameUseCase(appName, appInfo.packageName)
                    }
                }
            }
            hideImportInstalledGames()
        }
    }

    fun onGameLongPress(game: GameDomain) {
        _uiState.value = _uiState.value.copy(selectedGameForAction = game)
    }

    fun clearSelectedGameForAction() {
        _uiState.value = _uiState.value.copy(selectedGameForAction = null)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamesListScreen(
    onNavigateToGame: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: GamesListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newGameName by remember { mutableStateOf("") }
    var editGameName by remember { mutableStateOf("") }
    val context = LocalContext.current

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LiveSplits") },
                actions = {
                    uiState.selectedGameForAction?.let { selectedGame ->
                        IconButton(onClick = {
                            viewModel.showEditDialog(selectedGame)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            viewModel.showDeleteConfirm(selectedGame)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Game")
            }
        }
    ) { paddingValues ->
        if (uiState.games.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No games yet. Tap + to add one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.games, key = { it.id }) { game ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onNavigateToGame(game.id) },
                                onLongClick = { viewModel.onGameLongPress(game) }
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = game.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (game.packageName != null) {
                                Text(
                                    text = game.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Game Dialog
    if (uiState.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideAddDialog() },
            title = { Text("Add Game") },
            text = {
                OutlinedTextField(
                    value = newGameName,
                    onValueChange = { newGameName = it },
                    label = { Text("Game Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.addGame(newGameName) },
                    enabled = newGameName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideAddDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Game Dialog
    if (uiState.showEditDialog) {
        uiState.selectedGame?.let { selectedGame ->
            LaunchedEffect(selectedGame) {
                editGameName = selectedGame.name
            }
            AlertDialog(
                onDismissRequest = { viewModel.hideEditDialog() },
                title = { Text("Edit Game") },
                text = {
                    OutlinedTextField(
                        value = editGameName,
                        onValueChange = { editGameName = it },
                        label = { Text("Game Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.updateGame(selectedGame, editGameName) },
                        enabled = editGameName.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideEditDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirm) {
        uiState.selectedGame?.let { selectedGame ->
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteConfirm() },
                title = { Text("Delete Game") },
                text = { Text("Are you sure you want to delete '${selectedGame.name}'? This will also delete all categories and splits.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteGame(selectedGame) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
