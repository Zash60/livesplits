package com.livesplits.ui.screens.game
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.livesplits.domain.model.CategoryDomain
import com.livesplits.domain.model.LeaderboardEntry
import com.livesplits.domain.model.SpeedrunCategorySuggestion
import com.livesplits.network.SpeedrunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class GameUiState(
    val gameName: String = "",
    val categories: List<CategoryDomain> = emptyList(),
    val leaderboardEntries: List<LeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLeaderboardLoading: Boolean = false,
    val error: String? = null,
    val showAddCategoryDialog: Boolean = false,
    val showEditCategoryDialog: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val selectedCategory: CategoryDomain? = null,
    val showCategoryBottomSheet: Boolean = false,
    val showSpeedrunSuggestions: Boolean = false,
    val speedrunSuggestions: List<SpeedrunCategorySuggestion> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryForAction: CategoryDomain? = null,
    val selectedTab: Int = 0
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val getCategoriesByGameIdUseCase: com.livesplits.domain.usecase.category.GetCategoriesByGameIdUseCase,
    private val getCategoryByIdUseCase: com.livesplits.domain.usecase.category.GetCategoryByIdUseCase,
    private val insertCategoryUseCase: com.livesplits.domain.usecase.category.InsertCategoryUseCase,
    private val updateCategoryUseCase: com.livesplits.domain.usecase.category.UpdateCategoryUseCase,
    private val updateCategoryPbUseCase: com.livesplits.domain.usecase.category.UpdateCategoryPbUseCase,
    private val updateCategoryRunCountUseCase: com.livesplits.domain.usecase.category.UpdateCategoryRunCountUseCase,
    private val deleteCategoryUseCase: com.livesplits.domain.usecase.category.DeleteCategoryUseCase,
    private val getGameByIdUseCase: com.livesplits.domain.usecase.game.GetGameByIdUseCase,
    private val speedrunRepository: SpeedrunRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    private var searchJob: Job? = null
    private var currentGameId: Long = 0

    fun init(gameId: Long) {
        currentGameId = gameId
        loadGame(gameId)
        loadCategories(gameId)
    }

    private fun loadGame(gameId: Long) {
        viewModelScope.launch {
            val game = getGameByIdUseCase(gameId)
            if (game != null) {
                _uiState.value = _uiState.value.copy(gameName = game.name)
            }
        }
    }

    private fun loadCategories(gameId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getCategoriesByGameIdUseCase(gameId).collect { categories ->
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    isLoading = false
                )
            }
        }
    }

    fun searchSpeedrunCategories(query: String, gameId: String?) {
        searchJob?.cancel()
        
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        if (query.isBlank() || gameId == null) {
            _uiState.value = _uiState.value.copy(
                speedrunSuggestions = emptyList(),
                showSpeedrunSuggestions = false
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            val result = speedrunRepository.getCategories(gameId)
            result.onSuccess { suggestions ->
                val filtered = suggestions.filter { 
                    it.name.contains(query, ignoreCase = true) 
                }
                _uiState.value = _uiState.value.copy(
                    speedrunSuggestions = filtered,
                    showSpeedrunSuggestions = filtered.isNotEmpty()
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    showSpeedrunSuggestions = false
                )
            }
        }
    }

    fun showAddCategoryDialog() {
        _uiState.value = _uiState.value.copy(
            showAddCategoryDialog = true,
            selectedCategory = null,
            searchQuery = "",
            speedrunSuggestions = emptyList()
        )
    }

    fun hideAddCategoryDialog() {
        _uiState.value = _uiState.value.copy(
            showAddCategoryDialog = false,
            searchQuery = "",
            speedrunSuggestions = emptyList(),
            showSpeedrunSuggestions = false
        )
    }

    fun showEditCategoryDialog(category: CategoryDomain) {
        _uiState.value = _uiState.value.copy(
            showEditCategoryDialog = true,
            selectedCategory = category
        )
    }

    fun hideEditCategoryDialog() {
        _uiState.value = _uiState.value.copy(showEditCategoryDialog = false)
    }

    fun showDeleteConfirm(category: CategoryDomain) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            selectedCategory = category
        )
    }

    fun hideDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun addCategory(name: String, speedrunCategoryId: String? = null) {
        viewModelScope.launch {
            insertCategoryUseCase(currentGameId, name, speedrunCategoryId = speedrunCategoryId)
            hideAddCategoryDialog()
        }
    }

    fun updateCategory(category: CategoryDomain, newName: String) {
        viewModelScope.launch {
            updateCategoryUseCase(category.copy(name = newName))
            hideEditCategoryDialog()
        }
    }

    fun updateCategoryPb(categoryId: Long, newPbMs: Long) {
        viewModelScope.launch {
            updateCategoryPbUseCase(categoryId, newPbMs)
        }
    }

    fun updateCategoryRunCount(categoryId: Long, newCount: Int) {
        viewModelScope.launch {
            updateCategoryRunCountUseCase(categoryId, newCount)
        }
    }

    fun deleteCategory(category: CategoryDomain) {
        viewModelScope.launch {
            deleteCategoryUseCase(category.id)
            hideDeleteConfirm()
        }
    }

    fun showCategoryBottomSheet(category: CategoryDomain) {
        _uiState.value = _uiState.value.copy(
            showCategoryBottomSheet = true,
            selectedCategory = category
        )
    }

    fun hideCategoryBottomSheet() {
        _uiState.value = _uiState.value.copy(showCategoryBottomSheet = false)
    }

    fun loadLeaderboard(gameId: String, categoryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLeaderboardLoading = true)
            val result = speedrunRepository.getLeaderboard(gameId, categoryId)
            result.onSuccess { entries ->
                _uiState.value = _uiState.value.copy(
                    leaderboardEntries = entries,
                    isLeaderboardLoading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLeaderboardLoading = false,
                    error = it.message
                )
            }
        }
    }

    fun onCategoryLongPress(category: CategoryDomain) {
        _uiState.value = _uiState.value.copy(selectedCategoryForAction = category)
    }

    fun clearSelectedCategoryForAction() {
        _uiState.value = _uiState.value.copy(selectedCategoryForAction = null)
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameScreen(
    gameId: Long,
    onNavigateToSplits: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newCategoryName by remember { mutableStateOf("") }
    var editCategoryName by remember { mutableStateOf("") }
    var newPbTime by remember { mutableStateOf("") }
    var newRunCount by remember { mutableStateOf("") }
    var speedrunGameId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gameId) {
        viewModel.init(gameId)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.gameName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.selectedCategoryForAction?.let { selectedCategory ->
                        IconButton(onClick = {
                            viewModel.showEditCategoryDialog(selectedCategory)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            viewModel.showDeleteConfirm(selectedCategory)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == 0) { // Categories tab
                FloatingActionButton(onClick = { viewModel.showAddCategoryDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Categories") }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Leaderboard") }
                )
            }

            when (uiState.selectedTab) {
                0 -> CategoriesTab(
                    categories = uiState.categories,
                    isLoading = uiState.isLoading,
                    onCategoryClick = { viewModel.showCategoryBottomSheet(it) },
                    onCategoryLongClick = { viewModel.onCategoryLongPress(it) }
                )
                1 -> LeaderboardTab(
                    entries = uiState.leaderboardEntries,
                    isLoading = uiState.isLeaderboardLoading
                )
            }
        }
    }

    // Add Category Dialog
    if (uiState.showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideAddCategoryDialog() },
            title = { Text("Add Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { 
                            newCategoryName = it
                            speedrunGameId?.let { gameId ->
                                viewModel.searchSpeedrunCategories(it, gameId)
                            }
                        },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Speedrun.com suggestions
                    if (uiState.showSpeedrunSuggestions) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(uiState.speedrunSuggestions) { suggestion ->
                                ListItem(
                                    headlineContent = { Text(suggestion.name) },
                                    supportingContent = { Text(suggestion.type) },
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                newCategoryName = suggestion.name
                                                viewModel.addCategory(suggestion.name, suggestion.id)
                                            }
                                        )
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.addCategory(newCategoryName) },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideAddCategoryDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Category Dialog
    if (uiState.showEditCategoryDialog) {
        uiState.selectedCategory?.let { selectedCategory ->
            LaunchedEffect(selectedCategory) {
                editCategoryName = selectedCategory.name
            }
            AlertDialog(
                onDismissRequest = { viewModel.hideEditCategoryDialog() },
                title = { Text("Edit Category") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editCategoryName,
                            onValueChange = { editCategoryName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPbTime,
                            onValueChange = { newPbTime = it },
                            label = { Text("PB Time (ms)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newRunCount,
                            onValueChange = { newRunCount = it },
                            label = { Text("Run Count") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateCategory(selectedCategory, editCategoryName)
                            if (newPbTime.isNotBlank()) {
                                viewModel.updateCategoryPb(
                                    selectedCategory.id,
                                    newPbTime.toLongOrNull() ?: 0
                                )
                            }
                            if (newRunCount.isNotBlank()) {
                                viewModel.updateCategoryRunCount(
                                    selectedCategory.id,
                                    newRunCount.toIntOrNull() ?: 0
                                )
                            }
                            viewModel.hideEditCategoryDialog()
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideEditCategoryDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirm) {
        uiState.selectedCategory?.let { selectedCategory ->
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteConfirm() },
                title = { Text("Delete Category") },
                text = { Text("Are you sure you want to delete '${selectedCategory.name}'? This will also delete all splits.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteCategory(selectedCategory) }) {
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

    // Category Bottom Sheet
    @OptIn(ExperimentalMaterial3Api::class)
    if (uiState.showCategoryBottomSheet) {
        uiState.selectedCategory?.let { selectedCategory ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideCategoryBottomSheet() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = selectedCategory.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Launch Timer option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    // Launch timer with this category
                                    viewModel.hideCategoryBottomSheet()
                                    // TODO: Launch timer service
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Launch Timer")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // View & Edit Splits option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    viewModel.hideCategoryBottomSheet()
                                    onNavigateToSplits(selectedCategory.id)
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("View & Edit Splits")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PB and Run count info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PB", style = MaterialTheme.typography.labelMedium)
                        Text(formatTime(selectedCategory.pbTimeMs))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Runs", style = MaterialTheme.typography.labelMedium)
                        Text(selectedCategory.runCount.toString())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoriesTab(
    categories: List<CategoryDomain>,
    isLoading: Boolean,
    onCategoryClick: (CategoryDomain) -> Unit,
    onCategoryLongClick: (CategoryDomain) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (categories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No categories yet. Tap + to add one!")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onCategoryClick(category) },
                            onLongClick = { onCategoryLongClick(category) }
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PB: ${formatTime(category.pbTimeMs)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Runs: ${category.runCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardTab(
    entries: List<LeaderboardEntry>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No leaderboard data available")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${entry.rank}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Column {
                            Text(
                                text = entry.playerName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            entry.date?.let { date ->
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(date)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = formatTime(entry.timeMs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(timeMs: Long): String {
    if (timeMs <= 0) return "--:--"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (timeMs % 1000) / 10
    return String.format("%d:%02d.%02d", minutes, seconds, milliseconds)
}
