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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.glance.LocalContext
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
    
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    ) {
        Text(
            text = "Today's Classes",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.Black)
            )
        )
        
        Spacer(GlanceModifier.height(8.dp))
        
        if (classes.isEmpty()) {
            Text(
                text = "No classes today",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = ColorProvider(Color.Gray)
                )
            )
        } else {
            classes.forEach { classEntry ->
                val isCurrentClass = isCurrentClass(classEntry, currentTime)
                val isUpcoming = isUpcomingClass(classEntry, currentTime)
                val textColor = when {
                    isCurrentClass -> ColorProvider(Color.Blue)
                    isUpcoming -> ColorProvider(Color.Black)
                    else -> ColorProvider(Color.Gray)
                }
                
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${classEntry.startTime} - ${classEntry.endTime}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = textColor
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = classEntry.subject,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = textColor,
                            fontWeight = if (isCurrentClass) FontWeight.Bold else FontWeight.Normal
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
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
        val startTime = parseTime(classEntry.startTime)
        val endTime = parseTime(classEntry.endTime)
        startTime.toSecondOfDay() <= currentTime.toSecondOfDay() && currentTime.toSecondOfDay() <= endTime.toSecondOfDay()
    } catch (e: Exception) {
        Log.e("TimetableWidget", "Error parsing time for current class check", e)
        false
    }
}

private fun isUpcomingClass(classEntry: ClassEntry, currentTime: LocalTime): Boolean {
    return try {
        val startTime = parseTime(classEntry.startTime)
        currentTime.toSecondOfDay() < startTime.toSecondOfDay()
    } catch (e: Exception) {
        Log.e("TimetableWidget", "Error parsing time for upcoming class check", e)
        false
    }
} 