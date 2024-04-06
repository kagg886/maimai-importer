package top.kagg886.maimai.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import top.kagg886.maimai.upload.DivingFishUploadProtocol
import java.util.*

val imgCache = mutableMapOf<String, ByteArray>()

fun buildImg(img: ByteArray): String {
    val uuid = UUID.randomUUID().toString().replace("-","")
    imgCache[uuid] = img
    return uuid
}

fun Application.configureRouting() {
    routing {
        get("/config") {
            call.respond(
                listOf(
                    DivingFishUploadProtocol.getDescription()
                )
            )
        }

        get("/img") {
            val img = imgCache[call.parameters["id"]]
            if (img == null) {
                call.respond(404)
                return@get
            }
            call.respondBytes(img, ContentType.Image.JPEG)
        }
    }
}
