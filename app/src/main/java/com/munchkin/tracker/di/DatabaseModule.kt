package com.munchkin.tracker.di

import android.content.Context
import androidx.room.Room
import com.munchkin.tracker.data.MunchkinDatabase
import com.munchkin.tracker.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): MunchkinDatabase =
        Room.databaseBuilder(ctx, MunchkinDatabase::class.java, "munchkin.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePlayerDao(db: MunchkinDatabase): PlayerDao = db.playerDao()
    @Provides fun provideGameDao(db: MunchkinDatabase): GameDao = db.gameDao()
    @Provides fun provideGamePlayerDao(db: MunchkinDatabase): GamePlayerDao = db.gamePlayerDao()
    @Provides fun provideLevelChangeDao(db: MunchkinDatabase): LevelChangeDao = db.levelChangeDao()
    @Provides fun provideStatsDao(db: MunchkinDatabase): StatsDao = db.statsDao()
}