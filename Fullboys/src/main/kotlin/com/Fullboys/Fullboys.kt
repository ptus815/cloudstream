package com.Fullboys

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.getQualityFromName
import org.json.JSONObject

class Fullboys : MainAPI() {
    private val globalTvType = TvType.NSFW
    override var mainUrl = "https://fullboys.com"
    override var name = "Fullboys"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
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

        return MovieSearchResponse(
            name = name,
            url = url,
            apiName = this@Fullboys.name,
            type = TvType.NSFW,
            posterUrl = image
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/home?search=$query"
        val document = app.get(url).document
        return document.select("article.movie-item").mapNotNull { toSearchItem(it) }
    }


    override suspend fun load(url: String): LoadResponse {
        val title = soup.selectFirst(".title span")?.text().orEmpty()
        val poster: String? = soup.selectFirst("div.video-wrapper .mainPlayerDiv img")?.attr("src")
            ?: soup.selectFirst("head meta[property=og:image]")?.attr("content")
        val tags = soup.select("div.categoriesWrapper a")
            .map { it?.text()?.trim().orEmpty().replace(", ", "") }
        val actors = soup.select("div.video-wrapper div.video-info-row.userRow div.userInfo div.usernameWrap a")
            .map { it.text() }
        val relatedVideo = soup.select("li.fixedSizeThumbContainer").mapNotNull {
            val rTitle = it.selectFirst("div.phimage a")?.attr("title").orEmpty()
            val rUrl = fixUrlNull(it.selectFirst("div.phimage a")?.attr("href")) ?: return@mapNotNull null
            val rPoster = fixUrlNull(it.selectFirst("div.phimage img.js-videoThumb")?.attr("src"))
            newMovieSearchResponse(
                name = rTitle,
                url = rUrl,
                type = globalTvType
            ) {
                posterUrl = rPoster
            }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = title
            this.tags = tags
            addActors(actors)
            this.recommendations = relatedVideo
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = request.document

        val scriptElement = document.selectXpath("//script[contains(text(),'flashvars')]").firstOrNull()
        val scriptData = scriptElement?.data()?.substringAfter("=")?.substringBefore(";")?.trim()

        if (scriptData.isNullOrBlank()) return false

        val jsonObject = runCatching { JSONObject(scriptData) }.getOrNull() ?: return false
        val mediaDefinitions = jsonObject.optJSONArray("mediaDefinitions") ?: return false

        for (i in 0 until mediaDefinitions.length()) {
            val mediaObj = mediaDefinitions.optJSONObject(i) ?: continue
            val quality = mediaObj.optString("quality") ?: continue
            val videoUrl = mediaObj.optString("videoUrl") ?: continue
            val extlinkList = mutableListOf<ExtractorLink>()
            try {
                M3u8Helper().m3u8Generation(
                    M3u8Helper.M3u8Stream(videoUrl), true
                ).map { stream ->
                    extlinkList.add(
                        newExtractorLink(
                            source = name,
                            name = this.name,
                            url = stream.streamUrl,
                            type = ExtractorLinkType.M3U8
                        ) {
                            getQualityFromName(
                                Regex("(\\d+)").find(quality)?.groupValues?.getOrNull(1)
                            )?.let { this.quality = it }
                            this.referer = mainUrl
                        }
                    )
                }
                extlinkList.forEach(callback)
            } catch (e: Exception) {
                logError(e)
            }
        }
        return true
    }

    private fun fetchImgUrl(imgsrc: Element?): String? {
        return try {
            imgsrc?.attr("src")
                ?: imgsrc?.attr("data-src")
                ?: imgsrc?.attr("data-mediabook")
                ?: imgsrc?.attr("alt")
                ?: imgsrc?.attr("data-mediumthumb")
                ?: imgsrc?.attr("data-thumb_url")
        } catch (e: Exception) {
            null
        }
    }
}
