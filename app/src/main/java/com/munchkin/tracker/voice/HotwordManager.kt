package com.munchkin.tracker.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotwordManager @Inject constructor(
    @ApplicationContext private val context: Context
) : RecognitionListener {

    companion object {
        private const val TAG = "HotwordManager"
        private const val SAMPLE_RATE = 16000f
    }

    private var speechService: SpeechService? = null
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioThread: Thread? = null
    private var audioRecord: AudioRecord? = null
    private val isRunning = AtomicBoolean(false)
    private val isStopping = AtomicBoolean(false)

    private val _hotwordDetected = MutableStateFlow(false)
    val hotwordDetected: StateFlow<Boolean> = _hotwordDetected

    fun start() {
        if (isRunning.get()) {
            Log.d(TAG, "Already running")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted!")
            return
        }

        if (isStopping.get()) {
            Log.d(TAG, "Still stopping, retry later")
            return
        }

        isRunning.set(true)

        try {
            StorageService.unpack(context, "vosk-model-small-ru-0.22", "model-ru",
                { model ->
                    this.model = model
                    try {
                        recognizer = Recognizer(model, SAMPLE_RATE)
                        Log.d(TAG, "Model loaded successfully")

                        audioThread = Thread {
                            try {
                                val bufferSize = AudioRecord.getMinBufferSize(
                                    SAMPLE_RATE.toInt(),
                                    android.media.AudioFormat.CHANNEL_IN_MONO,
                                    android.media.AudioFormat.ENCODING_PCM_16BIT
                                )

                                audioRecord = AudioRecord(
                                    android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION,
                                    SAMPLE_RATE.toInt(),
                                    android.media.AudioFormat.CHANNEL_IN_MONO,
                                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                                    bufferSize * 2
                                )

                                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                                    Log.e(TAG, "AudioRecord not initialized")
                                    return@Thread
                                }

                                audioRecord?.startRecording()
                                Log.d(TAG, "🎤 Started listening for hotword...")

                                val buffer = ShortArray(bufferSize)
                                while (isRunning.get() && !Thread.currentThread().isInterrupted) {
                                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                                    if (bytesRead > 0) {
                                        if (recognizer?.acceptWaveForm(buffer, bytesRead) == true) {
                                            val result = recognizer?.result
                                            val text = JSONObject(result ?: "{}").optString("text", "")
                                            Log.d(TAG, "Result: $text")
                                        } else {
                                            val partial = recognizer?.partialResult ?: "{}"
                                            val text = JSONObject(partial).optString("partial", "")
                                            if (text.isNotEmpty() && text.length < 50) {
                                                Log.v(TAG, "Partial: $text")
                                                if (text.contains("эй манчкин", ignoreCase = true) ||
                                                    text.contains("хей манчкин", ignoreCase = true) ||
                                                    text.contains("эйманчкин", ignoreCase = true) ||
                                                    text.contains("а манчкин", ignoreCase = true) ||
                                                    text.contains("манчкин", ignoreCase = true)
                                                ) {
                                                    Log.i(TAG, "HOTWORD DETECTED: $text")
                                                    _hotwordDetected.value = true
                                                    Thread.sleep(1500)
                                                    _hotwordDetected.value = false
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: InterruptedException) {
                                Log.d(TAG, "Audio thread interrupted")
                            } catch (e: Exception) {
                                Log.e(TAG, "Audio thread error: ${e.message}", e)
                            } finally {
                                releaseAudioRecord()
                            }
                        }
                        audioThread?.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create recognizer: ${e.message}", e)
                        isRunning.set(false)
                    }
                },
                { e ->
                    Log.e(TAG, "Failed to unpack model: ${e.message}", e)
                    isRunning.set(false)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start: ${e.message}", e)
            isRunning.set(false)
        }
    }

    private fun releaseAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord: ${e.message}")
        }
        audioRecord = null
        Log.d(TAG, "AudioRecord released")
    }

    override fun onPartialResult(hypothesis: String) {}
    override fun onResult(hypothesis: String) {}
    override fun onFinalResult(hypothesis: String) {}
    override fun onError(exception: Exception) {
        Log.e(TAG, "Error: ${exception.message}", exception)
    }
    override fun onTimeout() {}

    fun stop() {
        Log.d(TAG, "Stopping hotword detection")

        if (isStopping.getAndSet(true)) {
            Log.d(TAG, "Already stopping")
            return
        }

        isRunning.set(false)

        releaseAudioRecord()

        audioThread?.interrupt()
        try {
            audioThread?.join(500)
        } catch (e: InterruptedException) {
        }
        audioThread = null

        speechService?.stop()
        speechService?.shutdown()

        isStopping.set(false)
        Log.d(TAG, "Hotword detection stopped")
    }
}