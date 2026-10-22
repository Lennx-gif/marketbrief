package app.marketbrief.core.data.positioning

import kotlinx.coroutines.flow.Flow

/** Crowd positioning for one symbol: the share of Myfxbook traders long vs short. This is crowd data, not a price signal. */
data class CrowdPosition(
    val symbol: String,
    val longPercent: Double,
    val shortPercent: Double,
    val longVolume: Double,
    val shortVolume: Double
)

data class MyfxbookCredentials(val email: String, val password: String)

/** Stores the Myfxbook login. The implementation must keep the password encrypted at rest. */
interface CredentialStore {
    suspend fun get(): MyfxbookCredentials?
    suspend fun save(credentials: MyfxbookCredentials)
    suspend fun clear()
}

interface PositioningRepository {
    val connected: Flow<Boolean>
    val positions: Flow<List<CrowdPosition>>
    val lastUpdated: Flow<Long?>

    /** Reads the credential store so [connected] is accurate. Makes no network call. */
    suspend fun loadConnectionState()

    /** Logs in with the given credentials and stores them if the login works. */
    suspend fun connect(email: String, password: String): Result<Unit>
    suspend fun disconnect()

    /** Fetches fresh positions unless the cache is newer than the minimum interval. */
    suspend fun refresh(force: Boolean = false): Result<Unit>
}

sealed class PositioningError(message: String) : Exception(message) {
    class NotConnected : PositioningError("Connect your Myfxbook account in Settings.")
    class BadCredentials(detail: String) : PositioningError(detail.ifBlank { "Myfxbook rejected the login." })
    class SessionExpired : PositioningError("Myfxbook session expired.")
    class RateLimited : PositioningError("Myfxbook daily request limit reached.")
    class Server(detail: String) : PositioningError(detail.ifBlank { "Myfxbook request failed." })
}
