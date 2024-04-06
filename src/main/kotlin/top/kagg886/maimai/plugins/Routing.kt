package top.kagg886.maimai.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import top.kagg886.maimai.upload.DivingFishUploadProtocol

fun Application.configureRouting() {
    routing {
        get("/config") {
            call.respond(
                listOf(
                    DivingFishUploadProtocol.getDescription()
                )
            )
        }
    }
}
