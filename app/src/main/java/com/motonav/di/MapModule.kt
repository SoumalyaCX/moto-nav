package com.motonav.di

import android.content.Context
import com.motonav.data.local.MapFileManager
import com.motonav.data.local.PreferencesStore
import com.motonav.map.MapStyleManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapModule {

    @Provides
    @Singleton
    fun provideMapFileManager(
        @ApplicationContext context: Context
    ): MapFileManager {
        return MapFileManager(context)
    }

    @Provides
    @Singleton
    fun provideMapStyleManager(
        @ApplicationContext context: Context,
        mapFileManager: MapFileManager,
        preferencesStore: PreferencesStore
    ): MapStyleManager {
        return MapStyleManager(context, mapFileManager, preferencesStore)
    }
}
