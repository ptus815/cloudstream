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

open class Bigwarp : ExtractorApi() {
    override val name = "Bigwarp"
    override val mainUrl = "https://bigwarp.io"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
    ): List<ExtractorLink>? {
        return try {
            val response = app.get(url).document
            val script = response.selectFirst("script:containsData(sources)")?.data().toString()
            Regex("sources:\\s*\\[.*file:\"(.*)\".*").find(script)?.groupValues?.get(1)?.let { link ->
                listOf(
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
        } catch (e: Exception) {
            Log.e(e) // Log error for debugging
            null
        }
    }
}
