package com.rifttally.core.data.di

import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.game.OfflineGameRepository
import com.rifttally.core.data.preset.OfflinePlayerPresetRepository
import com.rifttally.core.data.preset.PlayerPresetRepository
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.game.RandomSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Singleton
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
        /**
         * The kernel's cryptographic RNG (seeded from hardware entropy): as close to true random as an
         * offline app gets.
         */
        @Provides
        @Singleton
        fun providesRandomSource(): RandomSource {
            val random = SecureRandom()
            return RandomSource(random::nextInt)
        }

        @Provides
        fun providesIdGenerator(): IdGenerator = IdGenerator.RandomUuid

        @OptIn(ExperimentalTime::class)
        @Provides
        fun providesGameEventFactory(clock: Clock, idGenerator: IdGenerator): GameEventFactory =
            GameEventFactory(clock, idGenerator)
    }
}
