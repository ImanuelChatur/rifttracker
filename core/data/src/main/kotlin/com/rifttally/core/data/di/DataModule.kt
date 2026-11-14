package com.rifttally.core.data.di

import com.rifttally.core.data.repository.OfflineUserSettingsRepository
import com.rifttally.core.data.repository.UserSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindsUserSettingsRepository(impl: OfflineUserSettingsRepository): UserSettingsRepository
}
