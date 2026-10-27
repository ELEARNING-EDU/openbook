package com.example.epubreader.di

import android.content.Context
import com.example.epubreader.library.LibraryStore
import com.example.epubreader.library.ReadingLogStore
import com.example.epubreader.reader.BookmarkStore
import com.example.epubreader.reader.HighlightStore
import com.example.epubreader.reader.ReaderSettings
import com.example.epubreader.reader.ReadingPositions
import com.example.epubreader.reader.SavedPlaces
import com.example.epubreader.settings.AppLanguageStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The app's stores, one instance each: they all read and write the same private
 * preferences, so a second copy could disagree with the first.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun library(@ApplicationContext context: Context): LibraryStore = LibraryStore(context)

    @Provides
    @Singleton
    fun settings(@ApplicationContext context: Context): ReaderSettings = ReaderSettings(context)

    @Provides
    @Singleton
    fun positions(@ApplicationContext context: Context): ReadingPositions = ReadingPositions(context)

    @Provides
    @Singleton
    fun bookmarks(@ApplicationContext context: Context): BookmarkStore = BookmarkStore(context)

    @Provides
    @Singleton
    fun savedPlaces(positions: ReadingPositions): SavedPlaces = positions

    @Provides
    @Singleton
    fun highlights(@ApplicationContext context: Context): HighlightStore = HighlightStore(context)

    @Provides
    @Singleton
    fun language(@ApplicationContext context: Context): AppLanguageStore = AppLanguageStore(context)

    @Provides
    @Singleton
    fun readingLog(@ApplicationContext context: Context): ReadingLogStore = ReadingLogStore(context)
}
