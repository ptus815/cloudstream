package com.Fxggxt

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element
import org.json.JSONObject

class Fxggxt : MainAPI() {
    override var mainUrl = "https://fxggxt.com"
    override var name = "Fxggxt"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/tag/amateur-gay-porn/" to "Amateur",
        "$mainUrl/tag/bareback-gay-porn/" to "Bareback",
        "$mainUrl/tag/big-dick-gay-porn/" to "Big Dick",
        "$mainUrl/tag/bisexual-porn/" to "Bisexual",
        "$mainUrl/tag/group-gay-porn/" to "Group",
        "$mainUrl/tag/hunk-gay-porn-videos/" to "Hunk",
        "$mainUrl/tag/interracial-gay-porn/" to "Interracial",
        "$mainUrl/tag/muscle-gay-porn/" to "Muscle",
        "$mainUrl/tag/straight-guys-gay-porn/" to "Straight",
        "$mainUrl/tag/twink-gay-porn/" to "Twink",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Trang 1 là URL gốc, các trang sau có định dạng /page/2/
        val url = if (page > 1) {
            "${request.data}page/$page/"
        } else {
            request.data
        }
        val document = app.get(url, timeout = 30).document
        
        // Sử dụng selector mới dựa trên HTML được cung cấp
        val home = document.select(".videos-list article.thumb-block").mapNotNull {
            it.toSearchResult()
        }
        
        // Tự động kiểm tra xem có trang tiếp theo hay không
        val hasNext = document.select("div.pagination a:contains(Next)").isNotEmpty()

        return newHomePageResponse(
            list = HomePageList(request.name, home),
            hasNext = hasNext
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        // Link và tiêu đề nằm trong thẻ 'a' chính
        val linkTag = this.selectFirst("a") ?: return null
        val href = linkTag.attr("href")
        val title = linkTag.attr("title")

        // Poster nằm trong thẻ img với thuộc tính data-src
        val posterUrl = this.selectFirst("img")?.attr("data-src")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        // Trang web sử dụng ?s= để tìm kiếm, lặp qua vài trang để có thêm kết quả
        for (page in 1..7) {
            val url = if (page > 1) {
                "$mainUrl/page/$page/?s=$query"
            } else {
                "$mainUrl/?s=$query"
            }
            val document = app.get(url, timeout = 30).document
            
            val results = document.select(".videos-list article.thumb-block").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break // Dừng lại nếu trang không có kết quả
            }
            searchResponse.addAll(results)
        }
        return searchResponse.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        // Phương pháp cũ dùng script của react-helmet không còn.
        // Các trang WordPress với plugin Yoast SEO thường dùng JSON-LD.
        val script = document.selectFirst("script.yoast-schema-graph")?.html()
            ?: document.selectFirst("script[type=\"application/ld+json\"]")?.html()

        var title = document.selectFirst("h1.entry-title")?.text() ?: "No title"
        var plot: String? = null
        var poster: String? = null

        if (script != null) {
            try {
                val json = JSONObject(script)
                val graph = json.optJSONArray("@graph")
                if (graph != null) {
                    for (i in 0 until graph.length()) {
                        val item = graph.getJSONObject(i)
                        // Tìm đối tượng chứa thông tin chính (VideoObject, Article, WebPage)
                        when (item.optString("@type")) {
                            "VideoObject", "Article", "WebPage" -> {
                                if (item.has("name")) title = item.getString("name")
                                if (item.has("description")) plot = item.getString("description")
                                // Poster có thể nằm trong đối tượng 'image' hoặc mảng 'thumbnailUrl'
                                item.optJSONObject("image")?.let {
                                    poster = it.optString("url")
                                } ?: item.optJSONArray("thumbnailUrl")?.let {
                                    poster = it.optString(0)
                                }
                                if (plot != null) break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Bỏ qua nếu không phân tích được JSON
            }
        }
        
        // Lấy dữ liệu từ thẻ meta OpenGraph nếu không tìm thấy trong JSON-LD
        if (plot.isNullOrEmpty()) {
            plot = document.select("meta[property=og:description]").attr("content")
        }
        if (poster.isNullOrEmpty()) {
            poster = document.select("meta[property=og:image]").attr("content")
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster?.trim()
            this.plot = plot?.trim()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // LƯU Ý: Selector bên dưới là của trang web cũ và RẤT CÓ THỂ ĐÃ HỎNG.
        // Cần phải cập nhật lại bằng cách kiểm tra thủ công trang xem phim trên web.
        val doc = app.get(data).document
        val sources = doc.select("#pornone-video-player source") // Selector này có thể không còn đúng

        if (sources.isEmpty()) {
            return false // Trả về false nếu không tìm thấy link video
        }

        sources.forEach { item ->
            val src = item.attr("src")
            val qualityStr = item.attr("res") // Ví dụ: "720"
            // Dùng toIntOrNull() để tránh crash
            val quality = qualityStr.toIntOrNull()?.let {
                Qualities.find(it)
            } ?: Qualities.Unknown

            callback.invoke(
                ExtractorLink(
                    source = this.name,
                    name = "${this.name} ${quality.name}",
                    url = src,
                    referer = mainUrl, // Nên cung cấp referer
                    quality = quality.value
                )
            )
        }
        return true
    }
}
