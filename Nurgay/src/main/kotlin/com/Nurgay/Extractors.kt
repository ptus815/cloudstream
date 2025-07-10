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

class Dooodster : DoodLaExtractor() {
    override var mainUrl = "https://dooodster.com"
}

class Listeamed : Vidguardto() {
    override var mainUrl = "https://listeamed.net"
}

class Beamed : Vidguardto() {
    override var mainUrl = "https://bembed.net"
}

class MixDropAG : MixDrop(){
    override var mainUrl = "https://mixdrop.ag"
}

class StreamTapeto : StreamTape() {
    override var mainUrl = "https://streamtape.to"
}

open class BigwarpIO : ExtractorApi() {
    override var name = "Bigwarp"
    override var mainUrl = "https://bigwarp.io"
    override val requiresReferer = false

    private val sourceRegex = Regex("""file:\s*['"](.*?)['"],label:\s*['"](.*?)['"]""")
    private val qualityRegex = Regex("""\d+x(\d+) .*""")

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("Phisher","I'm Here")

        val resp = app.get(url).text
        Log.d("Phisher",resp)
        for (sourceMatch in sourceRegex.findAll(resp)) {
            val label = sourceMatch.groupValues[2]

            callback.invoke(
                newExtractorLink(
                    name,
                    "$name ${label.split(" ", limit = 2).getOrNull(1)}",
                    sourceMatch.groupValues[1], // streams are usually in mp4 format
                ) {
                    this.referer = url
                    this.quality =
                        qualityRegex.find(label)?.groupValues?.getOrNull(1)?.toIntOrNull()
                            ?: Qualities.Unknown.value
                }
            )
        }
    }
}
