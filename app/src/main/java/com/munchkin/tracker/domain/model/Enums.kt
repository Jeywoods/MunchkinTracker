package com.munchkin.tracker.domain.model

enum class Gender(val label: String, val icon: String) {
    MALE("Мужской", "♂"),
    FEMALE("Женский", "♀")
}

enum class VoiceState {
    SLEEPING, ACTIVE, RECORDING, ERROR
}

enum class LevelChangeSource(val label: String) {
    MANUAL("Кнопка"),
    VOICE("Голос"),
    UNDO("Отмена")
}

enum class PlayerClass(val label: String) {
    WARRIOR("Воин"),
    WIZARD("Волшебник"),
    THIEF("Вор"),
    CLERIC("Клирик")
}

enum class PlayerRace(val label: String) {
    HUMAN("Человек"),
    ELF("Эльф"),
    DWARF("Дварф"),
    HALFLING("Хафлинг")
}