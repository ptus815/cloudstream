package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

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

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (page == 1) {
            "$mainUrl${request.data}"
        } else {
            "$mainUrl${request.data}?page=$page"
        }
        
        val document = app.get(url).document
        val home = document.select("a.col-video").mapNotNull { 
            it.toSearchResult() 
        }

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
        val href = fixUrl(this.attr("href"))
        val title = this.selectFirst("p.name-video-list")?.text() ?: return null
        
        var posterUrl = this.selectFirst("img.img-video-list")?.attr("data-cfsrc")
        if (posterUrl.isNullOrBlank()) {
            posterUrl = this.selectFirst("img.img-video-list")?.attr("src")
        }
        
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        
        for (page in 1..5) {
            val url = "$mainUrl/?search=$query&page=$page"
            val document = app.get(url).document
            
            val results = document.select("a.col-video").mapNotNull { 
                it.toSearchResult() 
            }
            
            if (results.isEmpty()) break
            searchResponse.addAll(results)
        }
        
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        val title = document.selectFirst("meta[property=og:title]")?.attr("content") 
            ?: document.selectFirst("title")?.text() ?: ""
        val poster = document.selectFirst("meta[property=og:image]")?.attr("content") 
            ?: ""
        val description = document.selectFirst("meta[property=og:description]")?.attr("content") 
            ?: ""

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
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
        val document = app.get(data).document
        
        val videoElement = document.selectFirst("video#myvideo source")
        val videoUrl = videoElement?.attr("src") ?: return false
        
        // SỬA LỖI DEPRECATED Ở ĐÂY:
        callback.invoke(
            newExtractorLink( // Sử dụng newExtractorLink thay vì ExtractorLink constructor
                source = name,
                name = name,
                url = videoUrl,
                referer = mainUrl,
                quality = Qualities.Unknown.value,
                isM3u8 = videoUrl.contains(".m3u8")
            )
        )
        
        return true
    }
}