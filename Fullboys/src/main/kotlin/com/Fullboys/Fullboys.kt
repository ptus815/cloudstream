package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URLEncoder

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
        
        // SỬA LỖI encodeURL: Sử dụng URLEncoder chuẩn
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        
        for (page in 1..5) {
            val url = "$mainUrl/?search=$encodedQuery&page=$page"
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
        
        // CÁCH 1: Từ thẻ video
        var videoUrl = document.selectFirst("video#myvideo source")?.attr("src")
        
        // CÁCH 2: Từ thẻ script
        if (videoUrl.isNullOrBlank()) {
            videoUrl = document.select("script:containsData(video)")
                .firstOrNull()
                ?.data()
                ?.substringAfter("file: \"")
                ?.substringBefore("\"")
        }
        
        // CÁCH 3: Từ regex
        if (videoUrl.isNullOrBlank()) {
            videoUrl = document.select("script").map { it.html() }
                .find { it.contains("video") }
                ?.let { 
                    Regex("""file:\s*["'](https?://[^"']+)["']""")
                        .find(it)?.groupValues?.get(1)
                }
        }
        
        if (videoUrl.isNullOrBlank()) return false
        
        // SỬA LỖI DEPRECATED: Sử dụng ExtractorLink với cấu trúc mới
        callback.invoke(
            ExtractorLink(
                source = name,
                name = "Fullboys Video",
                url = videoUrl,
                referer = "$mainUrl/",
                quality = Qualities.Unknown.value,
                isM3u8 = videoUrl.contains(".m3u8")
            )
        )
        
        return true
    }
}