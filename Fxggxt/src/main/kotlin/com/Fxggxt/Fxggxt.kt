package com.Fxggxt

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.nodes.Element

class Fxggxt : MainAPI() {
    override var mainUrl = "https://fxggxt.com"
    override var name = "Fxggxt"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/tag/amateur-gay-porn/page/" to "Amateur",
        "$mainUrl/tag/bareback-gay-porn/page/" to "Bareback",
        "$mainUrl/tag/big-dick-gay-porn/page/" to "Big Dick",
        "$mainUrl/tag/bisexual-porn/page/" to "Bisexual",
        "$mainUrl/tag/group-gay-porn/page/" to "Group",
        "$mainUrl/tag/hunk-gay-porn-videos/page/" to "Hunk",
        "$mainUrl/tag/interracial-gay-porn/page/" to "Interracial",
        "$mainUrl/tag/muscle-gay-porn/page/" to "Muscle",
        "$mainUrl/tag/straight-guys-gay-porn/page/" to "Straight",
        "$mainUrl/tag/twink-gay-porn/page/" to "Twink"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page").document
        val videos = document.select("article.thumb-block a").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, videos, isHorizontalImages = true), hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val href = this.attr("href")
        val title = this.select("header.entry-header span").text()
        var poster = this.select("img").attr("data-src")
        if (poster.isNullOrEmpty()) poster = this.select("img").attr("src")

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResults = mutableListOf<SearchResponse>()
        for (i in 1..7) {
            val doc = app.get("$mainUrl/?s=$query&page=$i").document
            val results = doc.select("article.thumb-block a").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) break
            if (!searchResults.containsAll(results)) searchResults.addAll(results) else break
        }
        return searchResults
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.select("header.entry-header span").text().ifEmpty { "No Title" }
        val poster = doc.select("div.post-thumbnail img").attr("data-src")
        val description = doc.select("meta[name=description]").attr("content")

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val sources = doc.select("#pornone-video-player source")
        sources.forEach { item ->
            val src = item.attr("src")
            val quality = item.attr("res").ifEmpty { "720" }
            callback.invoke(
                newExtractorLink(name, name, src, data, quality.toIntOrNull() ?: 720, isM3u8 = src.endsWith(".m3u8"))
            )
        }
        return true
    }
}
