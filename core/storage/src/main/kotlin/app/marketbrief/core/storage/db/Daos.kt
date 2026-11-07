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

    @Query("SELECT DISTINCT sourceName FROM articles ORDER BY sourceName")
    fun observeSourceNames(): Flow<List<String>>

    @Query("SELECT * FROM articles WHERE publishedAt >= :cutoff AND relatedSymbols LIKE '%' || :symbol || '%' ORDER BY publishedAt DESC")
    suspend fun recentRelated(symbol: String, cutoff: Long): List<ArticleEntity>
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

@Dao
interface ArticleBodyDao {

    @Query("SELECT * FROM article_body WHERE articleId = :articleId")
    fun observeBody(articleId: String): Flow<ArticleBodyEntity?>

    @Query("SELECT * FROM article_body WHERE articleId = :articleId")
    suspend fun getBody(articleId: String): ArticleBodyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBody(body: ArticleBodyEntity)

    @Query("DELETE FROM article_body WHERE articleId = :articleId")
    suspend fun deleteBody(articleId: String)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC") fun observeEntries(): Flow<List<JournalEntryEntity>>
    @Query("SELECT * FROM journal_entries WHERE id = :id") suspend fun getEntry(id: String): JournalEntryEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertEntry(entry: JournalEntryEntity)
    @Query("DELETE FROM journal_entries WHERE id = :id") suspend fun deleteEntry(id: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addHeadlines(items: List<JournalHeadlineEntity>)
    @Query("SELECT * FROM journal_headlines ORDER BY entryId")
    fun observeHeadlines(): Flow<List<JournalHeadlineEntity>>
    @Query("SELECT * FROM journal_headlines WHERE entryId = :entryId") suspend fun headlines(entryId: String): List<JournalHeadlineEntity>
    @Query("DELETE FROM journal_headlines WHERE entryId = :entryId")
    suspend fun deleteHeadlines(entryId: String)
    @Query("SELECT * FROM journal_entries WHERE status = 'CLOSED' ORDER BY closedAt") suspend fun closedEntries(): List<JournalEntryEntity>
    @Query("SELECT * FROM journal_entries WHERE status = 'OPEN'") suspend fun openEntries(): List<JournalEntryEntity>
    @Query("SELECT * FROM account_settings WHERE id = 1") fun observeAccount(): Flow<AccountSettingsEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAccount(settings: AccountSettingsEntity)
}

@Dao
interface JournalAccountDao {
    @Query("SELECT * FROM journal_accounts ORDER BY createdAt")
    fun observeAll(): Flow<List<JournalAccountEntity>>

    @Query("SELECT * FROM journal_accounts WHERE archived = 0 ORDER BY createdAt")
    fun observeActive(): Flow<List<JournalAccountEntity>>

    @Query("SELECT * FROM journal_accounts WHERE id = :id")
    suspend fun get(id: Long): JournalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: JournalAccountEntity): Long

