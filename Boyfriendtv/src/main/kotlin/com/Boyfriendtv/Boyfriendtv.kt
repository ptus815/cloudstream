package com.Boyfriendtv

import org.jsoup.nodes.Element
import com.google.gson.Gson
import com.lagradost.api.Log
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.lang.System
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Boyfriendtv : MainAPI() {
    override var mainUrl              = "https://gay.xtapes.in"
    override var name                 = "Boyfriendtv"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "" to "Latest",
        "category/amateur-gay-porn" to "Amateur Cock",
        "category/asian-guys-porn" to "Asian",
        "category/bareback-no-condom-porn-282612" to "Bareback",
        "category/black-guys-porn-41537" to "Black Guys",
        "category/big-thick-cock-porn-816238" to "Big Dicks",
        "category/groupsex-gangbang-porn-189267" to "Group Sex",
        "category/hunks-muscle-studs-porn-310274" to "Hunks",
        "category/latin-287326" to "Latin",
        "category/porn-parody" to "Parody",
        "category/porn-movies-214660" to "Full Movies"
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
