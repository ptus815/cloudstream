package com.Nurgay

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Nurgay : MainAPI() {
    override var mainUrl              = "https://nurgay.to"
    override var name                 = "Nurgay"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "/?filter=latest" to "Lates",
        "/?filter=random" to "Random",
        "/?filter=longest" to "Longest",
        "/?filter=most-viewed" to "Most Viewed",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl${request.data}/page/$page/").document
        val home     = document.select("div.slide loop-video bx-clone > div.slide loop-video").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
                list    = HomePageList(
                name    = request.name,
                list    = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title     = this.select("a").attr("title")
        val href      = this.select("a").attr("href")
        val posterUrl = this.select("a > span").attr("style").substringAfter("url(").substringBefore(")")

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document
            val results = document.select("div.slide loop-video bx-clone > div.slide loop-video").mapNotNull { it.toSearchResult() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title       = document.select("meta[property=og:title]").attr("content")
        val poster      = document.select("meta[property='og:image']").attr("content")
        val description = document.select("meta[property=og:description]").attr("content")


        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        document.select(".tabcontent > iframe").amap {
            loadExtractor(
                it.attr("data-litespeed-src"),
                referer = data,
                subtitleCallback,
                callback
            )
        }

        document.select("div.title-block box-shadow" a").amap {
            loadExtractor(
                it.attr("href"),
                referer = data,
                subtitleCallback,
                callback
            )
        }
        return true
    }
}
