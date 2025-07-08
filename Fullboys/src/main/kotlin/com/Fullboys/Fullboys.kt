package com.Fullboys

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.network.CloudflareKiller // For potential Cloudflare challenges

class Fullboys : MainAPI() {
    override var mainUrl = "https://fullboys.com"
    override var name = "Fullboys"
    override val hasMainPage = true
    override val hasDownloadSupport = true // Keep if you intend to implement download for the direct video links
    override val supportedTypes = setOf(TvType.NSFW)

    // Optional: Add a User-Agent. Some sites might block requests without it.
    private val defaultHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.82 Safari/537.36"
        // You might need "Referer" if the site requires it, e.g., "Referer" to mainUrl
    )

    // Consider using CloudflareKiller if the site is protected by Cloudflare
    // override val mainPageInterceptor = CloudflareKiller()
    // override val normalInterceptor = CloudflareKiller()


    override val mainPage = mainPageOf(
        "/?filter=vip" to "HOT",
        "/topic/video/muscle" to "Muscle",
        "/topic/video/korean" to "Korean",
        "/topic/video/japanese" to "Japanese",
        "/topic/video/taiwanese" to "Taiwanese",
        "/topic/video/viet-nam" to "Vietnamese"
        // Add more categories from the site if desired
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pageUrl = if (page == 1) "$mainUrl${request.data}" else "$mainUrl${request.data.replace("?page=$page", "")}?page=$page" // Ensure page param isn't duplicated
        
        return try {
            // Use app.get with headers
            val document = app.get(pageUrl, headers = defaultHeaders).document
            val items = document.select("article.movie-item").mapNotNull { element ->
                toSearchResponse(element) // Changed to use a more generic SearchResponse mapper
            }

            newHomePageResponse(
                HomePageList(
                    name = request.name,
                    list = items,
                    isHorizontalImages = true // Assuming images are wider than tall
                ),
                hasNext = items.isNotEmpty() // Simple check, might need more robust pagination detection
            )
        } catch (e: Exception) {
            e.printStackTrace() // Log the error for debugging
            // Return an empty response or a response with an error message
            newHomePageResponse(HomePageList(request.name, emptyList()), hasNext = false)
        }
    }

    // Renamed to toSearchResponse for clarity and reusability
    private fun toSearchResponse(element: Element): SearchResponse? {
        val aTag = element.selectFirst("a") ?: return null
        val url = fixUrlNull(aTag.attr("href")) ?: return null // Use fixUrlNull for safety
        if (!url.startsWith(mainUrl)) return null // Basic validation

        val title = aTag.selectFirst("h2.title")?.text()?.trim() ?: return null
        if (title.isBlank()) return null

        // Prioritize "data-cfsrc" if it exists, otherwise use "src"
        val posterUrl = aTag.selectFirst("img")?.let { img ->
            fixUrlNull(img.attr("abs:data-cfsrc").ifBlank { img.attr("abs:src") })
        } ?: return null // If no image, maybe skip this item

        return newMovieSearchResponse( // Cloudstream provides helper for this
            name = title,
            url = url,
            apiName = this.name, // Use this.name for consistency
            type = TvType.NSFW, // Assuming all content is NSFW as per supportedTypes
            posterUrl = posterUrl
        ) {
            // You can add more details here if available on the list item itself
            // For example, if duration is available:
            // this.posterHeaders = mapOf("Duration" to element.selectFirst(".duration")?.text()?.trim())
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // The site uses /home?search= for search
        val searchUrl = "$mainUrl/home?search=${encode(query)}" // Ensure query is URL encoded
        return try {
            val document = app.get(searchUrl, headers = defaultHeaders).document
            document.select("article.movie-item").mapNotNull { element ->
                toSearchResponse(element)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        return try {
            val document = app.get(url, headers = defaultHeaders).document

            val title = document.selectFirst("h1.title-detail-media")?.text()?.trim() 
                ?: document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() // Fallback to OG title
                ?: return null // If no title, cannot proceed

            // Attempt to find video URL - this is often the trickiest part
            // The provided HTML doesn't show the video player structure, so this is a guess.
            // You'll need to inspect a video page to confirm the correct selector.
            val videoUrl = document.selectFirst("video#myvideo source[src], video#myvideo[src]")?.attr("abs:src")
                ?: document.select("script").firstOrNull { script -> 
                        script.data().contains("player.src({ src:") || script.data().contains("file:")
                    }?.data()?.let { scriptData ->
                        // Try to extract from common player initializations (very site-specific)
                        Regex("""(?:player\.src\(\{ src:|file:)\s*["']([^"']+)["']""").find(scriptData)?.groupValues?.get(1)
                    }
                ?: return null // If no video URL found, this item can't be loaded

            val posterUrl = fixUrlNull(document.selectFirst("video#myvideo")?.attr("abs:poster")
                ?: document.selectFirst("meta[property=og:image]")?.attr("content")) // Fallback to OG image

            val description = document.selectFirst("meta[name=description]")?.attr("content")?.trim()
                ?: document.selectFirst("meta[property=og:description]")?.attr("content")?.trim() // Fallback

            // Tags might be in a specific section, adjust selector as needed
            val tags = document.select("footer .box-tag a, .tags-container a.tag").mapNotNull { it.text()?.trim() }.filter { it.isNotBlank() }.distinct()

            // Recommendations (if any) - example selector, adjust based on actual site structure
            val recommendations = document.select("section.related-videos article.movie-item, .sidebar .movie-item").mapNotNull { recElement ->
                toSearchResponse(recElement)
            }

            return newMovieLoadResponse(
                name = title,
                url = url,
                apiName = this.name,
                type = TvType.NSFW, // Or determine dynamically if needed
                dataUrl = videoUrl, // This is the direct link to the video file
                posterUrl = posterUrl,
                plot = description,
                tags = tags,
                recommendations = recommendations
            ) {
                // Add other details if available, e.g., year, duration, actors
                // val durationText
