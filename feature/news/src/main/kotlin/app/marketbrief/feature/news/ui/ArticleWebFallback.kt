package app.marketbrief.feature.news.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.browser.customtabs.CustomTabsIntent
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun ArticleWebFallback(
    url: String,
    modifier: Modifier = Modifier,
    onExternalLinkUnavailable: () -> Unit = {},
    onWebViewChanged: (WebView?) -> Unit = {},
    onCanGoBackChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val safeUrl = remember(url) { validateArticleUrl(url) }
    val adHostMatcher = remember(context) {
        context.resources.openRawResource(app.marketbrief.feature.news.R.raw.ad_hosts)
            .bufferedReader()
            .useLines { AdHostMatcher.fromLines(it) }
    }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(safeUrl == null) }
    var progress by remember { mutableIntStateOf(0) }
    var blockedRequestCount by remember { mutableIntStateOf(0) }
    var showFullPage by remember(url) { mutableStateOf(false) }
    val showFullPageState by rememberUpdatedState(showFullPage)
    val externalLinkUnavailable by rememberUpdatedState(onExternalLinkUnavailable)
    val historyChanged by rememberUpdatedState(onCanGoBackChanged)
    val currentWebView by rememberUpdatedState(webView)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Original publisher page",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                enabled = safeUrl != null,
                onClick = {
                    showFullPage = !showFullPage
                    webView?.let { applyAdHiding(it, hidden = !showFullPage) }
                }
            ) {
                Text(if (showFullPage) "Hide ads" else "Show full page")
            }
        }
        Text(
            "This in-app view limits some ad requests. Ads may remain, and blocking can affect publisher pages.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (blockedRequestCount > 0) {
            Text(
                "Blocked $blockedRequestCount requests to listed ad hosts.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (loading && safeUrl != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (failed) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (safeUrl == null) "This article has an invalid web address." else "The publisher page couldn't load. Check your connection and retry.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (safeUrl != null) {
                        Button(onClick = {
                            failed = false
                            loading = true
                            webView?.reload()
                        }) { Text("Retry") }
                    }
                }
            }
        }
        safeUrl?.let { pageUrl ->
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                factory = { viewContext ->
                    createArticleWebView(
                        context = viewContext,
                        initialUrl = pageUrl,
                        matcher = adHostMatcher,
                        onProgress = { progress = it },
                        onLoading = { loading = it },
                        onError = { failed = it },
                        onBlockedRequests = { blockedRequestCount = it },
                        hideAds = { !showFullPageState },
                        openExternalUrl = { externalUrl ->
                            val intent = CustomTabsIntent.Builder().build()
                            if (intent.intent.resolveActivity(viewContext.packageManager) != null) {
                                intent.launchUrl(viewContext, Uri.parse(externalUrl))
                            } else {
                                externalLinkUnavailable()
                            }
                        },
                        onWebViewChanged = onWebViewChanged,
                        onCanGoBackChanged = { historyChanged(it) }
                    ).also {
                        webView = it
                    }
                },
                update = { view ->
                    if (view.tag != pageUrl) {
                        view.tag = pageUrl
                        view.loadUrl(pageUrl)
                    }
                    applyAdHiding(view, hidden = !showFullPage)
                }
            )
        }
    }

    DisposableEffect(url) {
        onDispose {
            currentWebView?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                destroy()
            }
            webView = null
            onWebViewChanged(null)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createArticleWebView(
    context: Context,
    initialUrl: String,
    matcher: AdHostMatcher,
    onProgress: (Int) -> Unit,
    onLoading: (Boolean) -> Unit,
    onError: (Boolean) -> Unit,
    onBlockedRequests: (Int) -> Unit,
    hideAds: () -> Boolean,
    openExternalUrl: (String) -> Unit,
    onWebViewChanged: (WebView?) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit
): WebView {
    val blockedCount = AtomicInteger()
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setGeolocationEnabled(false)
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgress(newProgress)
            }
        }
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (matcher.matches(request.url.host)) {
                    onBlockedRequests(blockedCount.incrementAndGet())
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(byteArrayOf()))
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val target = request.url.toString()
                if (request.url.scheme !in setOf("http", "https")) return true
                if (AdHostMatcher.sameRegistrableDomain(initialUrl, target)) return false
                if (request.hasGesture()) openExternalUrl(target)
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                onLoading(true)
                onError(false)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                onLoading(false)
                val count = blockedCount.get()
                onBlockedRequests(count)
                onCanGoBackChanged(view.canGoBack())
                Log.d("MarketBriefReader", "Blocked $count listed ad-host requests")
                applyAdHiding(view, hidden = hideAds())
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                if (request.isForMainFrame) {
                    onLoading(false)
                    onError(true)
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: android.webkit.WebResourceResponse
            ) {
                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                    onLoading(false)
                    onError(true)
                }
            }
        }
        tag = initialUrl
        onWebViewChanged(this)
        loadUrl(initialUrl)
    }
}

private fun applyAdHiding(webView: WebView, hidden: Boolean) {
    val script = if (hidden) HIDE_ADS_SCRIPT else REMOVE_ADS_STYLE_SCRIPT
    webView.evaluateJavascript(script, null)
}

private fun validateArticleUrl(url: String): String? {
    val uri = Uri.parse(url)
    return url.takeIf {
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }
}

private const val HIDE_ADS_SCRIPT = """
(function(){
  var old=document.getElementById('marketbrief-ad-filter');
  if(old) old.remove();
  var s=document.createElement('style');
  s.id='marketbrief-ad-filter';
  s.textContent='[id*="ad-"],[class*="ad-"],[id*="advert"],[class*="advert"],[id*="sponsor"],[class*="sponsor"],[id*="taboola"],[class*="taboola"],[id*="outbrain"],[class*="outbrain"],[style*="position: fixed"]{display:none!important;visibility:hidden!important}';
  document.head.appendChild(s);
})();
"""

private const val REMOVE_ADS_STYLE_SCRIPT = """
(function(){
  var old=document.getElementById('marketbrief-ad-filter');
  if(old) old.remove();
})();
"""
