package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.json.JSONObject
import org.json.JSONArray

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
        "/"                   to "Newest",
        "/topic/video/asian/"      to "Asian",
        "/topic/video/china/"      to "China",
        "/topic/video/group/"      to "Group",
        "/topic/video/japanese/"   to "Japanese",
        "/topic/video/korean/"     to "Korean",
        "/topic/video/muscle/"     to "Muscle",
        "/topic/video/singapore/"  to "Singapore",
        "/topic/video/taiwanese/"  to "Taiwanese",
        "/topic/video/thailand/"   to "Thailand",
        "/topic/video/threesome/"  to "Threesome",
        "/topic/video/viet-nam/"   to "Vietnamese",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/$page/").document
        val home = document.select("#div-search-results div.mb").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(
            list    = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.select(".title").text()
        val href = mainUrl + this.select(".thumbnail").attr("href")
        val posterUrl = this.selectFirst(".fix-w")?.attr("src")
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {

        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..7) {
            val document = app.get("$mainUrl/search/video/?s=$query&page=$i").document
            //val document = app.get("${mainUrl}/page/$i/?s=$query").document

            val results = document.select("div.video").mapNotNull { it.toSearchResult() }

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
    val doc = app.get(url).document
    val videoElement = doc.selectFirst("article[itemtype='http://schema.org/VideoObject']")
        ?: throw ErrorLoadingException("Không tìm thấy thẻ video")

    val title = videoElement.selectFirst("meta[itemprop='name']")?.attr("content") ?: "No Title"
    val poster = videoElement.selectFirst("meta[itemprop='thumbnailUrl']")?.attr("content") ?: ""
    val description = videoElement.selectFirst("meta[itemprop='description']")?.attr("content") ?: ""

    val actors = doc.select("#video-actors a").mapNotNull { it.text() }.filter { it.isNotBlank() }

    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = poster
        this.plot = description
        if (actors.isNotEmpty()) actors
    }
}

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val embedUrl = document.selectFirst("div.responsive-player iframe")?.attr("src")

        if (!embedUrl.isNullOrEmpty()) {
            loadExtractor(embedUrl, data, subtitleCallback, callback)
        }

        return true
    }
}
