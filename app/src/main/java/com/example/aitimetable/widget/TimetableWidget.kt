package com.example.aitimetable.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.glance.LocalContext
import androidx.glance.text.TextAlign
import com.example.aitimetable.MainActivity
import com.example.aitimetable.model.ClassEntry
import com.example.aitimetable.model.TimetableData
import com.google.gson.Gson
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}

class TimetableWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val classes = getTodayClasses(context)
            TimetableWidgetContent(classes)
        }
    }

    private fun getTodayClasses(context: Context): List<ClassEntry> {
        try {
            context.openFileInput("timetable.json").use { input ->
                val json = input.bufferedReader().use { it.readText() }
                Log.d("TimetableWidget", "Timetable JSON: $json")
                
                val gson = Gson()
                val timetable = gson.fromJson(json, TimetableData::class.java)
                
                val today = LocalDate.now().dayOfWeek
                val todayAbbr = today.getDisplayName(DateTextStyle.SHORT, Locale.ENGLISH).uppercase()
                
                Log.d("TimetableWidget", "Today is: $today ($todayAbbr)")
                Log.d("TimetableWidget", "Available days: ${timetable.days.map { it.day }}")
                
                return timetable.days.find { it.day.uppercase() == todayAbbr }?.classes ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("TimetableWidget", "Error reading timetable", e)
            return emptyList()
        }
    }
}

@Composable
private fun TimetableWidgetContent(classes: List<ClassEntry>) {
    val context = LocalContext.current
    val currentTime = LocalTime.now()
    
    // Dark theme colors matching app
    val darkBackground = Color(0xFF16161A)
    val darkSurface = Color(0xFF242629)
    val darkElevated = Color(0xFF2E2F33)
    val darkTextPrimary = Color(0xFFECECEC)
    val darkTextSecondary = Color(0xFF94A1B2)
    val accentPurple = Color(0xFF7F5AF0)
    val accentTeal = Color(0xFF2CB67D)
    val accentYellow = Color(0xFFFFD166)
    
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(darkBackground))
            .cornerRadius(12.dp)
    ) {
        // Compact header
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(accentPurple.copy(alpha = 0.2f)))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Schedify",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(darkTextPrimary)
                )
            )
            
            Spacer(GlanceModifier.defaultWeight())
            
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(darkTextSecondary)
                )
            )
        }
        
        Spacer(GlanceModifier.height(4.dp))
        
        // Classes content
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        ) {
            if (classes.isEmpty()) {
                // Empty state
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = "No classes today",
                        style = TextStyle(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = ColorProvider(darkTextSecondary)
                        )
                    )
                }
            } else {
                // Show classes in a more compact way
                // Determine if we have too many classes to display
                val displayedClasses = if (classes.size > 5) classes.take(4) else classes
                val hasMore = classes.size > displayedClasses.size
                
                displayedClasses.forEachIndexed { index, classEntry ->
                    val isCurrentClass = isCurrentClass(classEntry, currentTime)
                    val isUpcoming = isUpcomingClass(classEntry, currentTime)
                    
                    val statusColor = when {
                        isCurrentClass -> accentTeal
                        isUpcoming -> accentYellow
                        else -> darkTextSecondary
                    }
                    
                    val backgroundColor = when {
                        isCurrentClass -> darkElevated.copy(alpha = 0.8f)
                        else -> darkSurface.copy(alpha = 0.6f)
                    }
                    
                    // More compact class card
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ColorProvider(backgroundColor))
                            .cornerRadius(8.dp)
                            .padding(8.dp)
                    ) {
                        // Class title and time in a more compact layout
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            // Colored dot indicator
                            Box(
                                modifier = GlanceModifier
                                    .size(6.dp)
                                    .cornerRadius(3.dp)
                                    .background(ColorProvider(statusColor))
                            ) {}
                            
                            Spacer(GlanceModifier.width(6.dp))
                            
                            // Subject with time on same line
                            Column(
                                modifier = GlanceModifier.defaultWeight()
                            ) {
                                // Subject name
                                Text(
                                    text = classEntry.subject,
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(darkTextPrimary)
                                    ),
                                    maxLines = 1
                                )
                                
                                // Combined time and room info
                                Row(
                                    verticalAlignment = Alignment.Vertical.CenterVertically
                                ) {
                                    Text(
                                        text = "${classEntry.startTime}-${classEntry.endTime}",
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            color = ColorProvider(statusColor)
                                        )
                                    )
                                    
                                    // Only show room if available
                                    classEntry.room?.let { room ->
                                        Spacer(GlanceModifier.width(4.dp))
                                        Text(
                                            text = "• $room",
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                color = ColorProvider(darkTextSecondary)
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            // Status indicator for current class (even more compact)
                            if (isCurrentClass) {
                                Box(
                                    modifier = GlanceModifier
                                        .background(ColorProvider(accentTeal.copy(alpha = 0.2f)))
                                        .cornerRadius(4.dp)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Now",
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            color = ColorProvider(accentTeal)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    // Reduced spacing between class cards
                    if (index < displayedClasses.size - 1) {
                        Spacer(GlanceModifier.height(4.dp))
                    }
                }
                
                // Show "more" indicator if we couldn't display all classes
                if (hasMore) {
                    Spacer(GlanceModifier.height(4.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        Text(
                            text = "+ ${classes.size - displayedClasses.size} more classes",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = ColorProvider(accentPurple)
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun parseTime(timeStr: String): LocalTime {
    return try {
        // Try parsing 12-hour format with AM/PM
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        LocalTime.parse(timeStr, formatter)
    } catch (e: DateTimeParseException) {
        try {
            // Try alternative 12-hour format
            val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
            LocalTime.parse(timeStr, formatter)
        } catch (e: DateTimeParseException) {
            try {
                // Try 24-hour format as fallback
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                LocalTime.parse(timeStr, formatter)
            } catch (e: DateTimeParseException) {
                // If all else fails, try to clean up the string and parse again
                val cleanTime = timeStr
                    .replace(".", ":")
                    .replace(" AM", " am")
                    .replace(" PM", " pm")
                    .trim()
                val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                LocalTime.parse(cleanTime, formatter)
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
        Log.e("TimetableWidget", "Error parsing time for current class check", e)
        false
    }
}

private fun isUpcomingClass(classEntry: ClassEntry, currentTime: LocalTime): Boolean {
    return try {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val startTime = LocalTime.parse(classEntry.startTime, formatter)
        currentTime.toSecondOfDay() < startTime.toSecondOfDay()
    } catch (e: Exception) {
        Log.e("TimetableWidget", "Error parsing time for upcoming class check", e)
        false
    }
} 