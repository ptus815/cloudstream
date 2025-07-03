package com.Xtapes

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Xtapes : MainAPI() {
    override var mainUrl              = "https://gay.xtapes.in"
    override var name                 = "Xtapes"
    override val hasMainPage          = true
    override var lang                 = "en"
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
        val document = app.get("$mainUrl/${request.data}/page/$page/").document
        val home     = document.select("ul.listing-videos li").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title     = this.select("img").attr("title")
        val href      = fixUrl(this.select("a").attr("href"))
        val posterUrl = fixUrlNull(this.select("img").attr("src"))
        println(posterUrl)
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            val document = app.get("${mainUrl}/page/$i/?s=$query").document

            val results = document.select("ul.listing-videos li").mapNotNull { it.toSearchResult() }

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

        val title       = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().toString()
        val iframe      = document.selectFirst("#video-code iframe")?.attr("src").toString()
        val iframeDoc   = app.get(iframe).document
        val poster      = fixUrlNull(iframeDoc.selectFirst("div#vplayer > img")?.attr("src"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot      = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        document.select("#video-code iframe").forEach { links ->
            val url=links.attr("src")
            Log.d("Phisher Test",url)
            loadExtractor(url,subtitleCallback, callback)
        }
        return true
    }
}
