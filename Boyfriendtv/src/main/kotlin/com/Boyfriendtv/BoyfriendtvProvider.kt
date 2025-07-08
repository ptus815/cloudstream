<<<<<<<< HEAD:Boyfriendtv/src/main/kotlin/com/Boyfriendtv/BoyfriendtvProvider.kt
package com.Boyfriendtv
========
package com.Fxggxt
>>>>>>>> 5d3043660592708bf26fa5fe3e6ece33808b6ff3:Fxggxt/src/main/kotlin/com/Fxggxt/FxggxtPlugin.kt

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.DoodPmExtractor

@CloudstreamPlugin
<<<<<<<< HEAD:Boyfriendtv/src/main/kotlin/com/Boyfriendtv/BoyfriendtvProvider.kt
class BoyfriendtvProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Boyfriendtv())
========
class FxggxtPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Fxggxt())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(DoodPmExtractor())
>>>>>>>> 5d3043660592708bf26fa5fe3e6ece33808b6ff3:Fxggxt/src/main/kotlin/com/Fxggxt/FxggxtPlugin.kt
    }
}
