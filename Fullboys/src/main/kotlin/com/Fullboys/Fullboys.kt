package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

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

    // Sửa: Sử dụng đường dẫn tương đối để dễ dàng xử lý phân trang
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
        // Sửa: Xây dựng URL chính xác cho trang chủ và các danh mục
        val url = if (page > 1) {
            // Thêm tham số `page` cho các trang tiếp theo
            "$mainUrl${request.data}?page=$page"
        } else {
            // Trang 1 không cần tham số
            "$mainUrl${request.data}"
        }
        val document = app.get(url).document

        // Sửa: Bộ chọn đúng cho danh sách video là 'a.col-video'
        val home = document.select("a.col-video").mapNotNull { it.toSearchResult() }

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
        // Sửa: Lấy href từ chính phần tử 'a'
        val href = this.attr("href")
        // Sửa: Lấy tiêu đề từ thẻ <p> bên trong
        val title = this.selectFirst("p.name-video-list")?.text() ?: "N/A"
        
        // Sửa: Lấy ảnh bìa từ 'data-cfsrc' (do Cloudflare lazy-load), sau đó mới đến 'src'
        var posterUrl = this.selectFirst("img.img-video-list")?.attr("data-cfsrc")
        if (posterUrl.isNullOrEmpty()) {
            posterUrl = this.selectFirst("img.img-video-list")?.attr("src")
        }

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            // Thêm mainUrl vào trước href nếu nó là đường dẫn tương đối
            this.url = fixUrl(href)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..7) {
            // Sửa: Định dạng URL tìm kiếm chính xác là `/?search=...&page=...`
            val document = app.get("$mainUrl/?search=$query&page=$i").document
            
            // Sửa: Bộ chọn đúng cho kết quả tìm kiếm là 'a.col-video'
            val results = document.select("a.col-video").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) break

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }
        }
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        // Phần này có vẻ đúng vì trang web sử dụng các thẻ meta og
        val title = document.select("meta[property=og:title]").attr("content")
        val poster = document.select("meta[property='og:image']").attr("content")
        val description = document.select("meta[property=og:description]").attr("content")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        
        // Sửa: Lấy link video trực tiếp từ thuộc tính 'src' của thẻ 'video#myvideo'
        document.selectFirst("video#myvideo")?.attr("src")?.let { videoUrl ->
            callback.invoke(
                newExtractorLink(
                    source = this.name, // Tên nguồn (ví dụ: "Fullboys")
                    name = this.name, // Tên để hiển thị cho liên kết
                    url = videoUrl,
                    referer = data,
                    // Giả sử chất lượng mặc định vì không có thông tin chất lượng
                    quality = Qualities.Unknown.value 
                )
            )
        }
        return true
    }
}
