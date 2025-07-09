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

    private val cookies = mapOf(
        "hasVisited" to "1",
        "accessAgeDisclaimerPH" to "1"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        try {
            val categoryData = request.data
            val categoryName = request.name
            val pagedLink = if (page > 0) categoryData + page else categoryData
            val soup = app.get(pagedLink, cookies = cookies).document
            val home = soup.select("div.sectionWrapper div.wrap").mapNotNull { it ->
                val title = it.selectFirst("span.title a")?.text().orEmpty()
                val link = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
                val img = fetchImgUrl(it.selectFirst("img"))
                MovieSearchResponse(
                    name = title,
                    url = link,
                    apiName = this.name,
                    type = globalTvType,
                    posterUrl = img
                )
            }
            if (home.isNotEmpty()) {
                return newHomePageResponse(
                    list = HomePageList(
                        name = categoryName,
                        list = home,
                        isHorizontalImages = true
                    ),
                    hasNext = true
                )
            } else {
                throw ErrorLoadingException("No homepage data found!")
            }
        } catch (e: Exception) {
            logError(e)
        }
        throw ErrorLoadingException()
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/video/search?search=${query}"
        val document = app.get(url, cookies = cookies).document
        return document.select("div.sectionWrapper div.wrap").mapNotNull { it ->
            val title = it.selectFirst("span.title a")?.text() ?: return@mapNotNull null
            val link = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val image = fetchImgUrl(it.selectFirst("img"))
            MovieSearchResponse(
                name = title,
                url = link,
                apiName = this.name,
                type = globalTvType,
                posterUrl = image
            )
        }.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val soup = app.get(url, cookies = cookies).document
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
            MovieSearchResponse(
                name = rTitle,
                apiName = this.name,
                url = rUrl,
                posterUrl = rPoster
            )
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
        val request = app.get(data, cookies = cookies)
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
                ).apmap { stream ->
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

    // Đảm bảo hàm này nằm trong class Fullboys
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
