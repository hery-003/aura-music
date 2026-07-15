package com.auramusic.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.auramusic.data.local.dao.PlaylistDao
import com.auramusic.data.local.dao.SongDao
import com.auramusic.data.local.entity.PlaylistEntity
import com.auramusic.data.local.entity.PlaylistSongEntity
import com.auramusic.data.local.entity.SongEntity
import timber.log.Timber

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getInstance(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AuraDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AuraDatabase::class.java,
                "aura_music.db"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(false)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_artist ON songs(artist)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_album ON songs(album)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_album_id ON songs(album_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_genre ON songs(genre)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_path ON songs(path)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_is_favorite ON songs(is_favorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_play_count ON songs(play_count)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_last_played ON songs(last_played)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_date_added ON songs(date_added)")
                Timber.i("Database migrated from v1 to v2: added indices")
            }
        }

    }
}
