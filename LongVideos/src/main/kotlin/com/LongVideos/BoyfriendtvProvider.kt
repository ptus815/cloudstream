package com.Boyfriendtv

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.StreamTape

@CloudstreamPlugin
class BoyfriendtvProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Boyfriendtv())
    }
}
