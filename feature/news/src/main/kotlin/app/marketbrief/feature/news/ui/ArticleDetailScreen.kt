package app.marketbrief.feature.news.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.ArticleBody
import app.marketbrief.core.ui.components.AppleBanner
import app.marketbrief.core.ui.theme.MarketBriefTheme
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArticleDetailScreen(
    articleId: String,
    newsRepository: NewsRepository,
    onBack: () -> Unit,
    onSizeTrade: (String) -> Unit,
    inAppFallbackEnabled: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var article by remember { mutableStateOf<Article?>(null) }
    var articleBody by remember { mutableStateOf<ArticleBody?>(null) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isLoadingBody by remember { mutableStateOf(false) }
    var extractionFailed by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }

    // In-app browser state
    var isReaderView by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableIntStateOf(0) }
    var isWebLoading by remember { mutableStateOf(true) }
    var canGoBackInWeb by remember { mutableStateOf(false) }

    BackHandler(enabled = !isReaderView && canGoBackInWeb && webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    fun openInExternalBrowser(url: String) {
        val intent = CustomTabsIntent.Builder().build()
        try {
            intent.launchUrl(context, Uri.parse(url))
        } catch (_: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }

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
        article = null
        articleBody = null
        extractionFailed = false
        isLoadingBody = true
        article = newsRepository.getArticleById(articleId)
        isBookmarked = newsRepository.isBookmarked(articleId)

        // Load or extract article body
        val cached = newsRepository.getArticleBody(articleId)
        if (cached != null) {
            articleBody = cached
        } else {
            val res = newsRepository.extractArticleBody(articleId)
            articleBody = res.getOrNull()
            extractionFailed = res.isFailure
        }
        isLoadingBody = false
    }

    val currentArticle = article
    val hostName = remember(currentArticle?.url) {
        currentArticle?.url?.let {
            try {
                Uri.parse(it).host?.removePrefix("www.")
            } catch (_: Exception) {
                null
            }
        } ?: currentArticle?.sourceName.orEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F12))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Twitter/X-Style Top Browser Chrome Header
            Surface(
                color = Color(0xF40B0F12),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Close / Back button (48dp target)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Centered Domain / URL Pill showing secure host & refresh action
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF161B1E),
                        border = BorderStroke(0.5.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = Color(0xFF5CE1E6),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = hostName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isReaderView) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            webViewInstance?.reload()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = Color(0xFF8E8E93),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Overflow Menu (48dp target)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { overflowExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )

                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isReaderView) "Show live webpage" else "Show reader view") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isReaderView) Icons.Default.Language else Icons.Default.AutoStories,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    overflowExpanded = false
                                    isReaderView = !isReaderView
                                }
                            )
                            currentArticle?.let { art ->
                                DropdownMenuItem(
                                    text = { Text("Open in external browser") },
                                    leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
                                    onClick = {
                                        overflowExpanded = false
                                        openInExternalBrowser(art.url)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy link") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        overflowExpanded = false
                                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Article URL", art.url))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Markdown") },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                    onClick = {
                                        overflowExpanded = false
                                        val slug = art.title.lowercase()
                                            .replace(Regex("[^a-z0-9]+"), "-")
                                            .trim('-')
                                            .take(40)
                                        exportLauncher.launch("$slug.md")
                                    }
                                )
                            }
                        }
                    }
                }

                // Progress Bar under Chrome
                if (!isReaderView && isWebLoading) {
                    LinearProgressIndicator(
                        progress = { (webProgress / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = Color(0xFF5CE1E6),
                        trackColor = Color.Transparent
                    )
                }
            }

            // Main Content: Reader View vs. Live WebView
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (currentArticle == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color(0xFF5CE1E6),
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                } else if (isReaderView) {
                    // Reader View Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 100.dp)
                    ) {
                        currentArticle.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 10f),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = currentArticle.title,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 30.sp,
                                color = Color.White
                            )

                            // Publisher & Meta Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SourceAvatar(name = currentArticle.sourceName, size = 26)
                                Column {
                                    Text(
                                        text = currentArticle.sourceName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = formatDateTime(currentArticle.publishedAt),
                                        fontSize = 12.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }

                            val bodyText = articleBody?.markdown
                            if (!bodyText.isNullOrBlank()) {
                                MarkdownArticleBody(markdown = bodyText)
                            } else if (currentArticle.summary.isNotBlank()) {
                                Text(
                                    text = currentArticle.summary,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    color = Color(0xFFE0E0E0)
                                )
                            } else {
                                Text(
                                    text = "Preview text is unavailable for this story.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                } else {
                    // Safe Live In-App WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(0xFF0B0F12.toInt())
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        webProgress = newProgress
                                        isWebLoading = newProgress < 100
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isWebLoading = false
                                        canGoBackInWeb = canGoBack()
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        if (url.startsWith("http://") || url.startsWith("https://")) {
                                            return false
                                        }
                                        openInExternalBrowser(url)
                                        return true
                                    }
                                }
                                loadUrl(currentArticle.url)
                                webViewInstance = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    DisposableEffect(webViewInstance) {
                        onDispose {
                            webViewInstance?.stopLoading()
                            webViewInstance?.destroy()
                            webViewInstance = null
                        }
                    }
                }
            }
        }

        // Floating Bottom Action Pill
        currentArticle?.let { art ->
            val glassBackgroundColor = Color(0xCC12171A)
            val glassBorderColor = Color(0x26FFFFFF)
            val symbol = art.relatedSymbols.firstOrNull() ?: "EURUSD"

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .shadow(elevation = 14.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, glassBorderColor, CircleShape),
                shape = CircleShape,
                color = glassBackgroundColor,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bookmark action
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    val nextState = !isBookmarked
                                    newsRepository.setBookmarked(art.id, nextState)
                                    isBookmarked = nextState
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isBookmarked) Color(0xFF5CE1E6) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Share action
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, art.title)
                                    putExtra(Intent.EXTRA_TEXT, "${art.title}\n${art.url}")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share article"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Reader / Web View Toggle
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { isReaderView = !isReaderView },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isReaderView) Icons.Default.Language else Icons.Default.AutoStories,
                            contentDescription = if (isReaderView) "Web View" else "Reader View",
                            tint = Color(0xFF5CE1E6),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Trade Sizer Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable { onSizeTrade(symbol) },
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFF1F4E52)
                    ) {
                        Text(
                            text = "Size $symbol",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5CE1E6),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
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
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### "),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## "),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    continue
                }
                trimmed.startsWith("> ") -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = Color(0xFF161B1E),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = trimmed.removePrefix("> "),
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 22.sp,
                            color = Color(0xFFA0A0A5),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("•", fontSize = 15.sp, color = Color(0xFF5CE1E6))
                        Text(
                            text = trimmed.substring(2),
                            fontSize = 15.sp,
                            lineHeight = 23.sp,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
                else -> {
                    Text(
                        text = cleanInlineFormatting(trimmed),
                        fontSize = 16.sp,
                        lineHeight = 25.sp,
                        color = Color(0xFFE0E0E0)
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