    @Query("UPDATE journal_accounts SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Dao
interface TradeSetupDao {
    @Query("SELECT * FROM trade_setups ORDER BY name")
    fun observeAll(): Flow<List<TradeSetupEntity>>

    @Query("SELECT * FROM trade_setups WHERE archived = 0 ORDER BY name")
    fun observeActive(): Flow<List<TradeSetupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setup: TradeSetupEntity)

    @Query("UPDATE trade_setups SET archived = 1 WHERE id = :id")
    suspend fun archive(id: String)
}

@Dao
interface DailyEntryDao {
    @Query("SELECT * FROM daily_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<DailyEntryEntity>>

    @Query("SELECT * FROM daily_entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DailyEntryEntity)

    @Query("DELETE FROM daily_entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface JournalAttachmentDao {
    @Query("SELECT * FROM journal_attachments ORDER BY createdAt")
    fun observeAll(): Flow<List<JournalAttachmentEntity>>

    @Query("SELECT * FROM journal_attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt")
    suspend fun forOwner(ownerType: String, ownerId: String): List<JournalAttachmentEntity>

    @Query("SELECT * FROM journal_attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt")
    fun observeForOwner(ownerType: String, ownerId: String): Flow<List<JournalAttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(attachment: JournalAttachmentEntity)

    @Query("SELECT COUNT(*) FROM journal_attachments WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun countForOwner(ownerType: String, ownerId: String): Int

    @Query("DELETE FROM journal_attachments WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY instant") fun observeEvents(): Flow<List<CalendarEventEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(events: List<CalendarEventEntity>)
    @Query("DELETE FROM calendar_events WHERE source = 'USER'") suspend fun clearUserEvents()
}

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts WHERE triggeredAt IS NULL") fun observeActive(): Flow<List<PriceAlertEntity>>
    @Query("SELECT * FROM price_alerts WHERE triggeredAt IS NULL") suspend fun active(): List<PriceAlertEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(alert: PriceAlertEntity)
    @Query("DELETE FROM price_alerts WHERE id = :id") suspend fun delete(id: String)
}

@Dao
interface ArticleSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun indexArticles(items: List<ArticleFtsEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun indexBody(item: ArticleBodyFtsEntity)
    @Query("SELECT articleId FROM article_fts WHERE article_fts MATCH :query") suspend fun searchArticles(query: String): List<String>
    @Query("SELECT articleId FROM article_body_fts WHERE article_body_fts MATCH :query") suspend fun searchBodies(query: String): List<String>
    @Query("DELETE FROM article_fts WHERE articleId = :id") suspend fun removeArticle(id: String)
    @Query("DELETE FROM article_body_fts WHERE articleId = :id") suspend fun removeBody(id: String)
}

@Dao
interface SourcePrefDao {
    @Query("SELECT * FROM source_preferences") fun observeAll(): Flow<List<SourcePrefEntity>>
    @Query("SELECT * FROM source_preferences") suspend fun all(): List<SourcePrefEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun set(pref: SourcePrefEntity)
    @Query("DELETE FROM source_preferences WHERE sourceName = :name") suspend fun clear(name: String)
}

@Dao
interface ArticleTagDao {
    @Query("SELECT tag FROM article_tags WHERE articleId = :articleId ORDER BY tag") fun observeTags(articleId: String): Flow<List<String>>
    @Query("SELECT DISTINCT article_tags.tag FROM article_tags INNER JOIN bookmarks ON bookmarks.articleId = article_tags.articleId ORDER BY article_tags.tag") fun observeAllTags(): Flow<List<String>>
    @Query("SELECT articleId FROM article_tags WHERE tag = :tag") suspend fun articleIdsForTag(tag: String): List<String>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun add(tag: ArticleTagEntity)
    @Query("DELETE FROM article_tags WHERE articleId = :articleId AND tag = :tag") suspend fun remove(articleId: String, tag: String)
}

@Dao
interface RateCacheDao {
    @Query("SELECT * FROM rate_cache") fun observeAll(): Flow<List<RateCacheEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(rates: List<RateCacheEntity>)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_items ORDER BY position, addedAt")
    fun observeAll(): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist_items ORDER BY position, addedAt")
    suspend fun all(): List<WatchlistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<WatchlistItemEntity>)

    @Query("DELETE FROM watchlist_items WHERE symbol = :symbol")
    suspend fun delete(symbol: String)

    @Query("DELETE FROM watchlist_items")
    suspend fun clear()
}

@Dao
interface PresetDao {

    @Query("SELECT * FROM presets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: PresetEntity): Long

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TradeLogDao {

    @Query("SELECT * FROM trade_log ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TradeLogEntity>>

    @Insert
    suspend fun insert(entry: TradeLogEntity)

    @Query("DELETE FROM trade_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM trade_log")
    suspend fun clearAll()
}
