package com.Fxggxt

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.util.*

class Fxggxt : MainAPI() {
    override var mainUrl = "https://fxggxt.com"
    override var name = "Fxggxt"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    // Cấu trúc dữ liệu cho trang chính với các danh mục
    override val mainPage = mainPageOf(
        "$mainUrl/" to "Latest Videos",
        "$mainUrl/tag/amateur-gay-porn" to "Amateur",
        "$mainUrl/tag/bareback-gay-porn" to "Bareback",
        "$mainUrl/tag/big-dick-gay-porn" to "Big Dick",
        "$mainUrl/tag/bisexual-porn" to "Bisexual",
        "$mainUrl/tag/group-gay-porn" to "Group",
        "$mainUrl/tag/hunk-gay-porn-videos" to "Hunk",
        "$mainUrl/tag/interracial-gay-porn" to "Interracial",
        "$mainUrl/tag/muscle-gay-porn" to "Muscle",
        "$mainUrl/tag/straight-guys-gay-porn" to "Straight",
        "$mainUrl/tag/twink-gay-porn" to "Twink",
        "$mainUrl/category/gay-only-fans" to "OnlyFans"
    )

    // Lấy nội dung trang chính
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document
        
        val home = document.select("article.loop-video").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home, hasNext = true)
    }

    // Chuyển đổi phần tử HTML thành kết quả tìm kiếm
    private fun Element.toSearchResult(): SearchResponse {
        val title = this.selectFirst("header.entry-header span")?.text()?.trim() ?: ""
        val href = this.selectFirst("a")?.attr("href") ?: ""
        val posterUrl = this.selectFirst("img[data-src]")?.attr("data-src") 
            ?: this.selectFirst("img")?.attr("src") ?: ""

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    // Tìm kiếm video
    override suspend fun search(query: String): List<SearchResponse> {
        val searchResults = mutableListOf<SearchResponse>()
        val sanitizedQuery = query.replace(" ", "+")

        for (page in 1..5) {
            val url = "$mainUrl/page/$page/?s=$sanitizedQuery"
            val document = app.get(url).document
            
            val results = document.select("article.loop-video").mapNotNull {
                it.toSearchResult()
            }

            if (results.isEmpty()) break
            
            searchResults.addAll(results)
        }

        return searchResults
    }

    // Lấy chi tiết video
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: ""
        val poster = document.selectFirst("div.post-thumbnail img")?.attr("src") ?: ""
        val description = document.selectFirst("div.entry-content")?.text()?.trim() ?: ""
        
        // Trích xuất diễn viên
        val actors = document.select("a[href*='/actors/']").map {
            Actor(it.text().trim())
        }

        // Trích xuất thể loại
        val tags = document.select("a[rel='tag']").map { it.text() }

        // Trích xuất studio
        val studio = document.select("a[href*='/gay-porn-studios/']").firstOrNull()?.text()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            addActors(actors)
            this.studio = studio
        }
    }

    // Lấy liên kết video để phát
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Ưu tiên nguồn video trực tiếp
        val videoSources = document.select("source[src]").mapNotNull {
            val src = it.attr("abs:src")
            val quality = it.attr("res").removeSuffix("p").toIntOrNull() ?: getQualityFromUrl(src)
            ExtractorLink(
                name,
                quality.toString() + "p",
                src,
                mainUrl,
                quality,
                false
            )
        }

        if (videoSources.isNotEmpty()) {
            videoSources.forEach { callback.invoke(it) }
            return true
        }

        // Fallback: Sử dụng trình trích xuất chung
        val iframeSrc = document.selectFirst("iframe[src]")?.attr("src")
        if (iframeSrc != null) {
            loadExtractor(iframeSrc, mainUrl, subtitleCallback, callback)
        }

        return true
    }

    // Hàm trợ giúp: Lấy chất lượng từ URL
    private fun getQualityFromUrl(url: String): Int {
        return when {
            Regex("1080|720|480|360|240").find(url)?.value?.toIntOrNull() != null -> 
                Regex("1080|720|480|360|240").find(url)?.value?.toInt() ?: Qualities.Unknown.value
            else -> Qualities.Unknown.value
        }
    }
}