package com.Fxggxt

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.DoodPmExtractor

@CloudstreamPlugin
class FxggxtPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Fxggxt())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(DoodPmExtractor())
    }
}
