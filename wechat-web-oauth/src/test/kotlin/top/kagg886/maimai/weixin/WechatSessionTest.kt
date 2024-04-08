package top.kagg886.maimai.weixin

import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.awt.Graphics
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.reflect.jvm.isAccessible
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.minutes


class WechatSessionTest {

    var frame: JFrame? = null

    @Test
    fun testCreateSession(): Unit = runBlocking {
        val session = WechatSession.createNewSession(object : WechatSessionLoginListener {
            override suspend fun onQRCodeReceived(bytes: ByteArray) {
                frame?.dispose()
                val qr = ImageIO.read(ByteArrayInputStream(bytes))
                frame = JFrame("扫描二维码")
                frame!!.add(object : JPanel() {
                    override fun paintComponent(g: Graphics) {
                        super.paintComponent(g)
                        g.drawImage(qr, 0, 0, qr.width, qr.height, null)
                    }
                })
                frame!!.setSize(qr.width + 40, qr.height + 40)
                frame!!.isVisible = true
            }

            override suspend fun onQRCodeScanned(bytes: ByteArray) {
                frame?.dispose()
                val qr = ImageIO.read(ByteArrayInputStream(bytes))
                frame = JFrame("头像")
                frame!!.add(object : JPanel() {
                    override fun paintComponent(g: Graphics) {
                        super.paintComponent(g)
                        g.drawImage(qr, 0, 0, qr.width, qr.height, null)
                    }
                })
                frame!!.setSize(qr.width + 40, qr.height + 40)
                frame!!.isVisible = true
            }

            override suspend fun onLoginComplete(success: Boolean, message: String) {
                frame?.dispose()
                println("二维码扫描结果:$success, $message")
            }

            override suspend fun onInterval() = Unit
        })

        assertNotNull(session)

        val store = AcceptAllCookiesStorage()
        session.doOAuth(
            auth = "https://tgk-wcaime.wahlap.com/wc_auth/oauth/authorize/maimai-dx",
            store = store
        ) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10.minutes.inWholeMilliseconds
                connectTimeoutMillis = 10.minutes.inWholeMilliseconds
                socketTimeoutMillis = 10.minutes.inWholeMilliseconds
            }

//            install(Logging) {
//                logger = Logger.DEFAULT
//                level = LogLevel.HEADERS
//            }
        }
        val field = AcceptAllCookiesStorage::class.members.find { it.name == "container" }!!.run {
            isAccessible = true
            call(store) as MutableList<Cookie>
        }
        println(field.joinToString(","))
        session.logout()
    }
}

