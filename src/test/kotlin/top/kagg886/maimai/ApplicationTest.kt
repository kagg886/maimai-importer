package top.kagg886.maimai

import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import top.kagg886.maimai.data.*
import top.kagg886.maimai.plugins.configureRouting
import top.kagg886.maimai.plugins.configureSerialization
import top.kagg886.maimai.plugins.configureSockets
import top.kagg886.maimai.upload.DivingFishUploadProtocol
import top.kagg886.maimai.ws.awaitNewMessage
import java.awt.Graphics
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.time.Duration

private val log = KtorSimpleLogger("Client")

class ApplicationTest {
    @Test
    fun testRoot(): Unit = testApplication {
        application {
            configureSockets()
            configureSerialization()
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
                        val bytes = pack.content<WechatShowImage>().bytes
                        if (bytes == null) {
                            frame?.dispose()
                            continue
                        }
                        frame?.dispose()
                        val qr = ImageIO.read(ByteArrayInputStream(bytes))
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
                                                "root",
                                                "123456",
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
    fun testDivingFishProtocol(): Unit = runBlocking {
        val protocol = DivingFishUploadProtocol(
            DivingFishUploadProtocol.DivingFishUploadConfig(
                "root", "123456",
                listOf(1, 2, 3)
            )
        )
        try {
            protocol.auth()
            throw Exception()
        } catch (ignored: IllegalArgumentException) {
        }
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
            println(flow.awaitNewMessage(timeout = Duration.parse("1s")))
        }
    }
}
