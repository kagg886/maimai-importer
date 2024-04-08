package top.kagg886.maimai.upload

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import top.kagg886.maimai.util.toMaimaiDifficult
import top.kagg886.maimai.ws.Connection
import top.kagg886.maimai.ws.logInfo
import kotlin.time.measureTimedValue

class DivingFishUploadProtocol(conn0: Connection, config: DivingFishUploadConfig) :
    UploadProtocol<DivingFishUploadProtocol.DivingFishUploadConfig>(conn0, config) {

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

    override suspend fun upload(maimaiClient: HttpClient) {
        coroutineScope {
            config.diff.map {
                val diff = it.toMaimaiDifficult()
                async {
                    conn.logInfo("开始获取${diff}难度数据")
                    val (value, time) = measureTimedValue {
                        maimaiClient.get("https://maimai.wahlap.com/maimai-mobile/record/musicGenre/search/?genre=99&diff=$it")
                            .bodyAsText()
                    }
                    conn.logInfo("获取${diff}难度数据完成!用时:${time}")

                    net.post("https://www.diving-fish.com/api/pageparser/page") {
                        contentType(ContentType.Text.Plain)
                        setBody("<login><u>${config.username}</u><p>${config.password}</p></login>".trimIndent() + value)
                    }

                    conn.logInfo("上传${diff}难度数据完成!")
                }
            }.awaitAll()
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