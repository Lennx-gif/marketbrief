package app.marketbrief.core.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ArticleEntity::class,
        BookmarkEntity::class,
        DownloadEntity::class,
        ArticleBodyEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MarketBriefDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadDao(): DownloadDao
    abstract fun articleBodyDao(): ArticleBodyDao
}
