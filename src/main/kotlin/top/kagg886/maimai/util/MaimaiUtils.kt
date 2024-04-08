package top.kagg886.maimai.util

import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun Int.toMaimaiDifficult(): String {
    return when (this) {
        0 -> "Basic"
        1 -> "Advance"
        2 -> "Expert"
        3 -> "Master"
        4 -> "Re: Master"
        else -> throw NumberFormatException("")
    }
}

suspend fun HttpResponse.bodyAsDocument(): Document {
    return Jsoup.parse(this.bodyAsText());
}

suspend fun HttpResponse.bodyAsJson(): JsonElement {
    return Json.parseToJsonElement(bodyAsText())
}