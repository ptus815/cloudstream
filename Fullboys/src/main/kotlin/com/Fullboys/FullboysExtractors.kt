
package com.Fullboys

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.VidhideExtractor
import com.lagradost.cloudstream3.extractors.Contabostorage
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.*

open class Contabostorage : ExtractorApi() {
    override var name = "Contabostorage"
    override var mainUrl = "https://sin1.contabostorage.com/"
    override val requiresReferer = false



    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        with(app.get(url)) {
            this.document.let { document ->
                val finalLink = document.select("iframe#ifvideo").attr("src")
                return listOf(
                      ExtractorLink(
                        source = name,
                        name = name,
                        url = httpsify(finalLink),
                        ExtractorLinkType.M3U8
                    ) {
                        this.referer = url
                        this.quality = Qualities.Unknown.value
                    }
                )
            }
        }
        return null
    }
    }




class Pub : VidhideExtractor() {
    override var name = "Pub"
    override var mainUrl = "https://pub-bfa45585fe66485c873db2f46c452a48.r2.dev/"
    override val requiresReferer = false
}
