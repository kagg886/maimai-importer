package top.kagg886.maimai

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import top.kagg886.maimai.plugins.configureCORS
import top.kagg886.maimai.plugins.configureRouting
import top.kagg886.maimai.plugins.configureSockets

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureCORS()
    configureSockets()
    configureRouting()
}
