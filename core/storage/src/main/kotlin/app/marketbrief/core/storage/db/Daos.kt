package app.marketbrief.core.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: String): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE id NOT IN (SELECT articleId FROM bookmarks) AND id NOT IN (SELECT articleId FROM downloads) AND fetchedAt < :cutoff")
    suspend fun pruneOldArticles(cutoff: Long)

    @Query("DELETE FROM articles WHERE id NOT IN (SELECT articleId FROM bookmarks) AND id NOT IN (SELECT articleId FROM downloads)")
    suspend fun clearUnsaved()

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY publishedAt DESC")
    suspend fun search(query: String): List<ArticleEntity>

    @Query("SELECT COUNT(*) FROM articles")
    fun observeCount(): Flow<Int>
}

@Dao
interface BookmarkDao {

    @Query("SELECT articleId FROM bookmarks ORDER BY savedAt DESC")
    fun observeBookmarkIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE articleId = :articleId")
    suspend fun unbookmark(articleId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE articleId = :articleId)")
    suspend fun isBookmarked(articleId: String): Boolean
}

@Dao
interface DownloadDao {

    @Query("SELECT articleId FROM downloads ORDER BY downloadedAt DESC")
    fun observeDownloadedIds(): Flow<List<String>>

    @Query("SELECT * FROM downloads WHERE articleId = :articleId")
    suspend fun getDownload(articleId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE articleId = :articleId")
    suspend fun deleteDownload(articleId: String)
}
