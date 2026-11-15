package com.rifttally.core.data.di

import com.rifttally.core.data.timer.OfflineRoundTimerRepository
import com.rifttally.core.data.timer.RoundTimerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TimerModule {
    @Binds
    fun bindsRoundTimerRepository(impl: OfflineRoundTimerRepository): RoundTimerRepository
}
