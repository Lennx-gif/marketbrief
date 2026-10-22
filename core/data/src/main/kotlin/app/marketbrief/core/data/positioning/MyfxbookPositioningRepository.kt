package app.marketbrief.core.data.positioning

import app.marketbrief.core.data.remote.MyfxbookClient
import app.marketbrief.core.data.remote.RequestBudget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps one Myfxbook session in memory, logs in again when it expires (once per refresh),
 * and caches results so the screen can open freely without spending the 100-per-day allowance.
 */
class MyfxbookPositioningRepository(
    private val api: MyfxbookClient,
    private val store: CredentialStore,
    private val minIntervalMs: Long = 15 * 60_000L,
    private val budget: RequestBudget = RequestBudget(perMinute = 10, perDay = 100),
    private val clock: () -> Long = System::currentTimeMillis
) : PositioningRepository {

    private val lock = Mutex()
    private var session: String? = null
    private val _connected = MutableStateFlow<Boolean?>(null)
    private val _positions = MutableStateFlow<List<CrowdPosition>>(emptyList())
    private val _updated = MutableStateFlow<Long?>(null)

    /** Null until the credential store has been read, then true or false. Treated as false for display. */
    override val connected: Flow<Boolean> = _connected.asStateFlow().map { it == true }
    override val positions: Flow<List<CrowdPosition>> = _positions.asStateFlow()
    override val lastUpdated: Flow<Long?> = _updated.asStateFlow()

    override suspend fun loadConnectionState() {
        _connected.value = store.get() != null
    }

    override suspend fun connect(email: String, password: String): Result<Unit> = lock.withLock {
        if (!budget.tryAcquire()) return Result.failure(PositioningError.RateLimited())
        api.login(email.trim(), password).map { s ->
            session = s
            store.save(MyfxbookCredentials(email.trim(), password))
            _connected.value = true
        }
    }

    override suspend fun disconnect() = lock.withLock {
        session?.let { api.logout(it) }
        session = null
        store.clear()
        _positions.value = emptyList()
        _updated.value = null
        _connected.value = false
    }

    override suspend fun refresh(force: Boolean): Result<Unit> = lock.withLock {
        val creds = store.get()
        _connected.value = creds != null
        if (creds == null) return Result.failure(PositioningError.NotConnected())

        val last = _updated.value
        if (!force && last != null && clock() - last < minIntervalMs) return Result.success(Unit)

        var result = fetch(creds)
        if (result.exceptionOrNull() is PositioningError.SessionExpired) {
            session = null
            result = fetch(creds) // silent re-login, one retry only
        }
        result.map { list ->
            _positions.value = list
            _updated.value = clock()
        }
    }

    private suspend fun fetch(creds: MyfxbookCredentials): Result<List<CrowdPosition>> {
        var s = session
        if (s == null) {
            if (!budget.tryAcquire()) return Result.failure(PositioningError.RateLimited())
            val login = api.login(creds.email, creds.password)
            s = login.getOrElse { return Result.failure(it) }
            session = s
        }
        if (!budget.tryAcquire()) return Result.failure(PositioningError.RateLimited())
        return api.communityOutlook(s)
    }
}
