package com.Jayboystv

fun removeSquareBracketsContent(input: String): String {
    return input.replace(Regex("\\[.*?\\]"), "").trim()
}
