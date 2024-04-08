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

val appConfig by lazy {
    val configFile = File("application.conf")
    if (configFile.exists()) {
        HoconApplicationConfig(ConfigFactory.parseFile(configFile))
    } else {
        HoconApplicationConfig(ConfigFactory.load())
    }
}

val lxnsKey = checkNotNull(appConfig.propertyOrNull("application.lxns.apiKey")) {
    "配置：application.lxns.apiKey 不得为空！"
}

fun main() {
    embeddedServer(Netty, host = appConfig.host, port = appConfig.port, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureCORS()
    configureSockets()
    configureRouting()
}
