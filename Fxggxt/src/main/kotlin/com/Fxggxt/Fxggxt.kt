package com.Fxggxt

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.nodes.Element

class Fxggxt : MainAPI() {
    override var mainUrl = "https://fxggxt.com"
    override var name = "Fxggxt"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/tag/amateur-gay-porn/page/" to "Amateur",
        "$mainUrl/tag/bareback-gay-porn/page/" to "Bareback",
        "$mainUrl/tag/big-dick-gay-porn/page/" to "Big Dick",
        "$mainUrl/tag/bisexual-porn/page/" to "Bisexual",
        "$mainUrl/tag/group-gay-porn/page/" to "Group",
        "$mainUrl/tag/hunk-gay-porn-videos/page/" to "Hunk",
        "$mainUrl/tag/interracial-gay-porn/page/" to "Interracial",
        "$mainUrl/tag/muscle-gay-porn/page/" to "Muscle",
        "$mainUrl/tag/straight-guys-gay-porn/page/" to "Straight",
        "$mainUrl/tag/twink-gay-porn/page/" to "Twink"
    )

    // Sửa: URL cho trang được xử lý chính xác hơn
override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    // request.data sẽ là một phần của URL, ví dụ: "/tag/amateur-gay-porn/"
    // page là số trang
    val url = if (page > 1) {
        // Trang 2 trở đi có định dạng /page/2/
        "${request.data}page/$page/"
    } else {
        // Trang 1 không có số trang trong URL
        request.data
    }

    val document = app.get(url, timeout = 30).document
    // Sửa: Sử dụng bộ chọn CSS chính xác cho các mục video
    val responseList = document.select("article.loop-video.thumb-block").mapNotNull { it.toSearchResult() }
    return newHomePageResponse(
        HomePageList(request.name, responseList, isHorizontalImages = true),
        hasNext = true
    )
}

private fun Element.toSearchResult(): SearchResponse {
    // Sửa: Lấy liên kết từ thẻ <a> bên trong
    val aTag = this.selectFirst("a")
    val href = aTag?.attr("href") ?: return null // Bỏ qua nếu không có link

    // Sửa: Lấy tiêu đề từ thẻ span bên trong header
    val title = aTag.selectFirst("header.entry-header span")?.text() ?: "No Title"

    // Sửa: Lấy ảnh bìa từ thuộc tính data-src của thẻ img
    var posterUrl = aTag.selectFirst(".post-thumbnail-container img")?.attr("data-src")
    if (posterUrl.isNullOrEmpty()) {
        posterUrl = aTag.selectFirst(".post-thumbnail-container img")?.attr("src")
    }

    return newMovieSearchResponse(title, href, TvType.Movie) {
        this.posterUrl = posterUrl
    }
}

   override suspend fun search(query: String): List<SearchResponse> {
    val searchResponse = mutableListOf<SearchResponse>()
    // Sửa: Vòng lặp qua các trang kết quả tìm kiếm với URL chính xác
    for (i in 1..7) {
        val url = if (i > 1) {
            "$mainUrl/page/$i/?s=$query"
        } else {
            "$mainUrl/?s=$query"
        }
        val document = app.get(url, timeout = 30).document

        // Sử dụng lại hàm toSearchResult đã sửa
        val results = document.select("article.loop-video.thumb-block").mapNotNull { it.toSearchResult() }

        if (results.isEmpty()) {
            break // Dừng lại nếu không còn kết quả
        }
        
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
    
    // Sửa: Tìm thẻ script JSON-LD, là nơi chứa thông tin chi tiết
    val script = document.selectFirst("script[type=\"application/ld+json\"]")?.html() ?: ""
    val jsonObj = JSONObject(script.substringAfter("{\"@context\":\"https://schema.org\",\"@graph\":").substringBeforeLast("]}") + "]")
    val videoInfo = jsonObj.getJSONArray("graph").let {
        var video: JSONObject? = null
        for (i in 0 until it.length()) {
            if (it.getJSONObject(i).getString("@type") == "VideoObject") {
                video = it.getJSONObject(i)
                break
            }
        }
        video ?: throw Exception("Không tìm thấy VideoObject JSON")
    }

    val title = videoInfo.getString("name")
    val poster = videoInfo.getString("thumbnailUrl")
    val description = videoInfo.getString("description")
    val actors = videoInfo.optJSONArray("actor")?.let {
        (0 until it.length()).mapNotNull { i ->
            it.getJSONObject(i).getString("name")
        }
    }

    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = poster
        this.plot = description
        if (actors != null) {
            addActors(actors)
        }
    }
}

    override suspend fun loadLinks(
    data: String, // Đây là URL của trang video, ví dụ: https://fxggxt.com/men-cinderello-part-1-alex-marte-dean-young/
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    // Tải tài liệu HTML từ URL trang
    val document = app.get(data).document

    // Sửa: Tìm thẻ iframe bên trong div có class là "responsive-player"
    val embedUrl = document.selectFirst("div.responsive-player iframe")?.attr("src")

    // Kiểm tra xem URL có được tìm thấy không
    if (embedUrl != null) {
        // CloudStream có các trình trích xuất tích hợp cho các máy chủ phổ biến
        // Chỉ cần gọi loadExtractor với URL của iframe
        loadExtractor(embedUrl, data, subtitleCallback, callback)
    }

    return true
}
