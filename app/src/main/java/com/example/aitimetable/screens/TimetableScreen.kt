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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
                title = { 
                    Text(
                        "Schedify",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                actions = {
                    if (timetableData != null) {
                        // Floating action button style for add
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkPrimary)
                                .clickable { 
                                    selectedDayIndex = pagerState.currentPage
                                    showAddDialog = true 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Add, 
                                contentDescription = "Add Class",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert, 
                                contentDescription = "More options",
                                tint = DarkTextPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Timetable", color = DarkTextPrimary) },
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
                    titleContentColor = DarkTextPrimary,
                    actionIconContentColor = DarkTextPrimary
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
                            color = DarkTextPrimary,
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
                                containerColor = DarkBackground,
                                contentColor = DarkPrimary,
                                edgePadding = 0.dp,
                                divider = { 
                                    Divider(color = DarkBorder.copy(alpha = 0.5f), thickness = 1.dp) 
                                },
                                indicator = { tabPositions ->
                                    Box(
                                        Modifier
                                            .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                            .height(3.dp)
                                            .padding(horizontal = 16.dp)
                                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                            .background(DarkPrimary)
                                    )
                                }
                            ) {
                                data.days.forEachIndexed { index, day ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        text = { 
                                            Text(
                                                day.day,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (pagerState.currentPage == index) 
                                                                FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (pagerState.currentPage == index) 
                                                       DarkTextPrimary else DarkTextSecondary
                                            ) 
                                        }
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
                        containerColor = DarkError.copy(alpha = 0.9f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(8.dp)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Decorative element
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(DarkPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = DarkPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "No timetable available",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = DarkTextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Upload your timetable image to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = DarkTextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onUpload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    Icons.Rounded.Add, 
                    contentDescription = "Upload",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Upload Timetable",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = DarkTextSecondary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No classes for this day",
                    color = DarkTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
            }
            // Add some bottom padding
            item { Spacer(modifier = Modifier.height(80.dp)) }
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
    val cardBackground = if (isCurrentClass) {
        DarkElevated.copy(alpha = 0.95f)
    } else {
        DarkCardBackground
    }
    
    // Status indicator
    val statusColor = when {
        isCurrentClass -> CurrentClassColor
        isUpcoming -> UpcomingClassColor
        else -> PastClassColor
    }
    
    val statusLabel = when {
        isCurrentClass -> "Current"
        isUpcoming -> "Upcoming"
        else -> "Past"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardBackground
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isCurrentClass) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status indicator at top
            if (isCurrentClass) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(statusColor)
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(subjectColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = classEntry.subject,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = DarkTextPrimary
                        )
                    }
                    
                    // Status chip
                    if (isCurrentClass || isUpcoming) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Time indicator with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = statusColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${classEntry.startTime} - ${classEntry.endTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Room and professor info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        classEntry.room?.let { room ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = DarkTextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = room,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkTextSecondary
                                )
                            }
                        }
                        
                        classEntry.professor?.let { professor ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = DarkTextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = professor,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    // Action buttons
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(18.dp),
                                tint = DarkTextSecondary
                            )
                        }
                        
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp),
                                tint = DarkError.copy(alpha = 0.7f)
                            )
                        }
                    }
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