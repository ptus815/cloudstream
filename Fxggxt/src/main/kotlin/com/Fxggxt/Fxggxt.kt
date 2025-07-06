package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Fullboys : MainAPI() {
    override var mainUrl = "https://fullboys.com"
    override var name = "Fullboys"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Newest",
        "$mainUrl/topic/video/muscle/" to "Muscle",
        "$mainUrl/topic/video/japanese/" to "Japanese",
        "$mainUrl/topic/video/korean/" to "Korean",
        "$mainUrl/topic/video/taiwanese/" to "Taiwanese",
        "$mainUrl/topic/video/viet-nam/" to "Vietnamese"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page > 1) "${request.data}?page=$page" else request.data
        val doc = app.get(url).document
        val items = doc.select("a.col-video").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            HomePageList(request.name, items, isHorizontalImages = true),
            hasNext = items.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val href = this.attr("href")
        val title = this.selectFirst("p.name-video-list")?.text() ?: return null
        val poster = this.selectFirst("img")?.let {
            it.attr("data-cfsrc").takeIf { src -> src.isNotBlank() } ?: it.attr("src")
        }
        val duration = this.selectFirst(".duration")?.text()

        return newMovieSearchResponse(title, fixUrl(href), TvType.NSFW) {
            this.posterUrl = poster
            this.quality = getQualityFromString(duration)?.value
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        for (i in 1..5) {
            val url = if (i == 1) "$mainUrl/home?search=$query" else "$mainUrl/home?search=$query&page=$i"
            val doc = app.get(url).document
            val items = doc.select("a.col-video").mapNotNull { it.toSearchResult() }

            if (items.isEmpty()) break
            results.addAll(items)
        }
        return results
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1.title-detail-media")?.text() ?: "No Title"
        val videoUrl = doc.select("video#myvideo").attr("src")
        val poster = doc.select("video#myvideo").attr("poster")
        val description = doc.selectFirst("meta[name=description]")?.attr("content") ?: ""
        val actors = doc.select("footer .box-tag a").mapNotNull { it.text() }.filter { it.isNotBlank() }

        return newMovieLoadResponse(title, url, TvType.NSFW, videoUrl) {
            this.posterUrl = poster
            this.plot = description
            if (actors.isNotEmpty()) addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback(
            ExtractorLink(
                source = name,
                name = "Fullboys Stream",
                url = data,
                referer = mainUrl,
                quality = Qualities.Unknown.value,
                isM3u8 = false
            )
        )
        return true
    }
}
