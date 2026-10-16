package app.marketbrief.feature.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.feature.news.ui.ArticleCard
import app.marketbrief.feature.news.ui.NewsViewModel

@Composable
fun NewsScreen(
    onNavigateToCalculator: (String) -> Unit,
    viewModel: NewsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabRow(
            selectedTab = uiState.selectedCategory,
            onTabClick = { viewModel.onCategorySelected(it) },
            tabs = listOf("Tech", "Finance", "Forex", "Stocks", "Crypto", "Macro", "AI", "Startups")
        )

        if (uiState.isLoading && uiState.articles.isEmpty()) {
            LoadingIndicator()
        } else if (uiState.error != null) {
            ErrorMessage(message = uiState.error.orEmpty(), onRetry = { viewModel.refresh() })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.articles) { article ->
                    ArticleCard(
                        article = article,
                        onClick = { },
                        onSave = { viewModel.saveArticle(article) },
                        onSizeTrade = { symbol -> onNavigateToCalculator(symbol) }
                    )
                }
            }
        }
    }
}

@Composable
fun TabRow(
    selectedTab: String,
    onTabClick: (String) -> Unit,
    tabs: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            TextButton(
                onClick = { onTabClick(tab) },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = tab,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onRetry) {
                Text("Retry", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}