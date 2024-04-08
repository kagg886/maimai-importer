package top.kagg886.maimai

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.kagg886.maimai.data.*
import top.kagg886.maimai.plugins.configureRouting
import top.kagg886.maimai.plugins.configureSockets
import top.kagg886.maimai.upload.*
import top.kagg886.maimai.ws.awaitNewMessage
import java.awt.Graphics
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

private val log = KtorSimpleLogger("Client")

class ApplicationTest {

    @Test
    fun testLxnsScoreBean() {
        val score = Score(
            id = 123,
            levelIndex = LevelIndex.MASTER,
            achievements = 100.4890f,
            fc = FCType.fcp,
            dxScore = 114514,
            type = SongType.standard
        )
        println(Json {
            encodeDefaults = true
        }.encodeToString(score))
    }

    @Test
    fun testRoot(): Unit = testApplication {
        application {
            configureSockets()
            configureRouting()
        }

        val client = createClient {
            install(WebSockets)
        }
        client.ws("/ws") {
            var frame: JFrame? = null
            for (dataPack in incoming) {
                if (dataPack !is Frame.Text) {
                    continue
                }
                val pack = DataPack.decode(dataPack.readText())
                when {
                    pack.isInstance(WechatShowImage::class.java) -> {
                        val url = pack.content<WechatShowImage>().uid
                        if (url == null) {
                            frame?.dispose()
                            continue
                        }
                        frame?.dispose()
                        val qr = ImageIO.read(
                            ByteArrayInputStream(
                                client.get("/img") {
                                    parameter("id", url)
                                }.readBytes()
                            )
                        )
                        frame = JFrame("扫描二维码")

                        frame.add(object : JPanel() {
                            override fun paintComponent(g: Graphics) {
                                super.paintComponent(g)
                                g.drawImage(qr, 0, 0, qr.width, qr.height, null)
                            }
                        })
                        frame.setSize(qr.width + 40, qr.height + 40)
                        frame.isVisible = true
                    }

                    pack.isInstance(LogMessage::class.java) -> {
                        println(pack.content<LogMessage>().msg)
                    }

                    pack.isInstance(RequireMessage::class.java) -> {
                        when (pack.content<RequireMessage>().msg) {
                            RequireMessageInfo.PROTOCOL_CONFIG -> {
                                outgoing.send(
                                    Frame.Text(
                                        DataPack.build(
                                            DivingFishUploadProtocol.DivingFishUploadConfig(
                                                "iveour@163.com",
                                                "baleitem103",
                                                listOf(3, 4)
                                            )
                                        ).encode()
                                    )
                                )
                            }

                            RequireMessageInfo.WECHAT_UID -> {

                            }
                        }
                    }
                }
            }
        }

//        client.get("/").apply {
//            assertEquals(HttpStatusCode.OK, status)
//            assertEquals("Hello World!", bodyAsText())
//        }
    }

    @Test
    fun buildConfig() {
        println(
            DataPack.build(
                DivingFishUploadProtocol.DivingFishUploadConfig(
                    "root",
                    "123456",
                    listOf(3, 4)
                )
            ).encode()
        )
    }
    @Test
    fun testFlow(): Unit = runBlocking {
        //MutableSharedFlow的first()会等待新消息，但是MutableStateFlow不会
        val flow = MutableSharedFlow<Int>()
        launch {
            delay(1000)
            flow.emit(1)
            delay(1000)
            flow.emit(2)
            delay(5000)
        }
        println(flow.first())
        println(flow.first())
        println(flow.first())
        println(flow.first())
        println(flow.first())
    }

    @Test
    fun testAwaitNewMsg(): Unit = runBlocking {
        val flow = MutableSharedFlow<String>()
        launch {
            flow.emit("1")
            delay(1000)
            flow.emit("2")
            delay(1000)
            flow.emit("3")
            delay(1000)
            flow.emit("4")
            delay(10000)
        }

        for (i in 1..4) {
            println(flow.awaitNewMessage(timeout = 1.seconds))
        }
    }
}
