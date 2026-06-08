package com.munchkin.tracker.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.munchkin.tracker.domain.model.VoiceState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VoiceManager"
    }

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var playerNames: List<String> = emptyList()

    private val _voiceState = MutableStateFlow(VoiceState.SLEEPING)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    fun updatePlayerNames(names: List<String>) { playerNames = names }

    fun init() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ru", "RU")
                ttsReady = true
            }
        }
    }

    fun startListening() {
        Log.d(TAG, "startListening() called")
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d(TAG, "SpeechRecognizer available: $available")
        if (!available) {
            Log.e(TAG, "SpeechRecognizer is NOT available!")
            _voiceState.value = VoiceState.ERROR
            return
        }
        try {
            recognizer?.destroy()
            Log.d(TAG, "Previous recognizer destroyed")
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying previous recognizer: ${e.message}")
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        Log.d(TAG, "New recognizer created: ${recognizer != null}")
        recognizer?.setRecognitionListener(buildListener())
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        }
        try {
            recognizer?.startListening(intent)
            Log.d(TAG, "Listening started successfully")
            _voiceState.value = VoiceState.RECORDING
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed: ${e.message}", e)
            _voiceState.value = VoiceState.ERROR
        }
    }

    fun stopListening() {
        try { recognizer?.stopListening(); recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
        _voiceState.value = VoiceState.SLEEPING
        _amplitude.value = 0f
    }

    fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_utterance")
    }

    private fun buildListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { _voiceState.value = VoiceState.RECORDING }
        override fun onBeginningOfSpeech() { _amplitude.value = 0.1f }
        override fun onRmsChanged(rmsdB: Float) { _amplitude.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { _amplitude.value = 0f }
        override fun onError(error: Int) {
            Log.e(TAG, "onError: $error")
            _recognizedText.value = ""
            _voiceState.value = VoiceState.ERROR
        }
        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            Log.d(TAG, "onResults: $text")
            _recognizedText.value = text
            try { recognizer?.stopListening(); recognizer?.destroy() } catch (_: Exception) {}
            recognizer = null
            _amplitude.value = 0f
            _voiceState.value = VoiceState.SLEEPING
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            if (partial.isNotEmpty()) _recognizedText.value = partial
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun destroy() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        try { tts?.shutdown() } catch (_: Exception) {}
    }
}