package com.Fullboys

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.DoodPmExtractor

@CloudstreamPlugin
class FullboysProvider : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Fullboys())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(DoodPmExtractor())
    }
}
