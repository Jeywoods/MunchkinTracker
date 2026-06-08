package com.munchkin.tracker.voice

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.munchkin.tracker.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class LLMAction(
    val type: String,
    val player: String,
    val value: Any? = null
)

data class LLMResponse(
    val actions: List<LLMAction> = emptyList()
)

data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class OpenRouterResponse(
    val choices: List<Choice> = emptyList()
)

data class Choice(
    val message: MessageContent = MessageContent("")
)

data class MessageContent(
    val content: String = ""
)

@Singleton
class LLMParser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "LLMParser"
        private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODEL = "openai/gpt-oss-120b:free"
    }

    private val API_KEY: String by lazy {
        context.getString(R.string.openrouter_api_key)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun parseCommand(
        userText: String,
        playersInfo: List<String>
    ): List<LLMAction> = withContext(Dispatchers.IO) {
        try {
            val playersList = playersInfo.joinToString("\n") { "  - $it" }

            val prompt = """
Ты — парсер голосовых команд для настольной игры Манчкин. Преобразуй голосовую команду в JSON со списком действий.

Текущие игроки:
$playersList

Голосовая команда: "$userText"

ТИПЫ ДЕЙСТВИЙ:
- "set_power" — установить силу (value: число 1-50)
- "set_race" — установить ПЕРВУЮ расу (value: "Человек","Эльф","Дварф","Хафлинг")
- "set_race2" — установить ВТОРУЮ расу (value: "Человек","Эльф","Дварф","Хафлинг")
- "set_class" — установить ПЕРВЫЙ класс (value: "Воин","Волшебник","Вор","Клирик")
- "set_class2" — установить ВТОРОЙ класс (value: "Воин","Волшебник","Вор","Клирик")
- "level_change" — изменить уровень (value: число, + поднять, - опустить)
- "set_level" — установить уровень (value: число 1-10)

СТРОГИЕ ПРАВИЛА:
1. Если нельзя определить игрока — НЕ добавляй действие
2. Найди игрока в списке выше. Игнорируй регистр (Марат = марат). Игнорируй падеж (Маратуу→марат, Диме→Дима). Используй имя из списка как есть.
3. "первый класс"/"класс" → set_class, "второй класс"/"добавь класс" → set_class2
4. "первая раса"/"раса" → set_race, "вторая раса"/"добавь расу" → set_race2
5. НЕЛЬЗЯ устанавливать одинаковые значения в class1 и class2
6. НЕЛЬЗЯ устанавливать одинаковые значения в race1 и race2
7. Если назван несуществующий класс/раса — найди похожий: вор→Воин, волшебник→Волшебник, эльф→Эльф, человек→Человек, дворф→Дварф, хафлинг→Хафлинг, клирик→Клирик. Непонятно — пропусти.
8. Можно менять параметры НЕСКОЛЬКИХ игроков в одной команде
9. У каждого игрока может быть НЕСКОЛЬКО изменений в одной команде
10. "убери класс"/"убери расу" — value: "" (пустая строка)
11. "поменяй"/"измени"/"сделай"/"поставь"/"дай" → установка (set)
12. "добавь"/"ещё"/"второй" → set_class2 или set_race2
13. "повысь"/"подними"/"накинь"/"плюс" → level_change (положительное)
14. "понизь"/"опусти"/"убавь"/"минус" → level_change (отрицательное)

ПРИМЕРЫ:
"Илья сила 10" → {"actions":[{"type":"set_power","player":"Илья","value":10}]}
"поменяй Расу Влада на хафлинг и класс на воин" → {"actions":[{"type":"set_race","player":"Влад","value":"Хафлинг"},{"type":"set_class","player":"Влад","value":"Воин"}]}
"Диме добавь класс волшебник, а Илье уровень пять" → {"actions":[{"type":"set_class2","player":"Дима","value":"Волшебник"},{"type":"set_level","player":"Илья","value":5}]}
"всем плюс один" → {"actions":[]}
"убери класс у Ильи" → {"actions":[{"type":"set_class","player":"Илья","value":""}]}

Верни ТОЛЬКО JSON, без пояснений:
{"actions":[{"type":"...","player":"...","value":...}]}
""".trimIndent()

            val requestBody = OpenRouterRequest(
                model = MODEL,
                messages = listOf(
                    Message("system", "Ты — парсер голосовых команд для игры. Отвечай ТОЛЬКО JSON со списком actions."),
                    Message("user", prompt)
                )
            )

            val jsonBody = gson.toJson(requestBody)
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.d(TAG, "LLM Response: $responseBody")

            if (response.isSuccessful) {
                val openRouterResponse = gson.fromJson(responseBody, OpenRouterResponse::class.java)
                val content = openRouterResponse.choices.firstOrNull()?.message?.content ?: ""

                val jsonStart = content.indexOf('{')
                val jsonEnd = content.lastIndexOf('}') + 1
                val json = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    content.substring(jsonStart, jsonEnd)
                } else {
                    "{\"actions\": []}"
                }

                val llmResponse = gson.fromJson(json, LLMResponse::class.java)
                Log.i(TAG, "Parsed ${llmResponse.actions.size} actions: ${llmResponse.actions}")
                llmResponse.actions
            } else {
                Log.e(TAG, "API Error: ${response.code} - $responseBody")
                emptyList()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}", e)
            emptyList()
        }
    }
}