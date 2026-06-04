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
    @ApplicationContext private val context: Context,
    val commandParser: CommandParser
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

    private val _command = MutableStateFlow<VoiceCommand?>(null)
    val command: StateFlow<VoiceCommand?> = _command.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    // ─── Init ─────────────────────────────────────────────────────────────────
    fun init() {
        Log.d(TAG, "init() called")
        tts = TextToSpeech(context) { status ->
            Log.d(TAG, "TTS init status: $status")
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ru", "RU")
                Log.d(TAG, "TTS language set to ru-RU")
                ttsReady = true
            } else {
                Log.e(TAG, "TTS init failed with status: $status")
            }
        }
    }

    // ─── Start / stop listening ───────────────────────────────────────────────
    fun startListening() {
        Log.d(TAG, "startListening()")

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "SpeechRecognizer is NOT available on this device!")
            _voiceState.value = VoiceState.ERROR
            return
        }

        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying previous recognizer: ${e.message}")
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
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
            _voiceState.value = VoiceState.RECORDING
            Log.d(TAG, "Listening started, state=RECORDING")
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed: ${e.message}", e)
            _voiceState.value = VoiceState.ERROR
        }
    }

    fun stopListening() {
        Log.d(TAG, "stopListening() called")
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recognizer: ${e.message}")
        }
        recognizer = null
        _voiceState.value = VoiceState.SLEEPING
        _amplitude.value = 0f
        Log.d(TAG, "Listening stopped")
    }

    fun consumeCommand() {
        val cmd = _command.value
        Log.d(TAG, "consumeCommand() cmd=$cmd")
        _command.value = null
    }

    // ─── TTS ─────────────────────────────────────────────────────────────────
    fun speak(text: String) {
        Log.d(TAG, "speak(text=\"$text\") ttsReady=$ttsReady")
        if (ttsReady) {
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_utterance")
            Log.d(TAG, "TTS speak result: $result")
        } else {
            Log.w(TAG, "TTS not ready, cannot speak")
        }
    }

    // ─── Listener ─────────────────────────────────────────────────────────────
    private fun buildListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech() params=$params")
            _voiceState.value = VoiceState.RECORDING
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech() — user started speaking")
            _amplitude.value = 0.1f
        }

        override fun onRmsChanged(rmsdB: Float) {
            val amp = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            if (amp > 0.1f) {
                Log.v(TAG, "onRmsChanged(rmsdB=$rmsdB, amplitude=$amp)")
            }
            _amplitude.value = amp
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech() — user stopped speaking")
            _amplitude.value = 0f
        }

        override fun onError(error: Int) {
            val errorName = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
                SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
                SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
                SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
                SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
                else -> "UNKNOWN_ERROR($error)"
            }
            Log.e(TAG, "onError($errorName)")

            _voiceState.value = VoiceState.ERROR

            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                Log.d(TAG, "No speech detected, stopping")
                stopListening()
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull() ?: 0f

            Log.d(TAG, "onResults() text=\"$text\" confidence=$confidence allMatches=$matches")
            _recognizedText.value = text
            handleText(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            if (partial.isNotEmpty()) {
                Log.d(TAG, "onPartialResults() partial=\"$partial\"")
            }
            _recognizedText.value = partial
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "onEvent(eventType=$eventType, params=$params)")
        }
    }

    private fun handleText(text: String) {
        if (text.isBlank()) {
            stopListening()
            return
        }
        val cmd = commandParser.parse(text, playerNames)
        Log.i(TAG, "📢 Parsed command: $cmd from text=\"$text\"")
        _command.value = cmd
        stopListening()
    }

    fun destroy() {
        Log.d(TAG, "destroy() called")
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying recognizer: ${e.message}")
        }
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS: ${e.message}")
        }
    }
    fun updatePlayerNames(names: List<String>) {
        playerNames = names
    }
}