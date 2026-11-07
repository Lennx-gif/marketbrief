package app.marketbrief.feature.news

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.model.Category
import app.marketbrief.core.model.CountryFilter
import app.marketbrief.core.ui.components.AppleBanner
import app.marketbrief.core.ui.components.AppleGroupedSection
import app.marketbrief.core.ui.components.ApplePillChip
import app.marketbrief.core.ui.components.FilterChipRow
import app.marketbrief.core.ui.theme.MarketBriefTheme
import app.marketbrief.feature.news.ui.ArticleCard
import app.marketbrief.feature.news.ui.FeedAction
import app.marketbrief.feature.news.ui.FeedEvent
import app.marketbrief.feature.news.ui.SectionHeader
import app.marketbrief.feature.news.ui.SourceStrip
import app.marketbrief.feature.news.ui.TrendingHeroCard
import app.marketbrief.feature.news.ui.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onNavigateToCalculator: (String) -> Unit,
    onOpenArticle: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: NewsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by remember { mutableStateOf(false) }
    var categoryRowVisible by remember { mutableStateOf(true) }
    val categoryScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y < -1f) categoryRowVisible = false
                if (consumed.y > 1f) categoryRowVisible = true
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FeedEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is FeedEvent.OpenCalculator -> onNavigateToCalculator(event.symbol)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FeedHeader(
                homeCountry = uiState.homeCountryName,
                countryFilter = uiState.country,
                onOpenFilters = { showFilters = true },
                onOpenSettings = onOpenSettings
            )

            OutlinedTextField(
                value = uiState.feedSearchText,
                onValueChange = viewModel::onFeedSearchChanged,
                placeholder = { Text("Search articles...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            AnimatedVisibility(visible = categoryRowVisible) {
                CategoryFilterBar(
                    selectedCategories = uiState.selectedCategories,
                    onCategoryClick = { viewModel.onAction(FeedAction.ToggleCategory(it)) },
                    onFilterClick = { showFilters = true }
                )
            }

            if (Category.Saved in uiState.selectedCategories) {
                OutlinedTextField(
                    value = uiState.savedSearchText,
                    onValueChange = viewModel::onSavedSearchChanged,
                    label = { Text("Search saved articles and downloaded text") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    singleLine = true
                )
                if (uiState.savedTags.isNotEmpty()) {
                    LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            ApplePillChip("All tags", uiState.selectedSavedTag == null, onClick = { viewModel.onSavedTagSelected(null) })
                        }
                        items(uiState.savedTags) { tag ->
                            ApplePillChip(tag, uiState.selectedSavedTag == tag, onClick = { viewModel.onSavedTagSelected(if (uiState.selectedSavedTag == tag) null else tag) })
                        }
                    }
                }
            }

            if (uiState.usdKesRate != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("USD / KES", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("${String.format(java.util.Locale.US, "%.2f", uiState.usdKesRate)} KES", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Non-blocking Banner (Rate Limit / Offline status)
            uiState.banner?.let { banner ->
                AppleBanner(
                    message = banner.message,
                    isWarning = banner.isWarning,
                    actionLabel = banner.actionLabel,
                    onAction = { viewModel.onAction(FeedAction.Refresh) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Feed Content
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.onAction(FeedAction.Refresh) },
                modifier = Modifier.fillMaxSize().nestedScroll(categoryScrollConnection)
            ) {
                if (uiState.articles.isEmpty()) {
                    EmptyFeedPlaceholder(
                        isSavedView = Category.Saved in uiState.selectedCategories,
                        onResetFilter = { viewModel.onAction(FeedAction.ToggleCategory(Category.All)) }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val hero = uiState.articles.first()
                        val rest = uiState.articles.drop(1)
                        val shareArticle: (app.marketbrief.core.model.Article) -> Unit = { article ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, article.title)
                                putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.url}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Article"))
                        }

                        if (uiState.showSourceStrip) {
                            item(key = "sources") {
                                SourceStrip(sources = uiState.articles.map { it.article.sourceName }.distinct().take(12))
                            }
                        }
                        if (uiState.showMarketTicker) {
                            item(key = "cached-market-ticker") {
                                FeedMarketTicker(
                                    quotes = uiState.articles.mapNotNull { it.relatedQuote }
                                        .distinctBy { it.assetId }
                                )
                            }
                        }
                        item(key = "featured-header") { SectionHeader(title = "Featured News", actionLabel = "View More") }
                        item(key = "hero") {
                            TrendingHeroCard(
                                article = hero.article,
                                isBookmarked = hero.bookmarked,
                                onClick = { onOpenArticle(hero.article.id) },
                                onBookmarkToggle = { viewModel.onAction(FeedAction.ToggleBookmark(hero.article)) }
                            )
                        }
                        if (rest.isNotEmpty()) {
                            item(key = "breaking-header") { SectionHeader(title = "Breaking News", actionLabel = "View More") }
                        }
                        items(
                            items = rest,
                            key = { it.article.id }
                        ) { item ->
                            ArticleCard(
                                article = item.article,
                                isBookmarked = item.bookmarked,
                                isDownloaded = item.downloaded,
                                relatedQuote = item.relatedQuote,
                                onClick = { onOpenArticle(item.article.id) },
                                onBookmarkToggle = { viewModel.onAction(FeedAction.ToggleBookmark(item.article)) },
                                onDownload = { viewModel.onAction(FeedAction.Download(item.article.id)) },
                                onSizeTrade = { symbol -> onNavigateToCalculator(symbol) },
                                onShare = { shareArticle(item.article) }
                            )
                        }
                    }

                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            FeedFilterSheet(
                homeCountry = uiState.homeCountryName,
                countryFilter = uiState.country,
                showSourceStrip = uiState.showSourceStrip,
                onCountryFilterSelected = { viewModel.onAction(FeedAction.SetCountry(it)) },
                onSourceStripChanged = { viewModel.onAction(FeedAction.SetSourceStrip(it)) }
            )
        }
    }
}

@Composable
private fun FeedMarketTicker(quotes: List<app.marketbrief.core.model.Quote>) {
    if (quotes.isEmpty()) return
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quotes, key = { it.assetId.toString() }) { quote ->
            val change = quote.changePercent
            val up = change != null && change >= 0.0
            val stale = System.currentTimeMillis() - quote.timestamp > 5 * 60_000L
            Surface(shape = RoundedCornerShape(100.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(quote.assetId.symbol, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(java.math.BigDecimal.valueOf(quote.mid).stripTrailingZeros().toPlainString(), fontSize = 11.sp)
                    change?.let {
                        Text(
                            "${if (up) "↑" else "↓"} ${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(it))}%",
                            fontSize = 11.sp,
                            color = if (stale) MaterialTheme.colorScheme.onSurfaceVariant else if (up) MarketBriefTheme.colors.bullish else MarketBriefTheme.colors.bearish
                        )
                    }
                    if (stale) Text("stale", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun FeedHeader(
    homeCountry: String,
    countryFilter: CountryFilter,
    onOpenFilters: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Breaking News",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Financial News Page",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (countryFilter) {
                        is CountryFilter.Global -> "Global"
                        is CountryFilter.Home -> homeCountry
                        is CountryFilter.Only -> countryFilter.isoCodes.joinToString(", ")
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onOpenFilters) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Feed filters",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (countryFilter !is CountryFilter.Global) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).size(7.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary
                            ) {}
                        }
                    }
                }

                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

    }
}

@Composable
fun CategoryFilterBar(
    selectedCategories: Set<Category>,
    onCategoryClick: (Category) -> Unit,
    onFilterClick: () -> Unit
) {
    val categories = listOf(
        Category.Saved,
        Category.All,
        Category.Trade,
        Category.Finance,
        Category.Forex,
        Category.Tech,
        Category.Politics,
        Category.Stocks,
        Category.Crypto,
        Category.Commodities
    )

    FilterChipRow(
        items = categories,
        selectedItem = null,
        selectedItems = selectedCategories,
        onItemSelected = onCategoryClick,
        modifier = Modifier.padding(vertical = 6.dp),
        labelProvider = { if (it == Category.All) "Trending" else it.label },
        trailingAction = {
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Default.Tune, contentDescription = "Region and source filters")
            }
        }
    )
}

