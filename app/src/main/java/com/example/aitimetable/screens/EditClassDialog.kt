package com.example.aitimetable.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.aitimetable.model.ClassEntry
import com.example.aitimetable.ui.theme.*

@Composable
fun EditClassDialog(
    classEntry: ClassEntry,
    onDismiss: () -> Unit,
    onSave: (ClassEntry) -> Unit,
    isNewClass: Boolean = false
) {
    var subject by remember { mutableStateOf(classEntry.subject) }
    var startTime by remember { mutableStateOf(classEntry.startTime) }
    var endTime by remember { mutableStateOf(classEntry.endTime) }
    var room by remember { mutableStateOf(classEntry.room ?: "") }
    var professor by remember { mutableStateOf(classEntry.professor ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = if (isNewClass) "Add Class" else "Edit Class",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = DarkPrimary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = DarkPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = DarkPrimary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = DarkPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End Time (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = DarkPrimary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = DarkPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = DarkPrimary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = DarkPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = professor,
                    onValueChange = { professor = it },
                    label = { Text("Professor (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = DarkPrimary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = DarkPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            onSave(
                                ClassEntry(
                                    subject = subject,
                                    startTime = startTime,
                                    endTime = endTime,
                                    room = room.takeIf { it.isNotBlank() },
                                    professor = professor.takeIf { it.isNotBlank() }
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
} 