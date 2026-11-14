package com.rifttally.core.data.di

import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.game.OfflineGameRepository
import com.rifttally.core.data.preset.OfflinePlayerPresetRepository
import com.rifttally.core.data.preset.PlayerPresetRepository
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.IdGenerator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Module
@InstallIn(SingletonComponent::class)
internal interface GameModule {
    @Binds
    fun bindsGameRepository(impl: OfflineGameRepository): GameRepository

    @Binds
    fun bindsPlayerPresetRepository(impl: OfflinePlayerPresetRepository): PlayerPresetRepository

    companion object {
        @Provides
        fun providesIdGenerator(): IdGenerator = IdGenerator.RandomUuid

        @OptIn(ExperimentalTime::class)
        @Provides
        fun providesGameEventFactory(clock: Clock, idGenerator: IdGenerator): GameEventFactory =
            GameEventFactory(clock, idGenerator)
    }
}
