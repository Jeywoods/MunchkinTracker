package com.munchkin.tracker.voice

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

sealed class VoiceCommand {
    data class LevelChange(val playerName: String, val delta: Int) : VoiceCommand()
    data class SetLevel(val playerName: String, val level: Int) : VoiceCommand()
    data class EndGame(val winnerName: String?) : VoiceCommand()
    object NewGame : VoiceCommand()
    object Undo : VoiceCommand()
    object Unknown : VoiceCommand()
}

@Singleton
class CommandParser @Inject constructor() {

    private val numberWords = mapOf(
        "один" to 1, "одного" to 1, "одну" to 1, "первый" to 1, "первого" to 1,
        "два" to 2, "две" to 2, "двух" to 2, "второй" to 2, "второго" to 2,
        "три" to 3, "трёх" to 3, "трех" to 3, "третий" to 3, "третьего" to 3,
        "четыре" to 4, "четырёх" to 4, "четырех" to 4, "четвёртый" to 4, "четвертый" to 4,
        "пять" to 5, "пяти" to 5, "пятый" to 5, "пятого" to 5,
        "шесть" to 6, "шести" to 6, "шестой" to 6, "шестого" to 6,
        "семь" to 7, "семи" to 7, "седьмой" to 7, "седьмого" to 7,
        "восемь" to 8, "восьми" to 8, "восьмой" to 8, "восьмого" to 8,
        "девять" to 9, "девяти" to 9, "девятый" to 9, "девятого" to 9,
        "десять" to 10, "десяти" to 10, "десятый" to 10, "десятого" to 10,
        "одиннадцать" to 11
    )

    private fun parseNumber(text: String): Int? {
        val num = text.toIntOrNull()
        if (num != null) return num
        return numberWords[text.lowercase()]
    }

    fun parse(text: String, playerNames: List<String> = emptyList()): VoiceCommand {
        val lower = text.lowercase().trim()
        Log.d("CommandParser", "Parsing: \"$lower\" with playerNames=$playerNames")

        if (lower.contains("новая игра") || lower.contains("начать игру") ||
            lower.contains("начни игру") || lower.contains("начинаем игру") ||
            lower.contains("старт игры"))
            return VoiceCommand.NewGame

        if (Regex(".*\\bотмена\\b.*").matches(lower) ||
            Regex(".*\\bотменить\\b.*").matches(lower) ||
            Regex(".*\\bотмени\\b.*").matches(lower) ||
            Regex(".*\\bundo\\b.*").matches(lower) ||
            Regex(".*\\bназад\\b.*").matches(lower))
            return VoiceCommand.Undo

        val endGameRegex = Regex("""конец игры\s+(\w+)\s+победил""")
        endGameRegex.find(lower)?.let { mr ->
            val name = findPlayerName(mr.groupValues[1], playerNames) ?: mr.groupValues[1]
            return VoiceCommand.EndGame(name)
        }
        if (lower.contains("конец игры") || lower.contains("завершить игру") ||
            lower.contains("завершить") || lower.contains("стоп игра"))
            return VoiceCommand.EndGame(null)

        // ПОВЫШЕНИЕ
        val plusResult = tryParse(lower, """(\w+)\s+плюс\s+(\w+)""", playerNames)
        if (plusResult != null) return VoiceCommand.LevelChange(plusResult.first, plusResult.second)

        val plusDigitResult = tryParse(lower, """(\w+)\s*\+\s*(\d+)""", playerNames)
        if (plusDigitResult != null) return VoiceCommand.LevelChange(plusDigitResult.first, plusDigitResult.second)

        val addResult = tryParse(lower, """добавь\s+(\w+)\s+(\w+)""", playerNames)
        if (addResult != null) return VoiceCommand.LevelChange(addResult.first, addResult.second)

        val povysResult = tryParse(lower, """повысь\s+(?:уровень\s+)?(\w+)(?:\s+уровень)?\s+на\s+(\w+)""", playerNames)
        if (povysResult != null) return VoiceCommand.LevelChange(povysResult.first, povysResult.second)

        val podnimiResult = tryParse(lower, """подними\s+(?:уровень\s+)?(\w+)\s+на\s+(\w+)""", playerNames)
        if (podnimiResult != null) return VoiceCommand.LevelChange(podnimiResult.first, podnimiResult.second)

        val nakinResult = tryParse(lower, """накинь\s+(\w+)\s+(\w+)""", playerNames)
        if (nakinResult != null) return VoiceCommand.LevelChange(nakinResult.first, nakinResult.second)

        val uvelichResult = tryParse(lower, """увеличь\s+(?:уровень\s+)?(\w+)\s+на\s+(\w+)""", playerNames)
        if (uvelichResult != null) return VoiceCommand.LevelChange(uvelichResult.first, uvelichResult.second)

        // ПОНИЖЕНИЕ
        val minusResult = tryParse(lower, """(\w+)\s+минус\s+(\w+)""", playerNames)
        if (minusResult != null) return VoiceCommand.LevelChange(minusResult.first, -minusResult.second)

        val minusDigitResult = tryParse(lower, """(\w+)\s*-\s*(\d+)""", playerNames)
        if (minusDigitResult != null) return VoiceCommand.LevelChange(minusDigitResult.first, -minusDigitResult.second)

        val snimiResult = tryParse(lower, """сними\s+(?:с\s+)?(\w+)\s+(\w+)""", playerNames)
        if (snimiResult != null) return VoiceCommand.LevelChange(snimiResult.first, -snimiResult.second)

        val uberiResult = tryParse(lower, """убери\s+(?:уровень\s+)?(?:у\s+)?(\w+)\s+(\w+)""", playerNames)
        if (uberiResult != null) return VoiceCommand.LevelChange(uberiResult.first, -uberiResult.second)

        val uberiOne = Regex("""убери\s+(?:уровень\s+)?(?:у\s+)?(\w+)$""").find(lower)
        if (uberiOne != null) {
            val name = findPlayerName(uberiOne.groupValues[1], playerNames) ?: uberiOne.groupValues[1]
            return VoiceCommand.LevelChange(name, -1)
        }

        val ponizResult = tryParse(lower, """понизь\s+(?:уровень\s+)?(\w+)(?:\s+уровень)?\s+на\s+(\w+)""", playerNames)
        if (ponizResult != null) return VoiceCommand.LevelChange(ponizResult.first, -ponizResult.second)

        val ponizDoResult = tryParse(lower, """понизь\s+(?:уровень\s+)?(\w+)\s+до\s+(\w+)""", playerNames)
        if (ponizDoResult != null) return VoiceCommand.SetLevel(ponizDoResult.first, ponizDoResult.second)

        val opustiResult = tryParse(lower, """опусти\s+(?:уровень\s+)?(\w+)\s+на\s+(\w+)""", playerNames)
        if (opustiResult != null) return VoiceCommand.LevelChange(opustiResult.first, -opustiResult.second)

        val vnizResult = tryParse(lower, """(\w+)\s+(?:уровень\s+)?вниз\s+на\s+(\w+)""", playerNames)
        if (vnizResult != null) return VoiceCommand.LevelChange(vnizResult.first, -vnizResult.second)

        val ubavResult = tryParse(lower, """убавь\s+(\w+)\s+(\w+)""", playerNames)
        if (ubavResult != null) return VoiceCommand.LevelChange(ubavResult.first, -ubavResult.second)

        val umenshiResult = tryParse(lower, """уменьши\s+(?:уровень\s+)?(\w+)\s+на\s+(\w+)""", playerNames)
        if (umenshiResult != null) return VoiceCommand.LevelChange(umenshiResult.first, -umenshiResult.second)

        // УСТАНОВКА
        val levelResult = tryParse(lower, """(\w+)\s+уровень\s+(\w+)""", playerNames)
        if (levelResult != null) return VoiceCommand.SetLevel(levelResult.first, levelResult.second)

        val postavResult = tryParse(lower, """поставь\s+(?:уровень\s+)?(\w+)\s+(?:уровень\s+)?(\w+)""", playerNames)
        if (postavResult != null) return VoiceCommand.SetLevel(postavResult.first, postavResult.second)

        val ustanovResult = tryParse(lower, """установи\s+(?:уровень\s+)?(\w+)(?:\s+уровень)?\s+(?:на\s+)?(\w+)""", playerNames)
        if (ustanovResult != null) return VoiceCommand.SetLevel(ustanovResult.first, ustanovResult.second)

        val stalResult = tryParse(lower, """(\w+)\s+стал\s+(?:уровень\s+)?(\w+)""", playerNames)
        if (stalResult != null) return VoiceCommand.SetLevel(stalResult.first, stalResult.second)

        val sdelaiResult = tryParse(lower, """сделай\s+(\w+)\s+(?:уровень\s+)?(\w+)""", playerNames)
        if (sdelaiResult != null) return VoiceCommand.SetLevel(sdelaiResult.first, sdelaiResult.second)

        return VoiceCommand.Unknown
    }

