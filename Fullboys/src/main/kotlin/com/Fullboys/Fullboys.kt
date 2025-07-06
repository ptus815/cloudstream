package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Fullboys : MainAPI() {
    override var mainUrl = "https://fullboys.com"
    override var name = "Fullboys"
    override val hasMainPage = true
    override val hasQuickSearch = true
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
        "/topic/video/viet-nam/" to "Vietnamese"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val suffix = if (page <= 1) "" else "?page=$page"
        val doc = app.get("$mainUrl${request.data}$suffix").document
        val items = doc.select("a.col-video").mapNotNull(Element::toSearchResult)
        return newHomePageResponse(
            listOf(HomePageList(request.name, items, isHorizontalImages = true)),
            hasNext = items.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val href = fixUrl(attr("href"))
        val title = selectFirst("p.name-video-list")?.text() ?: return null
        val img = selectFirst("img.img-video-list")
        val poster = img?.attr("data-cfsrc").ifEmpty { img?.attr("src") }
        val duration = selectFirst(".duration")?.text().orEmpty()
        return newMovieSearchResponse(title, href, TvType.Movie) {
            posterUrl = poster
            quality = getQualityFromString(duration)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/home?search=$query").document
        return doc.select("a.col-video").mapNotNull(Element::toSearchResult)
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.title-detail-media")?.text() ?: return null
        val videoUrl = doc.select("video#myvideo").attr("src")
        val posterUrl = doc.select("video#myvideo").attr("poster")
        val description = doc.selectFirst("meta[name=description]")?.attr("content").orEmpty()
        val tags = doc.select("footer .box-tag a").map(Element::text)
        val previewImages = doc.select(".gallery-row-scroll img").map { it.absUrl("data-cfsrc") }
        val recs = doc.select(".box-list-video .col-video").mapNotNull {
            val title2 = it.selectFirst("p.name-video-list")?.text() ?: return@mapNotNull null
            val href2 = fixUrl(it.attr("href"))
            val img2 = it.selectFirst("img")?.attr("data-cfsrc")
            MovieSearchResponse(title2, href2, name = name, type = TvType.Movie, posterUrl = img2)
        }

        return newMovieLoadResponse(title, url, TvType.Movie) {
            this.posterUrl = posterUrl
            backgroundPosterUrl = previewImages.firstOrNull()
            plot = description
            this.tags = tags
            recommendations = recs
        }.addTrailer(videoUrl)
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        callback(
            ExtractorLink(
                name = "Fullboys",
                source = "fullboys.com",
                url = data,
                quality = Qualities.Unknown,
                isM3u8 = false
            )
        )
    }
}
