package app.marketbrief.feature.news.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.ArticleBody
import app.marketbrief.core.storage.db.ArticleDao
import app.marketbrief.core.storage.db.BookmarkDao
import app.marketbrief.core.storage.db.DownloadDao
import app.marketbrief.core.ui.components.AppleBanner
import app.marketbrief.core.ui.components.AppleHairlineDivider
import app.marketbrief.core.ui.theme.MarketBriefTheme
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String,
    articleDao: ArticleDao,
    bookmarkDao: BookmarkDao,
    downloadDao: DownloadDao,
    newsRepository: NewsRepository,
    onBack: () -> Unit,
    onSizeTrade: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var article by remember { mutableStateOf<Article?>(null) }
    var articleBody by remember { mutableStateOf<ArticleBody?>(null) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(false) }
    var isLoadingBody by remember { mutableStateOf(false) }
    var isDownloadingOffline by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            article?.let { art ->
                try {
                    context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                        val mdExport = buildMarkdownExport(art, articleBody)
                        outputStream.write(mdExport.toByteArray())
                    }
                } catch (_: Exception) { }
            }
        }
    }

    LaunchedEffect(articleId) {
        val entity = articleDao.getById(articleId)
        article = entity?.toDomain()
        isBookmarked = bookmarkDao.isBookmarked(articleId)
        isDownloaded = downloadDao.getDownload(articleId) != null

        // Load or extract article body
        val cached = newsRepository.getArticleBody(articleId)
        if (cached != null) {
            articleBody = cached
        } else {
            isLoadingBody = true
            val res = newsRepository.extractArticleBody(articleId)
            articleBody = res.getOrNull()
            isLoadingBody = false
        }
    }

    val currentArticle = article

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentArticle?.sourceName.orEmpty(),
                        fontSize = 16.sp,
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
                actions = {
                    currentArticle?.let { art ->
                        // Export .md
                        IconButton(onClick = {
                            val slug = art.title.lowercase()
                                .replace(Regex("[^a-z0-9]+"), "-")
                                .trim('-')
                                .take(40)
                            exportLauncher.launch("$slug.md")
                        }) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export Markdown",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Share
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, art.title)
                                putExtra(Intent.EXTRA_TEXT, "${art.title}\n${art.url}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Article"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Bookmark
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val nextState = !isBookmarked
                                newsRepository.setBookmarked(art.id, nextState)
                                isBookmarked = nextState
                            }
                        }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Open in Chrome Custom Tabs
                        IconButton(onClick = {
                            val customTabsIntent = CustomTabsIntent.Builder().build()
                            customTabsIntent.launchUrl(context, Uri.parse(art.url))
                        }) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "View original",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (currentArticle == null) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Full-bleed header image, like the reference
                currentArticle.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Headline first, then the byline row
                Text(
                    text = currentArticle.title,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    lineHeight = 33.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Publisher & Meta Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentArticle.sourceName.firstOrNull()?.uppercase() ?: "N",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column {
                            Text(
                                text = currentArticle.sourceName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatDateTime(currentArticle.publishedAt),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    currentArticle.sentiment?.let { score ->
                        val isBullish = score >= 0.15f
                        val isBearish = score <= -0.15f
                        val sentimentColor = when {
                            isBullish -> MarketBriefTheme.colors.bullish
                            isBearish -> MarketBriefTheme.colors.bearish
                            else -> MarketBriefTheme.colors.neutral
                        }
                        val label = when {
                            isBullish -> "Bullish"
                            isBearish -> "Bearish"
                            else -> "Neutral"
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = sentimentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = sentimentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                AppleHairlineDivider()

                // Body Section
                if (isLoadingBody) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Extracting full story for reader mode...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val body = articleBody
                    if (body != null) {
                        val reason = body.failureReason
                        if (body.kind != "FULL" && reason != null) {
                            AppleBanner(
                                message = "Full text unavailable (${reason.lowercase()}). Showing summary preview.",
                                isWarning = false,
                                actionLabel = "Open on Web",
                                onAction = {
                                    val customTabsIntent = CustomTabsIntent.Builder().build()
                                    customTabsIntent.launchUrl(context, Uri.parse(currentArticle.url))
                                }
                            )
                        }

                        MarkdownArticleBody(markdown = body.markdown)
                    } else {
                        Text(
                            text = currentArticle.summary,
                            fontSize = 16.sp,
                            lineHeight = 25.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons: Offline Save & Position Sizing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isDownloaded && !isDownloadingOffline) {
                                isDownloadingOffline = true
                                coroutineScope.launch {
                                    newsRepository.download(currentArticle.id)
                                    isDownloaded = true
                                    isDownloadingOffline = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDownloaded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        if (isDownloadingOffline) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (isDownloaded) "Downloaded" else "Download Offline",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    val symbol = currentArticle.relatedSymbols.firstOrNull() ?: "EURUSD"
                    Button(
                        onClick = { onSizeTrade(symbol) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "Size $symbol",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun MarkdownArticleBody(markdown: String) {
    val lines = markdown.split("\n")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var inList = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### "),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## "),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    // Top level title already shown in screen header
                    continue
                }
                trimmed.startsWith("> ") -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = trimmed.removePrefix("> "),
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("•", fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = trimmed.substring(2),
                            fontSize = 15.sp,
                            lineHeight = 23.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                trimmed.startsWith("![") && trimmed.contains("](") -> {
                    val url = trimmed.substringAfter("](").substringBefore(")")
                    val alt = trimmed.substringAfter("![").substringBefore("]")
                    if (url.isNotBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = alt.ifBlank { null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                else -> {
                    // Standard body paragraph
                    Text(
                        text = cleanInlineFormatting(trimmed),
                        fontSize = 16.sp,
                        lineHeight = 25.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun cleanInlineFormatting(text: String): String {
    return text
        .replace("**", "")
        .replace("*", "")
        .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")
}

private fun buildMarkdownExport(article: Article, body: ArticleBody?): String = buildString {
    appendLine("---")
    appendLine("title: \"${article.title.replace("\"", "\\\"")}\"")
    appendLine("source: ${article.sourceName}")
    appendLine("url: ${article.url}")
    appendLine("published: ${article.publishedAt}")
    appendLine("saved: ${System.currentTimeMillis()}")
    appendLine("---")
    appendLine()
    if (body?.kind != "FULL") {
        appendLine("> Preview only: full text was unavailable from the publisher.")
        appendLine()
    }
    append(body?.markdown ?: article.summary)
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
