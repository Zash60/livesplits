package com.livesplits.ui.screens.splits

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.livesplits.domain.model.SegmentDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitsUiState(
    val categoryName: String = "",
    val segments: List<SegmentDomain> = emptyList(),
    val pbTotal: Long = 0L,
    val sumOfBest: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddSegmentDialog: Boolean = false,
    val showEditSegmentDialog: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val selectedSegment: SegmentDomain? = null,
    val selectedSegmentForAction: SegmentDomain? = null
)

@HiltViewModel
class SplitsViewModel @Inject constructor(
    private val getSegmentsByCategoryIdUseCase: com.livesplits.domain.usecase.segment.GetSegmentsByCategoryIdUseCase,
    private val insertSegmentUseCase: com.livesplits.domain.usecase.segment.InsertSegmentUseCase,
    private val updateSegmentUseCase: com.livesplits.domain.usecase.segment.UpdateSegmentUseCase,
    private val updateSegmentTimesUseCase: com.livesplits.domain.usecase.segment.UpdateSegmentTimesUseCase,
    private val deleteSegmentUseCase: com.livesplits.domain.usecase.segment.DeleteSegmentUseCase,
    private val reorderSegmentUseCase: com.livesplits.domain.usecase.segment.ReorderSegmentUseCase,
    private val getTotalPbTimeUseCase: com.livesplits.domain.usecase.segment.GetTotalPbTimeUseCase,
    private val getSumOfBestUseCase: com.livesplits.domain.usecase.segment.GetSumOfBestUseCase,
    private val getCategoryByIdUseCase: com.livesplits.domain.usecase.category.GetCategoryByIdUseCase
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(SplitsUiState())
    val uiState: StateFlow<SplitsUiState> = _uiState

    private var categoryId: Long = 0

    fun init(categoryId: Long) {
        this.categoryId = categoryId
        loadCategoryName(categoryId)
        loadSegments(categoryId)
        loadStats(categoryId)
    }

    private fun loadCategoryName(categoryId: Long) {
        viewModelScope.launch {
            val category = getCategoryByIdUseCase(categoryId)
            if (category != null) {
                _uiState.value = _uiState.value.copy(categoryName = category.name)
            }
        }
    }

    private fun loadSegments(categoryId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getSegmentsByCategoryIdUseCase(categoryId).collect { segments ->
                _uiState.value = _uiState.value.copy(
                    segments = segments,
                    isLoading = false
                )
            }
        }
    }

    private fun loadStats(categoryId: Long) {
        viewModelScope.launch {
            val pbTotal = getTotalPbTimeUseCase(categoryId)
            val sumOfBest = getSumOfBestUseCase(categoryId)
            _uiState.value = _uiState.value.copy(
                pbTotal = pbTotal,
                sumOfBest = sumOfBest
            )
        }
    }

    fun showAddSegmentDialog() {
        _uiState.value = _uiState.value.copy(
            showAddSegmentDialog = true,
            selectedSegment = null
        )
    }

    fun hideAddSegmentDialog() {
        _uiState.value = _uiState.value.copy(showAddSegmentDialog = false)
    }

    fun showEditSegmentDialog(segment: SegmentDomain) {
        _uiState.value = _uiState.value.copy(
            showEditSegmentDialog = true,
            selectedSegment = segment
        )
    }

    fun hideEditSegmentDialog() {
        _uiState.value = _uiState.value.copy(showEditSegmentDialog = false)
    }

    fun showDeleteConfirm(segment: SegmentDomain) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            selectedSegment = segment
        )
    }

    fun hideDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun addSegment(name: String, position: Int) {
        viewModelScope.launch {
            insertSegmentUseCase(categoryId, name, position)
            hideAddSegmentDialog()
        }
    }

    fun updateSegment(segment: SegmentDomain, newName: String, newPosition: Int) {
        viewModelScope.launch {
            updateSegmentUseCase(segment.copy(name = newName, position = newPosition))
            hideEditSegmentDialog()
        }
    }

    fun updateSegmentTimes(segmentId: Long, pbTimeMs: Long?, bestTimeMs: Long?) {
        viewModelScope.launch {
            updateSegmentTimesUseCase(segmentId, pbTimeMs, bestTimeMs)
            loadStats(categoryId)
        }
    }

    fun deleteSegment(segment: SegmentDomain) {
        viewModelScope.launch {
            deleteSegmentUseCase(segment.id)
            hideDeleteConfirm()
            loadStats(categoryId)
        }
    }

    fun onSegmentReordered(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val segments = _uiState.value.segments
            if (fromIndex in segments.indices && toIndex in segments.indices) {
                val fromPosition = segments[fromIndex].position
                val toPosition = segments[toIndex].position
                reorderSegmentUseCase(categoryId, fromPosition, toPosition)
            }
        }
    }

    fun onSegmentLongPress(segment: SegmentDomain) {
        _uiState.value = _uiState.value.copy(selectedSegmentForAction = segment)
    }

    fun clearSelectedSegmentForAction() {
        _uiState.value = _uiState.value.copy(selectedSegmentForAction = null)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SplitsScreen(
    categoryId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SplitsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newSegmentName by remember { mutableStateOf("") }
    var newSegmentPosition by remember { mutableStateOf("1") }
    var editSegmentName by remember { mutableStateOf("") }
    var editSegmentPosition by remember { mutableStateOf("") }
    var editPbTime by remember { mutableStateOf("") }
    var editBestTime by remember { mutableStateOf("") }

    LaunchedEffect(categoryId) {
        viewModel.init(categoryId)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.categoryName,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.selectedSegmentForAction?.let { selectedSegment ->
                        IconButton(onClick = {
                            viewModel.showEditSegmentDialog(selectedSegment)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            viewModel.showDeleteConfirm(selectedSegment)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddSegmentDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Split")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PB Total", style = MaterialTheme.typography.labelMedium)
                    Text(formatTime(uiState.pbTotal))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sum of Best", style = MaterialTheme.typography.labelMedium)
                    Text(formatTime(uiState.sumOfBest))
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.segments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No splits yet. Tap + to add one!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.segments, key = { _, segment -> segment.id }) { index, segment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { /* Could show segment details */ },
                                    onLongClick = { viewModel.onSegmentLongPress(segment) }
                                )
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
                                        text = "#${index + 1} ${segment.name}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "PB: ${formatTime(segment.pbTimeMs)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Best: ${formatTime(segment.bestTimeMs)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Segment Dialog
    if (uiState.showAddSegmentDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideAddSegmentDialog() },
            title = { Text("Add Split") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newSegmentName,
                        onValueChange = { newSegmentName = it },
                        label = { Text("Split Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newSegmentPosition,
                        onValueChange = { newSegmentPosition = it },
                        label = { Text("Position") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addSegment(
                            newSegmentName,
                            newSegmentPosition.toIntOrNull() ?: uiState.segments.size + 1
                        )
                    },
                    enabled = newSegmentName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideAddSegmentDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Segment Dialog
    if (uiState.showEditSegmentDialog) {
        uiState.selectedSegment?.let { selectedSegment ->
            LaunchedEffect(selectedSegment) {
                editSegmentName = selectedSegment.name
                editSegmentPosition = selectedSegment.position.toString()
                editPbTime = selectedSegment.pbTimeMs.toString()
                editBestTime = selectedSegment.bestTimeMs.toString()
            }
            AlertDialog(
                onDismissRequest = { viewModel.hideEditSegmentDialog() },
                title = { Text("Edit Split") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editSegmentName,
                            onValueChange = { editSegmentName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editSegmentPosition,
                            onValueChange = { editSegmentPosition = it },
                            label = { Text("Position") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editPbTime,
                            onValueChange = { editPbTime = it },
                            label = { Text("PB Time (ms)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editBestTime,
                            onValueChange = { editBestTime = it },
                            label = { Text("Best Time (ms)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateSegment(
                                selectedSegment,
                                editSegmentName,
                                editSegmentPosition.toIntOrNull() ?: selectedSegment.position
                            )
                            viewModel.updateSegmentTimes(
                                selectedSegment.id,
                                editPbTime.toLongOrNull(),
                                editBestTime.toLongOrNull()
                            )
                            viewModel.hideEditSegmentDialog()
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideEditSegmentDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirm) {
        uiState.selectedSegment?.let { selectedSegment ->
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteConfirm() },
                title = { Text("Delete Split") },
                text = { Text("Are you sure you want to delete '${selectedSegment.name}'?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteSegment(selectedSegment) }) {
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

fun formatTime(timeMs: Long): String {
    if (timeMs <= 0) return "--:--"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (timeMs % 1000) / 10
    return String.format("%d:%02d.%02d", minutes, seconds, milliseconds)
}
