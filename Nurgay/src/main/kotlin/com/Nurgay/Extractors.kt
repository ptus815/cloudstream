package com.Nurgay

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.Vidguardto
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities

class Dooodster : DoodLaExtractor() {
    override var mainUrl = "https://dooodster.com"
}

class Listeamed : Vidguardto() {
    override var mainUrl = "https://listeamed.net"
}

class Beamed : Vidguardto() {
    override var mainUrl = "https://bembed.net"
}

class StreamTapeto : StreamTape() {
    override var mainUrl = "https://streamtape.to"
}

override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val response0 = app.get(url).text // html of DoodStream page to look for /pass_md5/...
        val md5 =mainUrl+(Regex("/pass_md5/[^']*").find(response0)?.value ?: return null)  // get https://dood.ws/pass_md5/...
        val trueUrl = app.get(md5, referer = url).text + "zUEJeL3mUN?token=" + md5.substringAfterLast("/")   //direct link to extract  (zUEJeL3mUN is random)
        val quality = Regex("\\d{3,4}p").find(response0.substringAfter("<title>").substringBefore("</title>"))?.groupValues?.get(0)
        return listOf(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = trueUrl
            ) {
                this.referer = mainUrl
                this.quality = getQualityFromName(quality)
            }
        ) // links are valid in 8h

    }
}

open class Bigwarp : ExtractorApi() {
    override val name = "Bigwarp"
    override val mainUrl = "https://bigwarp.io"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
    ): List<ExtractorLink>? {
        val response =app.get(url).document
        val script = response.selectFirst("script:containsData(sources)")?.data().toString()
        Regex("sources:\\s*\\[.file:\"(.*)\".*").find(script)?.groupValues?.get(1)?.let { link ->
                return listOf(
                    newExtractorLink(
                        source = name,
                        name = name,
                        url = link,
                        INFER_TYPE
                    ) {
                        this.referer = referer ?: "$mainUrl/"
                        this.quality = Qualities.P1080.value
                    }
                )
        }
        return null
    }
}
