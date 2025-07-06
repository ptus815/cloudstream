package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.json.JSONObject

class Fullboys : MainAPI() {
    override var mainUrl = "https://fullboys.com"
    override var name = "Fullboys"
    override val hasMainPage = true
    override var lang = "vi"
    override val hasQuickSearch = false
    override val hasDownloadSupport = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "/" to "Newest",
        "/topic/video/asian/" to "Asian",
        "/topic/video/china/" to "China",
        "/topic/video/group/" to "Group",
        "/topic/video/japanese/" to "Japanese",
        "/topic/video/korean/" to "Korean",
        "/topic/video/muscle/" to "Muscle",
        "/topic/video/singapore/" to "Singapore",
        "/topic/video/taiwanese/" to "Taiwanese",
        "/topic/video/thailand/" to "Thailand",
        "/topic/video/threesome/" to "Threesome",
        "/topic/video/viet-nam/" to "Vietnamese",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl${request.data}$page/").document
        val home = document.select("article.movie-item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val title = this.selectFirst("h2.title")?.text() ?: return null
        val href = mainUrl + aTag.attr("href")
        val posterUrl = this.selectFirst("img")?.attr("src")
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..7) {
            val document = app.get("$mainUrl/search/video/?s=$query&page=$i").document
            val results = document.select("article.movie-item").mapNotNull { it.toSearchResult() }
            if (results.isEmpty()) break
            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else break
        }
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val jsonData = doc.select("script[type=application/ld+json]").html()
        val json = JSONObject(jsonData)

        val title = json.optString("name", "No Title")
        val poster = json.optString("thumbnailUrl", "")
        val description = json.optString("description", "")

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        document.select("#video-code iframe").forEach { links ->
            val url=links.attr("src")
            Log.d("GayXXX Test",url)
            loadExtractor(url,subtitleCallback, callback)
        }
        return true
    }
}
