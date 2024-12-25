package com.example.aitimetable.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aitimetable.model.ClassEntry
import com.example.aitimetable.model.DaySchedule
import com.example.aitimetable.model.TimetableData
import com.example.aitimetable.network.GptVisionService
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class TimetableViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    private val gptVisionService = GptVisionService(context)
    private val gson = Gson()
    
    private val _timetableData = MutableStateFlow<TimetableData?>(null)
    val timetableData: StateFlow<TimetableData?> = _timetableData.asStateFlow()
    
    var isLoading by mutableStateOf(false)
        private set
    
    var error by mutableStateOf<String?>(null)
        private set

    var shouldNavigate by mutableStateOf(false)
        private set

    init {
        loadSavedTimetable()
    }

    fun resetNavigation() {
        shouldNavigate = false
    }

    fun processTimetableImage(imageUri: Uri) {
        if (_timetableData.value != null) {
            error = "A timetable already exists. Delete it first to add a new one."
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val result = gptVisionService.processTimetableImage(imageUri)
                if (result != null) {
                    _timetableData.value = result
                    saveTimetable(result)
                    shouldNavigate = true
                } else {
                    error = "Failed to process timetable"
                }
            } catch (e: Exception) {
                error = e.message ?: "An error occurred"
                _timetableData.value = null
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteTimetable() {
        viewModelScope.launch {
            try {
                // Delete the file
                context.deleteFile("timetable.json")
                // Clear the data
                _timetableData.value = null
                error = null
            } catch (e: Exception) {
                error = "Failed to delete timetable: ${e.message}"
            }
        }
    }

    fun updateClass(dayIndex: Int, classIndex: Int, updatedClass: ClassEntry) {
        _timetableData.value?.let { currentTimetable ->
            val updatedDays = currentTimetable.days.toMutableList()
            val updatedClasses = updatedDays[dayIndex].classes.toMutableList()
            updatedClasses[classIndex] = updatedClass
            updatedDays[dayIndex] = DaySchedule(
                day = updatedDays[dayIndex].day,
                classes = updatedClasses.sortedBy { it.startTime }
            )
            val updatedTimetable = TimetableData(updatedDays)
            _timetableData.value = updatedTimetable
            saveTimetable(updatedTimetable)
        }
    }

    fun addClass(dayIndex: Int, newClass: ClassEntry) {
        _timetableData.value?.let { currentTimetable ->
            val updatedDays = currentTimetable.days.toMutableList()
            val updatedClasses = updatedDays[dayIndex].classes.toMutableList()
            updatedClasses.add(newClass)
            updatedDays[dayIndex] = DaySchedule(
                day = updatedDays[dayIndex].day,
                classes = updatedClasses.sortedBy { it.startTime }
            )
            val updatedTimetable = TimetableData(updatedDays)
            _timetableData.value = updatedTimetable
            saveTimetable(updatedTimetable)
        }
    }

    fun deleteClass(dayIndex: Int, classIndex: Int) {
        _timetableData.value?.let { currentTimetable ->
            val updatedDays = currentTimetable.days.toMutableList()
            val updatedClasses = updatedDays[dayIndex].classes.toMutableList()
            updatedClasses.removeAt(classIndex)
            updatedDays[dayIndex] = DaySchedule(
                day = updatedDays[dayIndex].day,
                classes = updatedClasses
            )
            val updatedTimetable = TimetableData(updatedDays)
            _timetableData.value = updatedTimetable
            saveTimetable(updatedTimetable)
        }
    }

    private fun saveTimetable(timetableData: TimetableData) {
        viewModelScope.launch {
            try {
                context.openFileOutput("timetable.json", Context.MODE_PRIVATE).use { output ->
                    output.write(gson.toJson(timetableData).toByteArray())
                }
            } catch (e: Exception) {
                error = "Failed to save timetable: ${e.message}"
            }
        }
    }

    private fun loadSavedTimetable() {
        viewModelScope.launch {
            try {
                context.openFileInput("timetable.json").use { input ->
                    val json = input.bufferedReader().use { it.readText() }
                    _timetableData.value = gson.fromJson(json, TimetableData::class.java)
                }
            } catch (e: Exception) {
                // No saved timetable yet
                _timetableData.value = null
            }
        }
    }
} 