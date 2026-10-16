# MarketBrief - Android App for Traders & Tech Enthusiasts

A Kotlin-native Android app built with Jetpack Compose, Material 3, and a clean multi-module architecture. Designed for personal use with a structure that scales to a commercial product.

## Architecture

```
:app                      # Navigation, DI (Hilt), WorkManager, flavors (personal/store)
:feature:news             # Feed, article detail, saved, search
:feature:calculator       # Position size calculator with ViewModel
:feature:analysis         # Asset detail, indicators, screener
:feature:watchlist        # Watchlist and economic calendar
:core:ui                  # Theme and shared composables
:core:data                # Room, HTTP clients, repositories, providers
:core:model               # Article, AssetId, CurrencyCode, Candle, Quote (pure Kotlin)
:core:risk                # Position sizing, instruments, FX rates (pure Kotlin)
:core:analysis            # SMA, EMA, RSI, ATR, MACD, MarketSnapshot (pure Kotlin)
:core:entitlements        # Plans, features, limits, gates (pure Kotlin)
```

**Dependency Rule**: Features depend on `core:*`, never on each other. `core:model` depends on nothing. `core:risk` and `core:analysis` depend only on `core:model`. Only `:app` wires things together.

## Features

### Core Modules (Pure Kotlin - No Android Dependencies)
- **core:model** - Domain models: AssetId, CurrencyCode, Candle, Quote, Timeframe
- **core:risk** - Position sizing with 35 unit tests, instrument catalog, FX rate conversion
- **core:analysis** - Technical indicators: SMA, EMA, RSI, ATR, MACD, Bollinger Bands, Stochastic, Pivot Points
- **core:entitlements** - Plan system (FREE/PRO/PERSONAL), feature gates, limits
- **core:data** - NewsProvider, MarketDataSource, RateRepository interfaces
- **core:ui** - Material 3 theme, shared composables

### Feature Modules
- **feature:news** - Tech/Finance tabs, category chips, article cards, save/search, "Size Trade" shortcut
- **feature:calculator** - Instrument picker, balance, risk %/amount, long/short, entry/stop/TP, leverage, rate override
- **feature:analysis** - Asset detail, timeframe selector, MarketSnapshot, indicators, chart placeholder
- **feature:watchlist** - Watchlist with price, economic calendar, local notifications ready

### App Module
- Navigation with bottom bar
- Hilt DI setup
- Personal/Store flavors
- WorkManager for background tasks

## Getting Started

1. **Prerequisites**
   - Android Studio Koala or later
   - JDK 17
   - Android SDK 34

2. **Setup**
   ```bash
   # Copy local properties and add your API keys
   cp local.properties.example local.properties
   # Edit local.properties with your API keys
   ```

3. **Build**
   ```bash
   ./gradlew assemblePersonalDebug
   ```

4. **Run Tests**
   ```bash
   ./gradlew test
   ```

## API Keys (local.properties)

The app calls providers directly. Add your keys to `local.properties`:

```properties
FINNHUB_API_KEY=your_key
MARKETAUX_API_KEY=your_key
TWELVE_DATA_API_KEY=your_key
ALPHA_VANTAGE_API_KEY=your_key
```

## Flavor System

- **personal** - Everything unlocked, no billing code (Plan.PERSONAL)
- **store** - Ready for Play Billing integration (Plan.PRO/FREE)

## Extension Points

| To Add | Do This | Core Change |
|--------|---------|-------------|
| New asset class | Create `Instrument`, register with `InstrumentCatalog.withInstrument` | None |
| New price source | Implement `MarketDataSource` in `:core:data` | None |
| New FX rate source | Implement `RateSource` or build `PairRateTable` | None |
| New indicator | Add function to `Indicators`, field to `MarketSnapshot` | Small |
| New gated feature | Add to `Feature`, assign in `Plans`, check `gate()` | Small |
| Change free/paid | Edit `Plans.default` | None |

## Known Limits

- **Sizer**: No spread, commission, swap, slippage. Margin is an estimate.
- **Analysis**: Descriptive readings, not signals. Small differences from charting platforms possible.
- **Entitlements**: Client-side only. Real enforcement needs backend.

## Roadmap

See `basePlan.md` and `scalingPlan.md` for detailed backlog.

### M0: Setup ✓
### M1: Calculator MVP (in progress)
### M2: Data Layer
### M3: News UI
### M4: Analysis Feature
### M5: Polish
### M6: Watchlist & Alerts
### M7: Productize (if charging)

## License

Personal use. Commercial use requires data licenses, backend, and legal review.