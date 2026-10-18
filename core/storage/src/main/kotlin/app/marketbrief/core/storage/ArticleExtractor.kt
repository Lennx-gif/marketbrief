package app.marketbrief.core.storage

import app.marketbrief.core.model.Article
import app.marketbrief.core.model.ArticleBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * On-device article extraction and HTML-to-Markdown engine based on Readability heuristics.
 */
class ArticleExtractor(
    private val userAgent: String = "MarketBrief/1.0 (personal reader)",
    private val timeoutMillis: Int = 10_000
) {

    suspend fun extractFromUrl(article: Article): ArticleBody = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(article.url)
                .userAgent(userAgent)
                .timeout(timeoutMillis)
                .followRedirects(true)
                .ignoreHttpErrors(false)
                .get()

            extract(article, doc)
        } catch (e: Exception) {
            val reason = if (e.message?.contains("timeout", ignoreCase = true) == true) "TIMEOUT" else "NETWORK_ERROR"
            ArticleBody(
                articleId = article.id,
                markdown = article.summary,
                kind = "PREVIEW",
                wordCount = countWords(article.summary),
                extractedAt = System.currentTimeMillis(),
                failureReason = reason
            )
        }
    }

    fun extract(article: Article, doc: Document): ArticleBody {
        // 1. Paywall check
        if (isPaywalled(doc)) {
            return ArticleBody(
                articleId = article.id,
                markdown = article.summary,
                kind = "PREVIEW",
                wordCount = countWords(article.summary),
                extractedAt = System.currentTimeMillis(),
                failureReason = "PAYWALL"
            )
        }

        // 2. Locate main content container
        val candidate = findMainContainer(doc)
        if (candidate == null) {
            return fallbackPreview(article, "EMPTY")
        }

        // 3. Clean noise from candidate
        cleanElement(candidate)

        // 4. Convert DOM to Markdown
        val markdown = buildMarkdown(article, candidate)
        val plainText = candidate.text().trim()
        val paragraphs = candidate.select("p").map { it.text().trim() }.filter { it.length > 25 }

        // 5. Quality gate
        if (plainText.length < 300 || paragraphs.size < 2) {
            return fallbackPreview(article, "BLOCKED")
        }

        return ArticleBody(
            articleId = article.id,
            markdown = markdown,
            kind = "FULL",
            wordCount = countWords(plainText),
            extractedAt = System.currentTimeMillis(),
            failureReason = null
        )
    }

    private fun isPaywalled(doc: Document): Boolean {
        // Check JSON-LD
        val jsonLd = doc.select("script[type=application/ld+json]").eachText()
        if (jsonLd.any { it.contains("\"isAccessibleForFree\":false", ignoreCase = true) || it.contains("\"isAccessibleForFree\": false", ignoreCase = true) }) {
            return true
        }

        // Check CSS selectors
        val paywallSelectors = listOf(
            ".paywall", "#paywall", ".meter-paywall", ".subscriber-only",
            ".subscription-required", ".piano-container", "#gateway-content"
        )
        return paywallSelectors.any { doc.select(it).isNotEmpty() }
    }

    private fun findMainContainer(doc: Document): Element? {
        val selectors = listOf(
            "article",
            "[itemprop=articleBody]",
            ".article-body",
            ".story-body",
            ".entry-content",
            ".post-content",
            ".caas-body",
            "main"
        )
        for (selector in selectors) {
            val el = doc.selectFirst(selector)
            if (el != null && el.select("p").size >= 2) {
                return el
            }
        }
        return doc.body()
    }

    private fun cleanElement(element: Element) {
        val unwantedSelectors = listOf(
            "script", "style", "noscript", "iframe", "form", "nav",
            "header", "footer", "aside", ".advertisement", ".ad",
            ".social-share", ".social", ".newsletter", ".related-articles",
            ".comments", ".promo", ".disclaimer", ".author-bio"
        )
        unwantedSelectors.forEach { selector ->
            element.select(selector).remove()
        }
    }

    private fun buildMarkdown(article: Article, container: Element): String = buildString {
        appendLine("# ${article.title}")
        appendLine()
        appendLine("*${article.sourceName} · [View original](${article.url})*")
        appendLine()

        val elements = container.select("h1, h2, h3, h4, p, blockquote, ul, ol, figure")
        for (el in elements) {
            when (el.tagName().lowercase()) {
                "h1" -> {
                    val text = el.text().trim()
                    if (!text.equals(article.title, ignoreCase = true)) {
                        appendLine("## $text\n")
                    }
                }
                "h2" -> appendLine("## ${el.text().trim()}\n")
                "h3" -> appendLine("### ${el.text().trim()}\n")
                "h4" -> appendLine("#### ${el.text().trim()}\n")
                "p" -> {
                    val pText = nodeToMarkdown(el).trim()
                    if (pText.length > 20) {
                        appendLine("$pText\n")
                    }
                }
                "blockquote" -> {
                    val bText = el.text().trim()
                    if (bText.isNotBlank()) {
                        appendLine("> $bText\n")
                    }
                }
                "ul" -> {
                    el.select("li").forEach { li ->
                        appendLine("- ${li.text().trim()}")
                    }
                    appendLine()
                }
                "ol" -> {
                    el.select("li").forEachIndexed { i, li ->
                        appendLine("${i + 1}. ${li.text().trim()}")
                    }
                    appendLine()
                }
                "figure" -> {
                    val img = el.selectFirst("img")
                    val src = img?.absUrl("src")?.takeIf { it.isNotBlank() } ?: img?.attr("src")
                    val caption = el.selectFirst("figcaption")?.text()?.trim()
                    if (!src.isNullOrBlank() && !src.contains("1x1")) {
                        appendLine("![${caption ?: ""}]($src)\n")
                    }
                }
            }
        }
    }

    private fun nodeToMarkdown(element: Element): String {
        val sb = java.lang.StringBuilder()
        for (child in element.childNodes()) {
            when (child) {
                is TextNode -> sb.append(child.text())
                is Element -> {
                    when (child.tagName().lowercase()) {
                        "strong", "b" -> sb.append("**").append(child.text()).append("**")
                        "em", "i" -> sb.append("*").append(child.text()).append("*")
                        "a" -> {
                            val href = child.absUrl("href").ifBlank { child.attr("href") }
                            if (href.isNotBlank()) {
                                sb.append("[").append(child.text()).append("](").append(href).append(")")
                            } else {
                                sb.append(child.text())
                            }
                        }
                        "code" -> sb.append("`").append(child.text()).append("`")
                        else -> sb.append(child.text())
                    }
                }
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ")
    }

    private fun fallbackPreview(article: Article, reason: String): ArticleBody {
        return ArticleBody(
            articleId = article.id,
            markdown = article.summary,
            kind = "PREVIEW",
            wordCount = countWords(article.summary),
            extractedAt = System.currentTimeMillis(),
            failureReason = reason
        )
    }

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
}
