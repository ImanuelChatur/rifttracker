package com.rifttally.core.data.legend

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface LegendModule {
    @Binds
    fun bindsLegendRepository(impl: AssetLegendRepository): LegendRepository

    @Binds
    fun bindsLegendAssetSource(impl: AndroidLegendAssetSource): LegendAssetSource

    @Binds
    fun bindsLegendLogger(impl: AndroidLegendLogger): LegendLogger
}
