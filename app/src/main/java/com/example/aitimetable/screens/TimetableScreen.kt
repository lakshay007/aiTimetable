package com.example.aitimetable.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aitimetable.model.ClassEntry
import com.example.aitimetable.model.DaySchedule
import com.example.aitimetable.model.TimetableData
import com.example.aitimetable.ui.theme.*
import com.example.aitimetable.viewmodel.TimetableViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel = viewModel()
) {
    val timetableData by viewModel.timetableData.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableStateOf(0) }
    var selectedClassIndex by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    val pageCount = remember(timetableData) { timetableData?.days?.size ?: 0 }
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val scope = rememberCoroutineScope()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            viewModel.processTimetableImage(selectedUri)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("AI Timetable") },
                actions = {
                    if (timetableData != null) {
                        IconButton(onClick = { 
                            selectedDayIndex = pagerState.currentPage
                            showAddDialog = true 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Class")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Timetable") },
                                onClick = {
                                    viewModel.deleteTimetable()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = DarkError
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = DarkPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Processing timetable...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                timetableData?.let { data ->
                    if (data.days.isNotEmpty()) {
                        Column {
                            ScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = DarkSurface,
                                contentColor = Color.White,
                                edgePadding = 0.dp
                            ) {
                                data.days.forEachIndexed { index, day ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        text = { Text(day.day) }
                                    )
                                }
                            }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                if (page < data.days.size) {
                                    DaySchedule(
                                        daySchedule = data.days[page],
                                        dayIndex = page,
                                        onEditClass = { classIndex ->
                                            selectedDayIndex = page
                                            selectedClassIndex = classIndex
                                            showEditDialog = true
                                        },
                                        onDeleteClass = { classIndex ->
                                            viewModel.deleteClass(page, classIndex)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        EmptyState(onUpload = { imagePickerLauncher.launch("image/*") })
                    }
                } ?: EmptyState(onUpload = { imagePickerLauncher.launch("image/*") })

                // Show error if any
                viewModel.error?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        containerColor = DarkError,
                        contentColor = Color.White,
                    ) {
                        Text(error)
                    }
                }
            }
        }

        if (showEditDialog && selectedClassIndex != null) {
            timetableData?.let { data ->
                if (selectedDayIndex < data.days.size && selectedClassIndex!! < data.days[selectedDayIndex].classes.size) {
                    val classEntry = data.days[selectedDayIndex].classes[selectedClassIndex!!]
                    EditClassDialog(
                        classEntry = classEntry,
                        onDismiss = { showEditDialog = false },
                        onSave = { updatedClass ->
                            viewModel.updateClass(selectedDayIndex, selectedClassIndex!!, updatedClass)
                            showEditDialog = false
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            EditClassDialog(
                classEntry = ClassEntry("", "", "", null, null),
                onDismiss = { showAddDialog = false },
                onSave = { newClass ->
                    viewModel.addClass(selectedDayIndex, newClass)
                    showAddDialog = false
                },
                isNewClass = true
            )
        }
    }
}

@Composable
private fun EmptyState(onUpload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No timetable available",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Upload your timetable image to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onUpload,
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkPrimary,
                contentColor = Color.Black
            ),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Upload")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload Timetable")
        }
    }
}

@Composable
private fun DaySchedule(
    daySchedule: DaySchedule,
    dayIndex: Int,
    onEditClass: (Int) -> Unit,
    onDeleteClass: (Int) -> Unit
) {
    val currentTime = LocalTime.now()
    val subjectColorMap = remember(daySchedule.classes) {
        daySchedule.classes.map { it.subject }.distinct()
            .mapIndexed { index, subject -> 
                subject to SubjectColors[index % SubjectColors.size]
            }.toMap()
    }

    if (daySchedule.classes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No classes for this day",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            itemsIndexed(daySchedule.classes) { index, classEntry ->
                val isCurrentClass = try {
                    isCurrentClass(classEntry, currentTime)
                } catch (e: Exception) {
                    false
                }
                val isUpcoming = try {
                    isUpcomingClass(classEntry, currentTime)
                } catch (e: Exception) {
                    false
                }
                
                ClassCard(
                    classEntry = classEntry,
                    onEdit = { onEditClass(index) },
                    onDelete = { onDeleteClass(index) },
                    isCurrentClass = isCurrentClass,
                    isUpcoming = isUpcoming,
                    subjectColor = subjectColorMap[classEntry.subject] ?: SubjectColors[0]
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassCard(
    classEntry: ClassEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isCurrentClass: Boolean,
    isUpcoming: Boolean,
    subjectColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        isCurrentClass -> CurrentClassColor.copy(alpha = 0.1f)
                        isUpcoming -> UpcomingClassColor.copy(alpha = 0.1f)
                        else -> Color.Transparent
                    }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = classEntry.subject,
                    style = MaterialTheme.typography.titleMedium,
                    color = subjectColor
                )
                Text(
                    text = "${classEntry.startTime} - ${classEntry.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentClass) CurrentClassColor 
                           else if (isUpcoming) UpcomingClassColor
                           else PastClassColor
                )
                classEntry.room?.let { room ->
                    Text(
                        text = "Room: $room",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                classEntry.professor?.let { professor ->
                    Text(
                        text = "Prof: $professor",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = DarkError.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun isCurrentClass(classEntry: ClassEntry, currentTime: LocalTime): Boolean {
    return try {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val startTime = LocalTime.parse(classEntry.startTime, formatter)
        val endTime = LocalTime.parse(classEntry.endTime, formatter)
        startTime.toSecondOfDay() <= currentTime.toSecondOfDay() && currentTime.toSecondOfDay() <= endTime.toSecondOfDay()
    } catch (e: Exception) {
        false
    }
}

private fun isUpcomingClass(classEntry: ClassEntry, currentTime: LocalTime): Boolean {
    return try {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val startTime = LocalTime.parse(classEntry.startTime, formatter)
        currentTime.toSecondOfDay() < startTime.toSecondOfDay()
    } catch (e: Exception) {
        false
    }
} 