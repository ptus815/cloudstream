package com.Nurgay

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class NurgayProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Nurgay())
        registerExtractorAPI(Dooodster())
        registerExtractorAPI(Bigwarp())
        registerExtractorAPI(Listeamed())
        registerExtractorAPI(Beamed())
        registerExtractorAPI(Bgwp())
    }
}
