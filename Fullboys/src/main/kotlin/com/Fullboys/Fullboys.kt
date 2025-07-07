package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
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
        "/topic/video/korean/" to "Korean",
        "/topic/video/muscle/" to "Muscle",
        "/topic/video/viet-nam/" to "Vietnamese"
    )

override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/$page/").document
        val home = document.select("article.movie-item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(
            list    = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }


    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val href = aTag.attr("href")
        val title = aTag.selectFirst("h2.title")?.text() ?: "No Title"

        var posterUrl = aTag.selectFirst("img")?.attr("data-src")
        if (posterUrl.isNullOrEmpty()) {
            posterUrl = aTag.selectFirst("img")?.attr("src")
    }

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
    }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..7) {
            val url = if (i > 1) {
                "$mainUrl/page/$i/?s=$query"
            } else {
                "$mainUrl/?s=$query"
            }

            val document = app.get(url).document
            val results = document.select("article.movie-item").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) break
            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }
}

        return searchResponse
}

    
    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document

        val name = doc.selectFirst("h1.title-detail")?.text() ?: return null
        val videoUrl = doc.select("iframe.ifvideo").attr("src")
        val posterUrl = doc.select("preview-image").attr("poster")
        val description = doc.selectFirst("meta[name=description]")?.attr("content").orEmpty()
        val tags = doc.select("footer .box-tag a").map { it.text() }
        val previewImages = doc.select(".gallery-row-scroll img").mapNotNull {
            it.attr("data-cfsrc").takeIf { it.isNotBlank() }
        }

        val recommendations = doc.select(".movie-item").mapNotNull {
            val recName = it.selectFirst("p.title")?.text() ?: return@mapNotNull null
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
        val document = app.get(data).document
        val embedUrl = document.selectFirst("div.responsive-player iframe")?.attr("src")

        if (!embedUrl.isNullOrEmpty()) {
            loadExtractor(embedUrl, data, subtitleCallback, callback)
        }

return true
}
}
