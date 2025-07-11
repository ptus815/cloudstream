package com.Jayboystv

import android.content.Context
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.plugins.*

@CloudstreamPlugin
class JayboystvPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(JayboystvProvider())
    }
}
