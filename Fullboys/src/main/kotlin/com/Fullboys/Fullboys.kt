package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.*
import org.jsoup.nodes.Element

class Fullboys : MainAPI() {
    override var mainUrl = "https://fullboys.com"
    override var name = "Fullboys"
    override val hasMainPage = true
    override var lang = "en"
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
        val doc = app.get("$mainUrl/home?search=$query").document
        return doc.select(".col-video").mapNotNull {
            val title = it.selectFirst("p.name-video-list")?.text() ?: return@mapNotNull null
            val link = fixUrl(it.attr("href"))
            val poster = it.selectFirst("img")?.absUrl("data-cfsrc")
            val duration = it.selectFirst(".duration")?.text()

            MovieSearchResponse(
                title = title,
                url = link,
                apiName = this.name,
                type = TvType.NSFW,
                posterUrl = poster,
                quality = getQualityFromString(duration ?: "")
            )
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.title-detail-media")?.text() ?: return null
        val videoUrl = doc.select("video#myvideo").attr("src")
        val posterUrl = doc.select("video#myvideo").attr("poster")
        val description = doc.selectFirst("meta[name=description]")?.attr("content") ?: ""
        val tags = doc.select("footer .box-tag a").map { it.text() }

        val previewImages = doc.select(".gallery-row-scroll .img-review").map {
            it.absUrl("data-cfsrc")
        }

        val recommendations = doc.select(".box-list-video .col-video").mapNotNull {
            val name = it.selectFirst("p.name-video-list")?.text() ?: return@mapNotNull null
            val recUrl = fixUrl(it.attr("href"))
            val recPoster = it.selectFirst("img")?.absUrl("data-cfsrc")

            TvSeriesSearchResponse(
                title = name,
                url = recUrl,
                apiName = this.name,
                type = TvType.NSFW,
                posterUrl = recPoster
            )
        }

        return newMovieLoadResponse(title, url, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.backgroundPosterUrl = previewImages.firstOrNull()
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }.addTrailer(videoUrl)
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        callback.invoke(
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