package com.munchkin.tracker.domain.model

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