@Composable
private fun FeedFilterSheet(
    homeCountry: String,
    countryFilter: CountryFilter,
    showSourceStrip: Boolean,
    onCountryFilterSelected: (CountryFilter) -> Unit,
    onSourceStripChanged: (Boolean) -> Unit
) {
    val countries = listOf(homeCountry, "United States", "United Kingdom", "Eurozone", "Japan", "South Africa")
    val selectedCountries = (countryFilter as? CountryFilter.Only)?.isoCodes.orEmpty()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Region", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ApplePillChip("Global", countryFilter is CountryFilter.Global, { onCountryFilterSelected(CountryFilter.Global) })
            ApplePillChip("Home", countryFilter is CountryFilter.Home, { onCountryFilterSelected(CountryFilter.Home) })
        }
        Text("Choose countries", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(countries) { country ->
                val selected = country in selectedCountries
                ApplePillChip(country, selected, {
                    val next = if (selected) selectedCountries - country else selectedCountries + country
                    onCountryFilterSelected(if (next.isEmpty()) CountryFilter.Global else CountryFilter.Only(next))
                })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show source strip", style = MaterialTheme.typography.bodyMedium)
                Text("Keep it with the scrolling stories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = showSourceStrip, onCheckedChange = onSourceStripChanged)
        }
    }
}

@Composable
fun EmptyFeedPlaceholder(
    isSavedView: Boolean,
    onResetFilter: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AppleGroupedSection {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isSavedView) "No Bookmarked Articles" else "No Headlines Found",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isSavedView) "Tap the bookmark icon on any article to save it for offline reading." else "Try adjusting your category or country filters to view more news.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (isSavedView) {
                    Spacer(modifier = Modifier.height(6.dp))
                    ApplePillChip(
                        text = "View All News",
                        isSelected = true,
                        onClick = onResetFilter
                    )
                }
            }
        }
    }
}
