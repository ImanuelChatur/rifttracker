package com.rifttally.core.database.di

import android.content.Context
import androidx.room.Room
import com.rifttally.core.common.dispatchers.IoDispatcher
import com.rifttally.core.database.RiftDatabase
import com.rifttally.core.database.dao.GameDao
import com.rifttally.core.database.dao.GameEventDao
import com.rifttally.core.database.dao.PlayerPresetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesRiftDatabase(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RiftDatabase = Room.databaseBuilder(context, RiftDatabase::class.java, RiftDatabase.NAME)
        .setQueryCoroutineContext(ioDispatcher)
        .build()

    @Provides
    fun providesGameDao(database: RiftDatabase): GameDao = database.gameDao()

    @Provides
    fun providesGameEventDao(database: RiftDatabase): GameEventDao = database.gameEventDao()

    @Provides
    fun providesPlayerPresetDao(database: RiftDatabase): PlayerPresetDao = database.playerPresetDao()
}
