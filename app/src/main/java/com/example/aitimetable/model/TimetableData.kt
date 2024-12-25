package com.example.aitimetable.model

import kotlinx.serialization.Serializable

@Serializable
data class TimetableData(
    val days: List<DaySchedule>
)

@Serializable
data class DaySchedule(
    val day: String,
    val classes: List<ClassEntry>
)

@Serializable
data class ClassEntry(
    val subject: String,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val professor: String? = null
) 