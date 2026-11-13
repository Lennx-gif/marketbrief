# Build Fix and Dynamic Bottom Navigation Report

**Status:** Completed and Verified.

## Build Review and Resolution

1. **`:feature:news:compileDebugKotlin`**:
   - Initial failure: `MajorEventsTicker.kt:125:14: Unresolved reference 'height'`.
   - Resolution: Added missing `import androidx.compose.foundation.layout.height`.
   - Verified: Kotlin compilation succeeded cleanly.

2. **`:feature:calculator:compileDebugKotlin`**:
   - Initial failure: `CalculatorScreen.kt:224:13: Unresolved reference 'Spacer'`.
   - Resolution: Added missing `import androidx.compose.foundation.layout.Spacer`.
   - Verified: Kotlin compilation succeeded cleanly.

3. **Compose Dependency Resolution**:
   - Explicit foundation and animation dependencies (`compose-foundation`, `compose-animation`) resolved correctly without BOM conflict.

4. **Unit Tests**:
   - Command: `./gradlew :feature:news:testDebugUnitTest`
   - Result: **BUILD SUCCESSFUL** (all unit tests passing).

5. **Debug APK Assembly**:
   - Command: `./gradlew :app:assemblePersonalDebug`
   - Result: **BUILD SUCCESSFUL** (233 actionable tasks, 0 errors).
   - Artifact: `app/build/outputs/apk/personal/debug/app-personal-debug.apk` (23.1 MB).

---

## Dynamic Glassmorphic Bottom Pill Implementation

### Implemented Behaviors
- **Destination Scope**: Bottom bar is visible on top-level tabs (`news`, `analysis`, `calculator`, `watchlist`) and concealed on child screens (`article/{id}`, `settings`, `journal`).
- **Scroll Tracking**: Driven by `NewsScreen`'s `snapshotFlow` with deadzone/jitter filtering (+/-10px) to prevent flickering. Downward scroll translates the pill downward and fades it out; upward scroll or returning to top restores it.
- **Route State Reset**: Navigating between destinations resets visibility to ensure the bar is immediately accessible.
- **Physics Transitions**: Smooth alpha and translation transitions powered by `animateFloatAsState` with physics springs (`dampingRatio = 0.85f`, `stiffness = 400f`).
- **Content Clearance**: Added bottom clearance (`88.dp` - `100.dp`) across `NewsScreen`, `AnalysisScreen`, `CalculatorScreen`, and `WatchlistScreen` so that no list item or action button is ever obscured by the floating pill.

### Visual Styling
- Inset pill shape with capsule geometry (`CircleShape`).
- Frosted surface depth using `MarketBriefDesignTokens.GlassSurface` and hairline outline `MarketBriefDesignTokens.GlassBorder`.
- WhatsApp-inspired active highlight pill behind selected icon with turquoise accent typography (`MarketBriefDesignTokens.TurquoiseAccent`).
