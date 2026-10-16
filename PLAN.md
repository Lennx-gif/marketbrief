# MarketBrief: plan and backlog

Android app for tech and finance news with a position size calculator. Kotlin native, personal use, forex first, structured so other asset classes slot in without rewrites.

## 1. Decisions

| Area | Decision |
|---|---|
| Platform | Android, Kotlin, Jetpack Compose, Material 3 |
| Audience | Just you, so no accounts, no analytics, no billing |
| Backend | None at first. The app calls providers directly. API keys live in `local.properties` and reach the app through `BuildConfig`. This is fine for a personal build and is not safe if you ever publish the APK |
| Notifications | WorkManager polling plus local notifications, not FCM. No server needed |
| Assets | Forex only in the UI for now. The calculator core is already asset-agnostic |

A backend can be added later behind the `NewsProvider` and `RateSource` interfaces without touching the UI.

## 2. Architecture

```
:app                  navigation, DI (Hilt), manifest, WorkManager scheduling
:feature:news         feed, article detail, saved, search
:feature:calculator   calculator screen and ViewModel
:feature:watchlist    watchlist and economic calendar (v1.5)
:core:ui              theme and shared composables
:core:data            Room, HTTP clients, repositories, news providers, FX price fetch
:core:model           Article, Source, Tag (pure Kotlin)
:core:risk            DONE. Pure Kotlin position sizing, no Android dependencies
```

Dependency rule: features depend on `core:*`, never on each other. `core:risk` and `core:model` depend on nothing. Only `:app` wires things together.

Suggested libraries (swap freely): Compose BOM, Navigation Compose, Room, Hilt, Ktor or Retrofit with kotlinx.serialization, WorkManager, DataStore, Coil.

**Data flow**
- News: provider (RSS, Finnhub, Marketaux) -> `NewsRepository` (dedupe, tag) -> Room -> ViewModel -> UI
- Sizing: `RateRepository` fetches pair prices -> `PairRateTable` -> `PositionSizer`. The core never touches the network

**Calculator ViewModel rules**
- UI state holds the raw text fields. Parse to `BigDecimal` at the edge and never use `Double` for money or prices
- Map each `Rejection` to a human message. `BelowMinimumLot` should show what the minimum lot would risk
- Format numbers in the UI with `toPlainString()`. The core returns rounded values but does not format

## 3. Extension points

| To add | Do this | Core change |
|---|---|---|
| New asset (gold, an index, crypto) | Create an `Instrument` and register it with `InstrumentCatalog.withInstrument` | None |
| Live FX rates | Implement `RateSource`, or build a `PairRateTable` from fetched prices | None |
| New news source | Implement `NewsProvider` in `:core:data` | None |
| Prop-firm style rules | Add a layer above `PositionSizer` that caps risk by remaining daily loss or drawdown | None |

For a new asset, confirm contract size, pip size, lot step and min/max lot against your broker's specification. The built-in `XAUUSD` entry uses common defaults and may not match your broker. Unknown symbols return null from the catalog on purpose, so `BTCUSD` is never silently treated as forex.

**Known limits of the sizer (by design, for now)**
- No spread, commission, swap or slippage. If you want the stop to include spread, widen the stop you enter
- Margin is an estimate from `units * entry * rate / leverage`. Brokers differ, especially for metals and indices
- Currency conversion uses one hop through a pivot (USD first). That covers every standard forex account and pair

## 4. Backlog

### M0: Project setup (about 0.5 day)
- [ ] Open the project in Android Studio and run `gradle wrapper` to generate the wrapper
- [ ] Bump `kotlin` in `gradle/libs.versions.toml` to the current stable version and run `./gradlew :core:risk:test`
- [ ] Add the Android modules from section 2 as you reach them

### M1: Calculator MVP (about 1 week)
- [x] `core:risk`: instrument model, rate table, position sizer, 35 unit tests
- [ ] `:feature:calculator` screen: instrument picker, balance, account currency, risk % or fixed amount, long/short toggle, entry, stop (price or pips), optional take profit and leverage
  - Done when: three sample trades from `PositionSizerTest` produce identical results in the UI, and invalid input shows a message without crashing
- [ ] Results card: lots, units, risk (target and actual), pip value, stop pips, reward-to-risk, potential profit, margin, warnings
- [ ] Rate handling: when the account currency differs from the quote currency, show the rate used with a timestamp and let you override it manually
- [ ] Persist defaults (account currency, balance, risk %, last instrument) in DataStore
- [ ] Presets and a trade-calculation log in Room

### M2: Data layer (about 1 week)
- [ ] `NewsProvider` interface plus an RSS implementation for tech feeds
- [ ] Finance provider (Finnhub, Marketaux or similar). Verify free-tier limits and terms before committing
- [ ] Dedupe by normalised URL and title hash. Cache in Room. Refresh policy: show cached, refresh in background, 15 minute minimum interval
- [ ] `RateRepository` that fetches the pair prices the calculator needs and exposes a `PairRateTable` with an "as of" time
- [ ] Offline, rate-limited and partial-failure handling, covered by tests with a fake provider

### M3: News UI (1 to 2 weeks)
- [ ] Home with Tech and Finance tabs, category chips (Forex, Stocks, Crypto, Macro, AI, Startups), article cards
- [ ] Article detail: headline, snippet, source, open original in a Custom Tab, save for later
- [ ] Saved and search screens
- [ ] "Size a trade" shortcut from an article that opens the calculator with the tagged instrument filled in
- [ ] Dark theme and offline reading of the cached feed

### M4: Polish and daily use (about 1 week)
- [ ] Empty, loading and error states, accessibility pass, performance check on a low-end device
- [ ] Export and import of presets and the trade log

### M5: v1.5
- [ ] Watchlist with instrument tagging, so the feed filters to pairs you follow (for example USDJPY, USDCHF)
- [ ] Economic calendar with high-impact event alerts via WorkManager and local notifications
- [ ] Account-rules guard: daily loss limit, max drawdown, risk budget remaining today, applied on top of the sizer

### Later
- [ ] Stocks and crypto in the calculator UI (the core already supports them)
- [ ] AI summaries and sentiment tags
- [ ] Optional backend proxy if you ever need hidden keys or server push

## 5. Verification status

- `core:risk` compiles and all 35 tests pass. This sandbox cannot reach Maven Central or the Gradle plugin portal, so the tests were run by compiling with the Kotlin 2.0.21 compiler bundled in Gradle and running JUnit 4.13.2 directly.
- Expected values were checked independently with exact rational arithmetic in Python.
- A rounding bug in this kind of calculator is easy to miss: inverting a rate like 1/150 can make an exact 0.30 lots compute as 0.2999…9 and floor to 0.29. A regression test covers it, and removing the fix makes that test fail.
- Not verified: the Gradle build itself (plugin resolution, `jvmToolchain(17)`, the pinned Kotlin version). If the first build complains, bump the Kotlin version and make sure a JDK 17 is installed.

## 6. Questions that need your broker's specifications

- Lot step, minimum lot and maximum lot for each pair you trade
- Whether you want spread added to the stop distance automatically
- Contract size and pip definition for any non-forex symbol you add