    private fun tryParse(text: String, pattern: String, playerNames: List<String>): Pair<String, Int>? {
        val match = Regex(pattern).find(text) ?: return null
        val rawName = match.groupValues[1]
        val name = if (playerNames.isNotEmpty()) {
            findPlayerName(rawName, playerNames) ?: rawName
        } else {
            rawName
        }
        val n = parseNumber(match.groupValues[2])
        Log.d("CommandParser", "tryParse: pattern=$pattern, rawName=$rawName, name=$name, n=$n")
        return if (n != null) name to n else null
    }

    private fun findPlayerName(text: String, playerNames: List<String>): String? {
        if (playerNames.isEmpty()) return null
        val normalizedText = normalizeWord(text)
        Log.d("CommandParser", "findPlayerName: text=\"$text\", normalized=\"$normalizedText\"")

        var bestMatch: String? = null
        var bestDistance = Int.MAX_VALUE

        for (playerName in playerNames) {
            val normalizedPlayer = normalizeWord(playerName)
            val distance = levenshteinDistance(normalizedText, normalizedPlayer)
            val isMatch = normalizedText.equals(normalizedPlayer, ignoreCase = true) ||
                    normalizedText.startsWith(normalizedPlayer, ignoreCase = true) ||
                    normalizedPlayer.startsWith(normalizedText, ignoreCase = true) ||
                    distance <= 2

            if (isMatch && distance < bestDistance) {
                bestMatch = playerName
                bestDistance = distance
            }
        }
        return bestMatch
    }

    private fun normalizeWord(word: String): String {
        return word.lowercase()
            .replace("ё", "е")
            .replace("й", "и")
            .replace("ь", "")
            .replace("ъ", "")
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }

    fun isHotword(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("эй манчкин") ||
                lower.contains("хей манчкин") ||
                lower.contains("манчкин")
    }
}