package com.motonav.di

import android.content.Context
import com.motonav.data.local.db.MotoNavDatabase
import com.motonav.data.local.db.PoiDao
import com.motonav.data.local.db.RideDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideMotoNavDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): MotoNavDatabase {
        val db = MotoNavDatabase.buildDatabase(context, scope)
        scope.launch(Dispatchers.IO) {
            MotoNavDatabase.ensureInitialPoisSeeded(context, db)
        }
        return db
    }

    @Provides
    @Singleton
    fun providePoiDao(database: MotoNavDatabase): PoiDao {
        return database.poiDao()
    }

    @Provides
    @Singleton
    fun provideRideDao(database: MotoNavDatabase): RideDao {
        return database.rideDao()
    }
}
