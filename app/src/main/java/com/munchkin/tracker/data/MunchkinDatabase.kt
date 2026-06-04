package com.munchkin.tracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.munchkin.tracker.data.dao.*
import com.munchkin.tracker.data.entity.*

@Database(
    entities = [
        PlayerEntity::class,
        GameEntity::class,
        GamePlayerEntity::class,
        LevelChangeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MunchkinDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun gamePlayerDao(): GamePlayerDao
    abstract fun levelChangeDao(): LevelChangeDao
}
