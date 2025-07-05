package com.Fullboys

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import org.jsoup.nodes.Element
import org.json.JSONObject

class Fullboys : MainAPI() {
    override var mainUrl              = "https://fullboys.com"
    override var name                 = "Fullboys"
    override val hasMainPage          = true
    override var lang                 = "vi"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/home/"                   to "Newest",
        "${mainUrl}/topic/video/asian/"      to "Asian",
        "${mainUrl}/topic/video/china/"      to "China",
        "${mainUrl}/topic/video/group/"      to "Group",
        "${mainUrl}/topic/video/japanese/"   to "Japanese",
        "${mainUrl}/topic/video/korean/"     to "Korean",
        "${mainUrl}/topic/video/muscle/"     to "Muscle",
        "${mainUrl}/topic/video/singapore/"  to "Singapore",
        "${mainUrl}/topic/video/taiwanese/"  to "Taiwanese",
        "${mainUrl}/topic/video/thailand/"   to "Thailand",
        "${mainUrl}/topic/video/threesome/"  to "Treesome",
        "${mainUrl}/topic/video/viet-nam/"   to "Vietnamese",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "${request.data}$page"
        val doc = app.get(url).document
        val items = doc.select("a.col-video").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(
            listOf(HomePageList(request.name, items)),
            hasNext = items.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val href = attr("href")
        val title = selectFirst("p.name-video-list")?.text()?.trim() ?: return null
        val poster = selectFirst("img.img-video-list")?.attr("src") ?: return null
        return newMovieSearchResponse(title, mainUrl + href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?search=$query"
        val doc = app.get(url).document
        return doc.select("a.col-video").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val json = doc.select("script[type=application/ld+json]").joinToString("") { it.data() }
        val jsonObj = JSONObject(json)

        val title = jsonObj.optString("name", "No Title")
        val poster = jsonObj.optString("thumbnailUrl")
        val description = jsonObj.optString("description", "")
        val videoUrl = jsonObj.optString("contentUrl")

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            addDuration(jsonObj.optString("duration"))
            this.addLinks(
                ExtractorLink(
                    name,
                    name,
                    videoUrl,
                    url,
                    quality = getQualityFromName(videoUrl),
                    isM3u8 = videoUrl.endsWith(".m3u8")
                )
            )
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val videoUrl = doc.selectFirst("video#myvideo")?.attr("src") ?: return false
        callback.invoke(
            ExtractorLink(
                name,
                name,
                videoUrl,
                data,
                quality = getQualityFromName(videoUrl),
                isM3u8 = videoUrl.endsWith(".m3u8")
            )
        )
        return true
    }
}
