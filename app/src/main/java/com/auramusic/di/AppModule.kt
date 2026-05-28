package com.auramusic.di

import android.content.Context
import com.auramusic.data.local.database.AuraDatabase
import com.auramusic.data.local.dao.PlaylistDao
import com.auramusic.data.local.dao.SongDao
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.data.repository.MusicRepositoryImpl
import com.auramusic.domain.repository.MusicRepository
import com.auramusic.player.AuraColorManager
import com.auramusic.player.EqualizerManager
import com.auramusic.player.MusicPlayer
import com.auramusic.util.MusicScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuraDatabase {
        return AuraDatabase.getInstance(context)
    }

    @Provides
    fun provideSongDao(database: AuraDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    fun providePlaylistDao(database: AuraDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    @Singleton
    fun provideMusicRepository(
        songDao: SongDao,
        playlistDao: PlaylistDao
    ): MusicRepository {
        return MusicRepositoryImpl(songDao, playlistDao)
    }

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideMusicPlayer(
        @ApplicationContext context: Context,
        preferences: AppPreferences
    ): MusicPlayer {
        return MusicPlayer(context, preferences)
    }

    @Provides
    fun provideMusicScanner(@ApplicationContext context: Context): MusicScanner {
        return MusicScanner(context)
    }
}
