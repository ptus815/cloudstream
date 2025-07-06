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
            hasNext = home.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val href = fixUrl(this.attr("href"))
        val title = this.selectFirst("p.name-video-list")?.text() ?: return null
        
        // Xử lý ảnh với Cloudflare
        val img = this.selectFirst("img.img-video-list")
        val posterUrl = when {
            img?.hasAttr("data-cfsrc") == true -> img.attr("data-cfsrc")
            img?.hasAttr("src") == true -> img.attr("src")
            else -> ""
        }
        
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        var currentPage = 1
        
        while (currentPage <= 5) {
            val url = "$mainUrl/?search=${query.encodeURL()}&page=$currentPage"
            val document = app.get(url).document
            
            val results = document.select("a.col-video").mapNotNull { 
                it.toSearchResult() 
            }
            
            if (results.isEmpty()) break
            
            searchResponse.addAll(results)
            currentPage++
        }
        
        return searchResponse.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        val title = document.selectFirst("meta[property=og:title]")?.attr("content") 
            ?: document.selectFirst("title")?.text() ?: "No Title"
            
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
        
        // Cách 1: Lấy từ thẻ video source
        val directSource = document.selectFirst("video#myvideo source")?.attr("src")
        if (!directSource.isNullOrBlank()) {
            callback(createExtractorLink(directSource))
            return true
        }
        
        // Cách 2: Tìm trong thẻ script
        val scriptContent = document.select("script").map { it.html() }
            .find { it.contains("video_url") || it.contains("file:") }
        
        scriptContent?.let { script ->
            // Regex tìm URL video
            val videoRegex = Regex("""(https?://[^"'\\s]+\.(?:mp4|m3u8))""")
            val matches = videoRegex.findAll(script)
            
            matches.forEach { match ->
                val videoUrl = match.value
                callback(createExtractorLink(videoUrl))
            }
            
            if (matches.any()) return true
        }
        
        return false
    }
    
    private fun createExtractorLink(url: String): ExtractorLink {
        return ExtractorLink(
            source = name,
            name = name,
            url = url,
            referer = "$mainUrl/",
            quality = Qualities.Unknown.value
        )
    }
}