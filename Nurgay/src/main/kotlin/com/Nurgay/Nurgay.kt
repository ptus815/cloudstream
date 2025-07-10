package com.Nurgay

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lagradost.api.Log

class Nurgay : MainAPI() {
    override var mainUrl = "https://nurgay.to"
    override var name = "Nurgay"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val lang = "de"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "" to "Latest",
        "?filter=random" to "Random",
        "?filter=longest" to "Longest",
        "?filter=most-viewed" to "Most Viewed"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) "$mainUrl/${request.data}" else "$mainUrl/${request.data}/page/$page/"
        val doc = app.get(url).document
        val items = doc.select("div.thumb-block").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            HomePageList(name = request.name, list = items),
            hasNext = items.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = selectFirst("a") ?: return null
        val href = fixUrl(aTag.attr("href"))
        val title = aTag.selectFirst(".entry-header .video-title")?.text()?.trim() ?: return null
        val image = aTag.selectFirst("img")?.attr("src") ?: return null

        return MovieSearchResponse(
            name = title,
            url = href,
            apiName = this@Nurgay.name,
            type = TvType.NSFW,
            posterUrl = image
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val doc = app.get(url).document
        return doc.select("div.thumb-block").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val name = doc.selectFirst("meta[property=og:title]")?.attr("content") ?: return null
        val videoUrl = doc.selectFirst("video#myvideo")?.attr("src") ?: return null
        val posterUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")
        val description = doc.selectFirst("meta[property=og:description]")?.attr("content").orEmpty()

        return MovieLoadResponse(
            name = name,
            url = url,
            apiName = this.name,
            type = TvType.NSFW,
            dataUrl = videoUrl,
            posterUrl = posterUrl,
            plot = description
        )
    }

override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = coroutineScope {
        val parsedList = data.fromJson<List<String>>()
        parsedList.map { url ->
            launch {
                Log.d("Phisher", url)
                loadExtractor(url, "$mainUrl/", subtitleCallback, callback)
            }
        }.joinAll()

        true
    }

    val gson = Gson()
    private inline fun <reified T> String.fromJson(): T = gson.fromJson(this, object : TypeToken<T>() {}.type)
}

