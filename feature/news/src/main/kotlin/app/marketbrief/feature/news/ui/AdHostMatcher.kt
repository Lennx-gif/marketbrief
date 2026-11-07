package app.marketbrief.feature.news.ui

import java.net.URI
import java.net.URISyntaxException

class AdHostMatcher(hosts: Collection<String>) {
    private val normalizedHosts = hosts.mapNotNull(::normalizeHost).toSet()

    fun matches(host: String?): Boolean {
        val normalized = host?.let(::normalizeHost) ?: return false
        return normalizedHosts.any { blocked ->
            normalized == blocked || normalized.endsWith(".$blocked")
        }
    }

    companion object {
        fun fromLines(lines: Sequence<String>): AdHostMatcher =
            AdHostMatcher(lines.map { it.substringBefore('#').trim() }.filter { it.isNotBlank() }.toList())

        fun sameRegistrableDomain(firstUrl: String, secondUrl: String): Boolean {
            val first = normalizeHost(parseHost(firstUrl)) ?: return false
            val second = normalizeHost(parseHost(secondUrl)) ?: return false
            return registrableDomain(first) == registrableDomain(second)
        }

        private fun parseHost(url: String): String? =
            try {
                URI(url).host
            } catch (_: URISyntaxException) {
                null
            }

        private fun normalizeHost(host: String?): String? =
            host?.trim()?.trimEnd('.')?.lowercase()?.takeIf { it.isNotBlank() }

        private fun registrableDomain(host: String): String {
            val labels = host.split('.')
            if (labels.size < 2 || labels.any { it.isBlank() }) return host
            val commonTwoPartSuffixes = setOf(
                "co.uk", "org.uk", "ac.uk", "com.au", "net.au", "org.au",
                "co.jp", "co.ke", "co.za", "com.br", "com.mx", "co.nz"
            )
            val suffix = labels.takeLast(2).joinToString(".")
            val labelCount = if (suffix in commonTwoPartSuffixes) 3 else 2
            return labels.takeLast(labelCount.coerceAtMost(labels.size)).joinToString(".")
        }
    }
}
