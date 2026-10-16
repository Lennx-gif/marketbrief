package app.marketbrief.feature.news

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.marketbrief.core.model.Category
import app.marketbrief.core.model.CountryFilter
import app.marketbrief.core.ui.components.AppleBanner
import app.marketbrief.core.ui.components.AppleGroupedSection
import app.marketbrief.core.ui.components.ApplePillChip
import app.marketbrief.core.ui.components.AppleSegmentedControl
import app.marketbrief.core.ui.theme.MarketBriefTheme
import app.marketbrief.feature.news.ui.ArticleCard
import app.marketbrief.feature.news.ui.FeedAction
import app.marketbrief.feature.news.ui.FeedEvent
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
            // Apple Minimalist Header
            FeedHeader(
                homeCountry = uiState.homeCountryName,
                countryFilter = uiState.country,
                isSavedFilterActive = Category.Saved in uiState.selectedCategories,
                onCountryFilterSelected = { viewModel.onAction(FeedAction.SetCountry(it)) },
                onSavedToggle = { viewModel.onAction(FeedAction.ToggleCategory(Category.Saved)) },
                onOpenSettings = onOpenSettings
            )

            // Horizontally Scrollable Category Filter Row
            CategoryFilterBar(
                selectedCategories = uiState.selectedCategories,
                onCategoryClick = { viewModel.onAction(FeedAction.ToggleCategory(it)) }
            )

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
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.articles.isEmpty() && !uiState.isRefreshing) {
                    EmptyFeedPlaceholder(
                        isSavedView = Category.Saved in uiState.selectedCategories,
                        onResetFilter = { viewModel.onAction(FeedAction.ToggleCategory(Category.All)) }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.articles,
                            key = { it.article.id }
                        ) { item ->
                            ArticleCard(
                                article = item.article,
                                isBookmarked = item.bookmarked,
                                isDownloaded = item.downloaded,
                                onClick = { onOpenArticle(item.article.id) },
                                onBookmarkToggle = { viewModel.onAction(FeedAction.ToggleBookmark(item.article)) },
                                onDownload = { viewModel.onAction(FeedAction.Download(item.article.id)) },
                                onSizeTrade = { symbol -> onNavigateToCalculator(symbol) },
                                onShare = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, item.article.title)
                                        putExtra(Intent.EXTRA_TEXT, "${item.article.title}\n${item.article.url}")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Article"))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedHeader(
    homeCountry: String,
    countryFilter: CountryFilter,
    isSavedFilterActive: Boolean,
    onCountryFilterSelected: (CountryFilter) -> Unit,
    onSavedToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MarketBrief",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (countryFilter is CountryFilter.Home) "$homeCountry Trade & Macro" else "Global Financial Intelligence",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onSavedToggle) {
                    Icon(
                        imageVector = if (isSavedFilterActive) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Saved Articles",
                        tint = if (isSavedFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

        // Home / Global Segment Control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleSegmentedControl(
                items = listOf(CountryFilter.Home, CountryFilter.Global),
                selectedItem = countryFilter,
                onItemSelected = onCountryFilterSelected,
                labelProvider = { if (it is CountryFilter.Home) homeCountry else "Global" }
            )
        }
    }
}

@Composable
fun CategoryFilterBar(
    selectedCategories: Set<Category>,
    onCategoryClick: (Category) -> Unit
) {
    val categories = listOf(
        Category.All,
        Category.Saved,
        Category.Trade,
        Category.Finance,
        Category.Forex,
        Category.Tech,
        Category.Politics,
        Category.Stocks,
        Category.Crypto,
        Category.Commodities
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            ApplePillChip(
                text = category.label,
                isSelected = category in selectedCategories,
                onClick = { onCategoryClick(category) }
            )
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