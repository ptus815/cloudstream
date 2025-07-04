package com.Fxggxt

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject
import org.jsoup.nodes.Element

class Fxggxt : MainAPI() {
    override var mainUrl = "https://fxggxt.com"
    override var name = "Fxggxt"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/tag/amateur-gay-porn" to "Amateur",
        "$mainUrl/tag/bareback-gay-porn" to "Bareback",
        "$mainUrl/tag/big-dick-gay-porn" to "Big Dick",
        "$mainUrl/tag/bisexual-porn" to "Bisexual",
        "$mainUrl/tag/group-gay-porn" to "Group",
        "$mainUrl/tag/hunk-gay-porn-videos" to "Hunk",
        "$mainUrl/tag/interracial-gay-porn" to "Interracial",
        "$mainUrl/tag/muscle-gay-porn" to "Muscle",
        "$mainUrl/tag/straight-guys-gay-porn" to "Straight",
        "$mainUrl/tag/twink-gay-porn" to "Twink",

        )

        override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
            var document = app.get("$mainUrl${request.data}$page", timeout = 30).document
            val responseList  = document.select(".popbop.vidLinkFX").mapNotNull { it.toSearchResult() }
            return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true),hasNext = true)

    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.select(".videotitle").text()
        val href =  this.attr("href")
        var posterUrl = this.select(".imgvideo").attr("data-src")

        if (posterUrl.isEmpty()) {
            posterUrl = this.select(".imgvideo").attr("src")
        }

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {

        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..7) {
            var document = app.get("$mainUrl/search?q=$query&page=$i", timeout = 30).document

            //val document = app.get("${mainUrl}/page/$i/?s=$queassry").document

            val results = document.select(".popbop.vidLinkFX").mapNotNull { it.toSearchResult() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse

    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val script = document.select("script[data-react-helmet=\"true\"]").html()
        val jsonObj = JSONObject(script)
        val title = jsonObj.get("name")
        val poster = jsonObj.getJSONArray("thumbnailUrl")[0]
        val description = jsonObj.get("description")


        return newMovieLoadResponse(title.toString(), url, TvType.NSFW, url) {
            this.posterUrl = poster.toString()
            this.plot = description.toString()
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document
        val sources = doc.select("#pornone-video-player source")
        sources.forEach { item->
            val src = item.attr("src")
            val quality = item.attr("res")
            callback.invoke(newExtractorLink(
                source = name,
                name = name,
                url = src
            ) {
                this.referer = ""
                this.quality = quality.toInt()
            }
            )
        }



        return true
    }
}
