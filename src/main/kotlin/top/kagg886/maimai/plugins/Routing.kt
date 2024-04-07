package top.kagg886.maimai.plugins

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.kagg886.maimai.upload.DivingFishUploadProtocol
import top.kagg886.maimai.util.Statics
import java.util.*
import kotlin.time.Duration

val imgCache = mutableMapOf<String, ByteArray>()

private val scope = CoroutineScope(Dispatchers.IO)

fun buildImg(img: ByteArray): String {
    val uuid = UUID.randomUUID().toString().replace("-", "")
    imgCache[uuid] = img
    scope.launch { //15min后删除图像
        delay(Duration.parse("15m"))
        imgCache.remove(uuid)
    }
    return uuid
}


fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        singlePageApplication {
            useResources = true
            filesPath = "fronted"
            defaultPage = "index.html"
        }
    }

    routing {
        get("/static") {
            call.respond(
                mapOf(
                    "connection" to Statics.connection,
                    "scan" to Statics.scanSuccess,
                    "import" to Statics.importSuccess,
                )
            )
        }
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
