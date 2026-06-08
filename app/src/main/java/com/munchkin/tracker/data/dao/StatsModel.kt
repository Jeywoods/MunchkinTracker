package com.munchkin.tracker.data.dao

data class GenderWinCount(val gender: String, val wins: Int)
data class GenderCount(val gender: String, val count: Int)
data class PlayerWinCount(val id: Long, val name: String, val gender: String, val wins: Int)
data class ClassWinCount(val class1: String?, val class2: String?, val wins: Int)
data class RaceWinCount(val race1: String?, val race2: String?, val wins: Int)
data class ClassPopularityCount(val class1: String?, val class2: String?, val games_count: Int)
data class RacePopularityCount(val race1: String?, val race2: String?, val games_count: Int)
data class ClassEfficiencyCount(val class1: String?, val class2: String?, val total_games: Int, val wins: Int)
data class RaceEfficiencyCount(val race1: String?, val race2: String?, val total_games: Int, val wins: Int)
data class ClassRaceComboCount(val class1: String?, val class2: String?, val race1: String?, val race2: String?, val wins: Int)
data class GameDurationRaw(val avg_duration: Double?, val total_games: Int, val min_duration: Long?, val max_duration: Long?)