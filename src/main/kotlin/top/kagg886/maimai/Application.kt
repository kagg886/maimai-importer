package top.kagg886.maimai

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import top.kagg886.maimai.plugins.configureCORS
import top.kagg886.maimai.plugins.configureRouting
import top.kagg886.maimai.plugins.configureSockets
import java.io.File

fun main() {
    val configFile = File("application.conf")
    val config = if (configFile.exists()) {
        HoconApplicationConfig(ConfigFactory.parseFile(configFile))
    } else {
        HoconApplicationConfig(ConfigFactory.load())
    }

    val host = config.property("ktor.deployment.host").getString()
    val port = config.property("ktor.deployment.port").getString().toInt()

    embeddedServer(Netty, host = host, port = port, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureCORS()
    configureSockets()
    configureRouting()
}
