package com.drive.roadhazard.ui.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.drive.roadhazard.R
import com.drive.roadhazard.data.EventType
import com.drive.roadhazard.data.RoadEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.util.Locale

// Helper class to manage TTS state, ensuring it's initialized before use
private class TtsManager(context: Context, private val onInit: (TextToSpeech) -> Unit) {
    val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            Log.d("EventDialog", "TTS Initialized")
            onInit(tts)
        } else {
            Log.e("EventDialog", "TTS Initialization failed")
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}


@Composable
fun EventConfirmationDialog(
    event: RoadEvent,
    onConfirm: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    // --- STT (Speech-to-Text) ---
    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }
    val sttIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    // --- TTS (Text-to-Speech) ---
    val ttsInitialized = remember { MutableStateFlow<TextToSpeech?>(null) }
    val ttsManager = remember {
        TtsManager(context) { ttsEngine ->
            ttsInitialized.value = ttsEngine
        }
    }

    // --- Main Logic Flow ---
    LaunchedEffect(key1 = event) {
        // 1. Wait for TTS to be initialized
        val tts = ttsInitialized.first { it != null } ?: return@LaunchedEffect

        val question = when (event.type) {
            EventType.POTHOLE -> "Was it a pothole?"
            EventType.SPEED_BREAKER -> "Was it a speed breaker?"
            EventType.MULTIPLE_SPEED_BREAKERS -> "Were there multiple speed breakers?"
            else -> "Was it a road event?"
        }

        // 2. Set up STT listener
        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                Log.d("EventDialog", "STT: Ready for speech")
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val response = matches[0].lowercase(Locale.ROOT)
                    Log.d("EventDialog", "STT Result: $response")
                    when {
                        response.contains("yes") || response.contains("yeah") -> onConfirm(true)
                        response.contains("no") || response.contains("nope") -> onConfirm(false)
                        else -> onConfirm(false) // No match
                    }
                } else {
                    onConfirm(false) // No results
                }
            }

            override fun onError(error: Int) {
                isListening = false
                Log.e("EventDialog", "STT Error: $error")
                onConfirm(false) // On error, dismiss
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(recognitionListener)

        // 3. Set up TTS listener
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                // When TTS is done, start listening
                Log.d("EventDialog", "TTS Done, starting STT")
                (context as? Activity)?.runOnUiThread {
                    try {
                        speechRecognizer.startListening(sttIntent)
                    } catch (e: Exception) {
                        Log.e("EventDialog", "STT startListening failed", e)
                        onConfirm(false)
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e("EventDialog", "TTS Error")
                onConfirm(false) // Can't speak
            }
        })

        // 4. Start TTS
        tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "confirmation_prompt")
    }

    // --- Cleanup ---
    DisposableEffect(key1 = Unit) {
        onDispose {
            Log.d("EventDialog", "Disposing TTS and STT")
            ttsManager.shutdown()
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }
    }

    // --- UI ---
    Dialog(onDismissRequest = { onConfirm(false) }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isListening) "Listening..." else "Event Detected!", // Dynamic title
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                val (eventIcon, eventName) = when (event.type) {
                    EventType.SPEED_BREAKER -> "⚠️" to "SPEED BREAKER"
                    EventType.POTHOLE -> "🕳️" to "POTHOLE"
                    EventType.BROKEN_PATCH -> "🚧" to "BROKEN PATCH"
                    EventType.MULTIPLE_SPEED_BREAKERS -> "⚠️⚠️" to "MULTIPLE SPEED BREAKERS"
                }

                Text(
                    text = "$eventIcon $eventName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                InfoRow(
                    "Location",
                    "${String.format("%.6f", event.latitude)}, ${
                        String.format(
                            "%.6f",
                            event.longitude
                        )
                    }"
                )
                InfoRow("Speed", "${event.speed.toInt()} km/h")

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { onConfirm(false) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "No",
                            modifier = Modifier.size(20.dp)
                        )
                        Text("No", modifier = Modifier.padding(start = 8.dp))
                    }
                    TextButton(
                        onClick = { onConfirm(true) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4CAF50))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = "Yes",
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Yes", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
    }
}