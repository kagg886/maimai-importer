package top.kagg886.maimai.upload

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

class DivingFishUploadProtocol(config: DivingFishUploadConfig) :
    UploadProtocol<DivingFishUploadProtocol.DivingFishUploadConfig>(config) {

    @Serializable
    data class DivingFishUploadConfig(
        val username: String,
        val password: String,
        val diff: List<Int>,
    )

    private val net = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }

        install(ContentNegotiation) {
            json()
        }

//        install(Logging) {
//            level = LogLevel.BODY
//            logger = Logger.DEFAULT
//        }
    }

    override suspend fun auth() {
        val resp = net.post("https://www.diving-fish.com/api/maimaidxprober/login") {
            contentType(ContentType.Application.Json)
            setBody(
                DivingFishUploadRequest(
                    config.username,
                    config.password
                )
            )
        }.body<DivingFishUploadResponse>()
        if (resp.errcode != 0) {
            throw IllegalArgumentException(resp.message)
        }
    }

    override suspend fun upload(diff: Int, sourceHTML: String) {
        net.post("https://www.diving-fish.com/api/pageparser/page") {
            contentType(ContentType.Text.Plain)
            setBody("<login><u>${config.username}</u><p>${config.password}</p></login>".trimIndent() + sourceHTML)
        }
    }

    companion object {
        fun getDescription() = ProtocolDescription(
            name = "水鱼查分器",
            url = "https://www.diving-fish.com/maimaidx/prober/",
            className = DivingFishUploadConfig::class.java.name,
            require = listOf(
                RequireArgs(
                    name = "username",
                    type = RequireArgType.TEXT
                ),
                RequireArgs(
                    name = "password",
                    type = RequireArgType.TEXT
                ),
                RequireArgs(
                    name = "diff",
                    type = RequireArgType.LIST
                )
            )
        )
    }
}

@Serializable
data class DivingFishUploadRequest(val username: String, val password: String)

@Serializable
data class DivingFishUploadResponse(
    val errcode: Int = 0,
    val message: String,
)