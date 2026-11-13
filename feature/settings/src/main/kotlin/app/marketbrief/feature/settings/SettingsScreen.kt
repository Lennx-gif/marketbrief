package app.marketbrief.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.ui.components.AppleGroupedSection
import app.marketbrief.core.ui.components.AppleHairlineDivider
import app.marketbrief.core.ui.components.ApplePillChip
import app.marketbrief.core.ui.components.LabeledSegmentedRow
import app.marketbrief.core.ui.components.SettingRow
import app.marketbrief.core.ui.theme.MarketBriefTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    versionName: String,
    versionCode: Int,
    flavorName: String,
    twelveDataKeyConfigured: Boolean,
    onNavigateToOnboarding: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = remember(context) { context.getSystemService(ClipboardManager::class.java) }
    var showMyfxbookDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by rememberSaveable { mutableStateOf(false) }
    var diagnosticsExpanded by rememberSaveable { mutableStateOf(false) }
    var debugInfoCopied by rememberSaveable { mutableStateOf(false) }
    var repositoryLinkUnavailable by rememberSaveable { mutableStateOf(false) }
    var digestTimeDraft by rememberSaveable(uiState.digestTime) { mutableStateOf(uiState.digestTime) }
    var quietHoursDraft by rememberSaveable(uiState.quietHours) { mutableStateOf(uiState.quietHours) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setHeadlinesNotif(true)
    }

    if (showMyfxbookDialog) {
        MyfxbookLoginDialog(
            onDismiss = { showMyfxbookDialog = false },
            onConnect = { email, password ->
                viewModel.connectMyfxbook(email, password)
                showMyfxbookDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Home Region
            item {
                SettingsSectionHeader(title = "FEED & REGION")
                AppleGroupedSection {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Home Trade Country",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Current: ${uiState.homeCountry}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        val countries = listOf("Kenya", "United States", "United Kingdom", "Eurozone", "Japan", "Germany", "South Africa")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(countries) { country ->
                                ApplePillChip(
                                    text = country,
                                    isSelected = country.equals(uiState.homeCountry, ignoreCase = true),
                                    onClick = { viewModel.setHomeCountry(country) }
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Kenya trade lens", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Adds one Kenya-focused NewsAPI request per home refresh", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = uiState.kenyaTradeLens, onCheckedChange = viewModel::setKenyaTradeLens)
                        }
                    }

                }
            }

            item {
                SettingsSectionHeader(title = "ONBOARDING & PREFERENCES")
                AppleGroupedSection {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Preferred Categories",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.preferredCategories.isEmpty()) "All categories prioritized" else uiState.preferredCategories.joinToString(", "),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onNavigateToOnboarding,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text("Manage Categories", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                            TextButton(onClick = {
                                viewModel.resetOnboarding()
                                onNavigateToOnboarding()
                            }) {
                                Text("Reset Onboarding", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionHeader(title = "CALCULATOR")
                AppleGroupedSection {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Account currency · ${uiState.accountCurrency}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("USD", "EUR", "GBP", "KES", "JPY", "CAD", "AUD", "CHF")) { currency ->
                                ApplePillChip(currency, currency == uiState.accountCurrency, onClick = { viewModel.setAccountCurrency(currency) })
                            }
                        }

                        if (showLicenseDialog) {
                            AlertDialog(
                                onDismissRequest = { showLicenseDialog = false },
                                title = { Text("Open-source licenses") },
                                text = {
                                    Column(
                                        modifier = Modifier
                                            .heightIn(max = 420.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        openSourceLicenseEntries.forEach { entry ->
                                            AboutBody(entry)
                                        }
                                        AboutBody("This list covers the principal direct application libraries. Transitive dependency notices are provided by their respective projects.")
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showLicenseDialog = false }) { Text("Close") }
                                }
                            )
                        }
                    }
                }
            }

            // Section 2: Daily Request Quota
            item {
                SettingsSectionHeader(title = "DAILY DIGEST")
                AppleGroupedSection {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Daily market digest", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("One scheduled summary each day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = uiState.headlinesNotif, onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else viewModel.setHeadlinesNotif(enabled)
                        })
                    }
                    AppleHairlineDivider()
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Time (Africa/Nairobi)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = digestTimeDraft, onValueChange = { digestTimeDraft = it }, label = { Text("HH:mm") }, modifier = Modifier.weight(1f), singleLine = true)
                            Button(onClick = { viewModel.setDigestTime(digestTimeDraft) }, enabled = digestTimeDraft.matches(Regex("([01]\\d|2[0-3]):[0-5]\\d"))) { Text("Save") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Home-country headlines only", fontSize = 14.sp)
                            Switch(checked = uiState.homeOnlyDigest, onCheckedChange = viewModel::setHomeOnlyDigest)
                        }
                        OutlinedTextField(value = quietHoursDraft, onValueChange = { quietHoursDraft = it }, label = { Text("Quiet hours (HH:mm-HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        TextButton(onClick = { viewModel.setQuietHours(quietHoursDraft) }) { Text("Save quiet hours") }
                    }
                }
            }

            item {
                SettingsSectionHeader(title = "NEWS SOURCES")
                AppleGroupedSection {
                    if (uiState.sources.isEmpty()) {
                        Text("Sources appear here after headlines load.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else uiState.sources.forEachIndexed { index, source ->
                        val current = uiState.sourcePreferences[source] ?: "NORMAL"
                        Row(Modifier.fillMaxWidth().clickable {
                            val next = when (current) { "NORMAL" -> "BOOST"; "BOOST" -> "MUTE"; else -> "NORMAL" }
                            viewModel.setSourcePreference(source, next)
                        }.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(source, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            ApplePillChip(text = current.lowercase().replaceFirstChar { it.uppercase() }, isSelected = current != "NORMAL", onClick = {
                                val next = when (current) { "NORMAL" -> "BOOST"; "BOOST" -> "MUTE"; else -> "NORMAL" }
                                viewModel.setSourcePreference(source, next)
                            })
                        }
                        if (index < uiState.sources.lastIndex) AppleHairlineDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }

            // Section 2: Daily Request Quota
            item {
                SettingsSectionHeader(title = "API QUOTA BUDGET")
                AppleGroupedSection {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "NewsAPI Free Quota",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${uiState.budgetUsed} / 100 used",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.budgetUsed > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (uiState.budgetUsed / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (uiState.budgetUsed > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MarketBriefTheme.colors.subtlePill
                        )

                        Text(
                            text = "NewsAPI developer plan allows 100 requests/day. The app automatically serves cached Room data when the daily budget is exhausted.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                        Text("Twelve Data: ${uiState.twelveDataRemaining} / 800 credits remaining", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Myfxbook: ${uiState.myfxbookRemaining} / 100 requests remaining", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Section 3: Analysis Preferences
            item {
                SettingsSectionHeader(title = "ANALYSIS VIEW")
                AppleGroupedSection {
                    LabeledSegmentedRow(
                        label = "Display Mode",
                        options = listOf("SENTIMENT", "BOTH"),
                        selected = uiState.analysisView,
                        onSelect = viewModel::setAnalysisView,
                        supportingText = "Choose whether analysis includes headline volume statistics.",
                        modifier = Modifier.padding(16.dp),
                        labelProvider = { if (it == "SENTIMENT") "Sentiment Only" else "Sentiment & Volume" }
                    )
                }
            }

            // Section 4: Offline Reading & Cache
            item {
                SettingsSectionHeader(title = "OFFLINE & STORAGE")
                AppleGroupedSection {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wi-Fi Only Downloads",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Save articles for offline reading only when on Wi-Fi",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.wifiOnly,
                            onCheckedChange = { viewModel.setWifiOnly(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    AppleHairlineDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearCachedNews() }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "Clear Cached News",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "${uiState.cachedArticleCount} articles saved on this device",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Crowd positioning account
            item {
                SettingsSectionHeader(title = "CROWD POSITIONING")
                AppleGroupedSection {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Myfxbook",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.myfxbookConnected) "Connected" else "Not connected",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                enabled = !uiState.myfxbookBusy,
                                onClick = {
                                    if (uiState.myfxbookConnected) viewModel.disconnectMyfxbook()
                                    else showMyfxbookDialog = true
                                }
                            ) { Text(if (uiState.myfxbookConnected) "Disconnect" else "Connect") }
                        }
                        uiState.myfxbookMessage?.let {
                            Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                        Text(
                            text = "Shows what other Myfxbook traders hold (long vs short). Your password is stored encrypted on this device and used only to sign in to Myfxbook.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 5: Appearance
            item {
                SettingsSectionHeader(title = "APPEARANCE")
                AppleGroupedSection {
                    LabeledSegmentedRow(
                        label = "Theme",
                        options = listOf("SYSTEM", "LIGHT", "DARK"),
                        selected = uiState.themeMode,
                        onSelect = viewModel::setThemeMode,
                        modifier = Modifier.padding(16.dp),
                        forceStacked = true,
                        labelProvider = {
                            when (it) {
                                "SYSTEM" -> "System"
                                "LIGHT" -> "Light"
                                else -> "Dark"
                            }
                        }
                    )
                }
            }

            item {
                SettingsSectionHeader(title = "FEED & READER")
                AppleGroupedSection {
                    SettingRow(
                        title = "Show market ticker on Feed",
                        subtitle = "Use cached quotes only; no quote request is started from a news card.",
                        trailingContent = {
                            Switch(
                                checked = uiState.showMarketTickerOnFeed,
                                onCheckedChange = viewModel::setShowMarketTickerOnFeed
                            )
                        }
                    )
                    AppleHairlineDivider()
                    SettingRow(
                        title = "Always use in-app page when full text fails",
                        subtitle = "Publisher pages may still contain ads or fail to load.",
                        trailingContent = {
                            Switch(
                                checked = uiState.inAppArticleFallback,
                                onCheckedChange = viewModel::setInAppArticleFallback
                            )
                        }
                    )
                }
            }

            item {
                SettingsSectionHeader(title = "ABOUT")
                AppleGroupedSection {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "MarketBrief",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = DebugInfoBuilder.formatVersion(versionName, versionCode, flavorName),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AboutHeading("What it is")
                        AboutBody("A financial news reader with in-app reading and market tools—sentiment, position sizing, and a watchlist—for forex-first traders.")

                        AboutHeading("Data sources and attribution")
                        AboutBody("NewsAPI.org supplies headlines and links. Publisher and author credits stay attached to each story; NewsAPI's Developer plan is limited to development and testing, and production use requires an eligible subscription.")
                        AboutBody("Twelve Data supplies market quotes. Quotes may be delayed and are not tick data. External display, redistribution, commercial use, and exchange data may require additional rights under your subscription.")
                        AboutBody("Myfxbook community positioning is shown only when connected. It describes other traders' positions and is not a forecast.")

                        AboutHeading("Disclaimers")
                        AboutBody("MarketBrief is not financial advice. Sentiment is a keyword heuristic. Crowd positioning describes other traders, not a prediction. In-app reading blocks a small CC0-1.0 host list and hides selected ad containers; ads can remain, publisher pages may break, and blocking may affect publishers.")

                        AboutHeading("Privacy")
                        AboutBody("There is no MarketBrief account. Saved stories, journal entries, and watchlist choices stay on this device. Requests to NewsAPI.org, Twelve Data, and Myfxbook send the data needed for those services; publisher hosts also receive requests for article pages and images. Myfxbook credentials are encrypted on this device and are sent to Myfxbook to authenticate.")

                        TextButton(onClick = { showLicenseDialog = true }) { Text("Open-source licenses") }
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Lennx-gif/marketbrief"))
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                                repositoryLinkUnavailable = false
                            } else {
                                repositoryLinkUnavailable = true
                            }
                        }) { Text("Project repository") }
                        if (repositoryLinkUnavailable) {
                            AboutBody("No browser is available to open the project repository.")
                        }

                        AppleHairlineDivider()
                        SettingRow(
                            title = "Diagnostics",
                            subtitle = "Provider budgets and last refresh; excludes credentials and article text.",
                            onClick = { diagnosticsExpanded = !diagnosticsExpanded },
                            trailingContent = { Text(if (diagnosticsExpanded) "Hide" else "Show", color = MaterialTheme.colorScheme.primary) }
                        )
                        if (diagnosticsExpanded) {
                            AboutBody("NewsAPI: ${(100 - uiState.budgetUsed).coerceAtLeast(0)} / 100 requests remaining")
                            AboutBody("Twelve Data: ${uiState.twelveDataRemaining} / 800 credits remaining · key ${if (twelveDataKeyConfigured) "configured" else "missing"}")
                            AboutBody("Myfxbook: ${uiState.myfxbookRemaining} / 100 requests remaining")
                            AboutBody("Last market refresh: ${formatRefreshTime(uiState.lastMarketRefreshEpochMillis)}")
                            Button(onClick = {
                                val text = DebugInfoBuilder.build(
                                    DebugInfoInput(
                                        versionName = versionName,
                                        versionCode = versionCode,
                                        flavor = flavorName,
                                        newsRequestsRemaining = (100 - uiState.budgetUsed).coerceAtLeast(0),
                                        twelveDataCreditsRemaining = uiState.twelveDataRemaining,
                                        myfxbookRequestsRemaining = uiState.myfxbookRemaining,
                                        lastMarketRefreshEpochMillis = uiState.lastMarketRefreshEpochMillis,
                                        cachedArticleCount = uiState.cachedArticleCount,
                                        twelveDataKeyConfigured = twelveDataKeyConfigured,
                                        myfxbookConnected = uiState.myfxbookConnected
                                    )
                                )
                                if (clipboard == null) {
                                    debugInfoCopied = false
                                } else {
                                    clipboard.setPrimaryClip(ClipData.newPlainText("MarketBrief debug info", text))
                                    debugInfoCopied = true
                                }
                            }) { Text(if (debugInfoCopied) "Debug info copied" else "Copy debug info") }
                            if (clipboard == null) {
                                AboutBody("Clipboard is unavailable on this device.")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun AboutHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
private fun AboutBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private val openSourceLicenseEntries = listOf(
    "AndroidX, Jetpack Compose, Room, WorkManager, and DataStore — Apache License 2.0",
    "Kotlin and kotlinx libraries — Apache License 2.0",
    "Dagger Hilt — Apache License 2.0",
    "Ktor — Apache License 2.0",
    "OkHttp and Okio — Apache License 2.0",
    "Coil — Apache License 2.0",
    "Jsoup — MIT License",
    "JUnit — Eclipse Public License 1.0",
    "MockK and Turbine — Apache License 2.0"
)

private fun formatRefreshTime(epochMillis: Long): String =
    if (epochMillis <= 0L) {
        "Not available"
    } else {
        java.text.DateFormat.getDateTimeInstance().format(java.util.Date(epochMillis))
    }

@Composable
private fun MyfxbookLoginDialog(onDismiss: () -> Unit, onConnect: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // not saveable on purpose: never written to saved state
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect Myfxbook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConnect(email, password) }) { Text("Connect") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
