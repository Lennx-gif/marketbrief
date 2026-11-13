package app.marketbrief.feature.news

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.model.Article
import app.marketbrief.feature.news.model.NewsUiState
import app.marketbrief.feature.news.ui.CategoryChipsRow
import app.marketbrief.feature.news.ui.CompactBreakingNewsCard
import app.marketbrief.feature.news.ui.HeroArticleCard
import androidx.compose.foundation.layout.Spacer
import app.marketbrief.feature.news.ui.HorizontalArticleRow
import app.marketbrief.feature.news.ui.MajorEventsTicker
import app.marketbrief.feature.news.ui.NewsViewModel
import app.marketbrief.feature.news.ui.ProfileHeaderSection
import app.marketbrief.feature.news.ui.SectionHeader
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onOpenArticle: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenCalculator: (String) -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onScrollChanged: (Float) -> Unit = { },
    onBottomBarVisibilityChanged: (Boolean) -> Unit = { },
    initialCategory: String? = null,
    viewModel: NewsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialCategory) {
        if (!initialCategory.isNullOrBlank()) {
            viewModel.selectCategory(initialCategory)
        }
    }

    // Scroll state for LazyColumn
    val listState = rememberLazyListState()

    // Track scroll for static top bar glassmorphism and dynamic bottom bar hiding/revealing
    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousScrollOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }.collect { (currentIndex, currentOffset, isScrolling) ->
            // Report scroll position for static TopNavBar glassmorphism depth
            val topScroll = if (currentIndex == 0) {
                currentOffset.toFloat()
            } else {
                100f
            }
            onScrollChanged(topScroll)

            // Dynamic bottom bar visibility:
            // When at the very top of the list, always show the bottom bar
            if (currentIndex == 0 && currentOffset <= 15) {
                onBottomBarVisibilityChanged(true)
            } else if (isScrolling) {
                // Determine scroll direction
                val delta = if (currentIndex != previousIndex) {
                    (currentIndex - previousIndex) * 200 + (currentOffset - previousScrollOffset)
                } else {
                    currentOffset - previousScrollOffset
                }

                // Apply deadzone/jitter threshold (10px) to prevent flickering
                if (delta > 10) {
                    // Scrolling DOWN -> fade away bottom bar
                    onBottomBarVisibilityChanged(false)
                } else if (delta < -10) {
                    // Scrolling UP -> reveal bottom bar
                    onBottomBarVisibilityChanged(true)
                }
            }

            previousIndex = currentIndex
            previousScrollOffset = currentOffset
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NewsViewModel.NewsEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is NewsViewModel.NewsEvent.OpenArticle -> onOpenArticle(event.articleId)
            }
        }
    }

    when (val state = uiState) {
        is NewsUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is NewsUiState.Success -> {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Profile Header
                    item {
                        ProfileHeaderSection(
                            userName = state.userName,
                            onSettingsClick = onOpenSettings
                        )
                    }

                    // 2. Major Events Ticker (auto-scrolling like bank teller)
                    if (state.breakingNews.isNotEmpty()) {
                        item {
                            MajorEventsTicker(
                                events = state.breakingNews.map { article ->
                                    app.marketbrief.feature.news.ui.MajorEvent(
                                        id = article.id,
                                        category = article.categories.firstOrNull() ?: "News",
                                        title = article.title,
                                        impact = (article.sentiment ?: 0f * 5).coerceIn(-5f..5f),
                                        symbols = article.relatedSymbols.take(4),
                                        imageUrl = article.imageUrl
                                    )
                                }
                            )
                        }
                    }

                    // 3. Category Filter Chips
                    item {
                        CategoryChipsRow(
                            categories = state.categories,
                            selectedCategory = state.selectedCategory,
                            onCategorySelected = viewModel::onCategorySelected
                        )
                    }

                    // 4. Hero Article - "Today trending"
                    state.heroArticle?.let { hero ->
                        item {
                            SectionHeader(title = "Today trending 🔥")
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                            HeroArticleCard(
                                article = hero,
                                isBookmarked = state.bookmarkedArticleIds.contains(hero.id),
                                onClick = { onOpenArticle(hero.id) },
                                onBookmarkClick = { viewModel.toggleBookmark(hero.id) }
                            )
                        }
                    }

                    // 5. Latest News - Vertical List
                    if (state.latestNews.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Latest news", actionLabel = "View More")
                        }
                        items(state.latestNews, key = { it.id }) { article ->
                            HorizontalArticleRow(
                                article = article,
                                isBookmarked = state.bookmarkedArticleIds.contains(article.id),
                                onClick = { onOpenArticle(article.id) },
                                onBookmarkClick = { viewModel.toggleBookmark(article.id) }
                            )
                        }
                    }

                    // Empty state handling
                    if (state.heroArticle == null && state.breakingNews.isEmpty() && state.latestNews.isEmpty()) {
                        item {
                            EmptyFeedPlaceholder(
                                isSavedView = state.selectedCategory == "Saved",
                                onResetFilter = { viewModel.onCategorySelected("Trending") }
                            )
                        }
                    }
                }
            }
        }
        is NewsUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Text(
                        text = state.message,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    androidx.compose.material3.Button(onClick = { viewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedPlaceholder(
    isSavedView: Boolean,
    onResetFilter: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isSavedView) "No Bookmarked Articles" else "No Headlines Found",
                fontSize = 17.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isSavedView)
                    "Tap the bookmark icon on any article to save it for later reading."
                else
                    "Try adjusting your category filter to view more news.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!isSavedView) {
                androidx.compose.material3.TextButton(onClick = onResetFilter) {
                    Text("View All News")
                }
            }
        }
    }
}