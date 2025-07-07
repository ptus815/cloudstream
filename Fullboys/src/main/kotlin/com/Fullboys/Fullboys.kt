package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element

class Fullboys : MainAPI() {
    override var mainUrl = "https://fullboys.com"
    override var name = "Fullboys"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "/?filter=vip" to "HOT",
        "/topic/video/muscle" to "Muscle",
        "/topic/video/korean" to "Korean",
        "/topic/video/japanese" to "Japanese",
        "/topic/video/taiwanese" to "Taiwanese",
        "/topic/video/viet-nam" to "Vietnamese"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl${request.data}" else "$mainUrl${request.data}?page=$page"
        val document = app.get(pageUrl).document

        val items = document.select("article.movie-item").mapNotNull { toSearchItem(it) }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = true
            ),
            hasNext = items.isNotEmpty()
        )
    }

    private fun toSearchItem(element: Element): SearchResponse? {
        val aTag = element.selectFirst("a") ?: return null
        val url = fixUrl(aTag.attr("href"))
        val name = aTag.selectFirst("h2.title")?.text() ?: return null

        val image = aTag.selectFirst("img")?.let {
            it.attr("data-cfsrc").takeIf { src -> src.isNotBlank() } ?: it.attr("src")
        } ?: return null

        val duration = aTag.selectFirst("span.duration")?.text()

        return MovieSearchResponse(
            name = name,
            url = url,
            apiName = this@Fullboys.name,
            type = TvType.NSFW,
            posterUrl = image,
            quality = getQualityFromString(duration)
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/home?search=$query"
        val document = app.get(url).document
        return document.select("article.movie-item").mapNotNull { toSearchItem(it) }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document

        val name = doc.selectFirst("h1.title-detail-media")?.text() ?: return null
        val videoUrl = doc.selectFirst("video#myvideo")?.attr("src") ?: return null
        val posterUrl = doc.selectFirst("video#myvideo")?.attr("poster")
        val description = doc.selectFirst("meta[name=description]")?.attr("content").orEmpty()
        val tags = doc.select("footer .box-tag a").map { it.text() }
        val previewImages = doc.select(".gallery-row-scroll img").mapNotNull {
            it.attr("data-cfsrc").takeIf { it.isNotBlank() }
        }

        val recommendations = doc.select(".box-list-video .col-video").mapNotNull {
        val recName = it.selectFirst("p.name-video-list")?.text() ?: return@mapNotNull null
            val recUrl = fixUrl(it.attr("href"))
            val recThumb = it.selectFirst("img")?.let {
                it.attr("data-cfsrc").takeIf { it.isNotBlank() } ?: it.attr("src")
            }
            MovieSearchResponse(
                name = recName,
                url = recUrl,
                apiName = this@Fullboys.name,
                type = TvType.NSFW,
                posterUrl = recThumb
            )
        }

        return MovieLoadResponse(
            name = name,
            url = url,
            apiName = this.name,
            type = TvType.NSFW,
            dataUrl = videoUrl,
            posterUrl = posterUrl,
            backgroundPosterUrl = previewImages.firstOrNull(),
            plot = description,
            tags = tags,
            recommendations = recommendations
        )
    }

        override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    callback(
        newExtractorLink(
            source = name,
            name = "Fullboys Stream",
            url = data,
            referer = mainUrl,
            quality = 720,
            isM3u8 = false
        )
    )
    return true
}
}
