package com.example.aitimetable.network

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.aitimetable.BuildConfig
import com.example.aitimetable.model.TimetableData
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class GptVisionService(private val context: Context) {
    private val TAG = "GptVisionService"
    private val gson = Gson()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    
    suspend fun processTimetableImage(imageUri: Uri): TimetableData? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting image processing for URI: $imageUri")
            
    
            val bitmap = context.contentResolver.openInputStream(imageUri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            } ?: throw Exception("Failed to decode image")
            
            Log.d(TAG, "Image decoded successfully. Size: ${bitmap.width}x${bitmap.height}")
            
           
            val scaledBitmap = scaleBitmap(bitmap)
            
            
            val prompt = """
                This is a timetable image. Please extract the schedule information and return it as a JSON object with the following structure:
                {
                    "days": [
                        {
                            "day": "string",
                            "classes": [
                                {
                                    "subject": "string",
                                    "startTime": "string",
                                    "endTime": "string",
                                    "room": "string (optional)",
                                    "professor": "string (optional)"
                                }
                            ]
                        }
                    ]
                }
                
                Important instructions:
                1. Make sure subjects match the correct corresponding days
                2. Do not include breaks or lunch breaks
                3. Include all days from the timetable
                4. Use 12-hour format with AM/PM for times (e.g., "9:30 AM", "2:30 PM")
                5. Return only the JSON object, no additional text
                6. Ensure day names are in short form (MON, TUE, etc.)
                7. Make sure to extract all visible information from the image
            """.trimIndent()

         
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                    image(scaledBitmap)
                }
            )

            val result = response.text?.trim() ?: throw Exception("Empty response from Gemini")
            Log.d(TAG, "Received response: $result")

      
            val jsonStart = result.indexOf("{")
            val jsonEnd = result.lastIndexOf("}") + 1

            if (jsonStart == -1 || jsonEnd == 0) {
                Log.e(TAG, "No JSON found in response content")
                throw Exception("No JSON found in response: $result")
            }

            val jsonString = result.substring(jsonStart, jsonEnd)
            Log.d(TAG, "Extracted JSON: $jsonString")

         
            try {
                JsonParser.parseString(jsonString)
                Log.d(TAG, "JSON structure validated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Invalid JSON structure", e)
                throw Exception("Invalid JSON structure: $jsonString")
            }

            return@withContext gson.fromJson(jsonString, TimetableData::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            e.printStackTrace()
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val maxDimension = 1024
        return if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
    }
} 