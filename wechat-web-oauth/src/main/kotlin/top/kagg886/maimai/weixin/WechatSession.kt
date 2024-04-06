package top.kagg886.maimai.weixin

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import org.json.XML
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern


@Serializable
//    private String pass_ticket;
//    private String wxsid;
//    private String skey;
//    private long wxuin;
data class LoginInfo(
    @SerialName("pass_ticket") val passTicket: String,
    @SerialName("wxsid") val wxSid: String,
    @SerialName("skey") val sKey: String,
    @SerialName("wxuin") val uin: Int,
)

@Serializable
data class User(
    @SerialName("UserName") val username: String,
    @SerialName("Uin") val uin: Long,
    @SerialName("HeadImgUrl") val avatar: String,
)


class WechatSession private constructor(val storage: CookiesStorage) {
    var info: LoginInfo? = null
    var currentUser: User? = null

    fun isInited(): Boolean = info != null && currentUser != null

    internal val net = HttpClient(CIO) {
        followRedirects = false

        defaultRequest {
            headers {
                header("Client-Version", "2.0.0")
                header(
                    "Extspam",
                    "Go8FCIkFEokFCggwMDAwMDAwMRAGGvAESySibk50w5Wb3uTl2c2h64jVVrV7gNs06GFlWplHQbY/5FfiO++1yH4ykCyNPWKXmco+wfQzK5R98D3so7rJ5LmGFvBLjGceleySrc3SOf2Pc1gVehzJgODeS0lDL3/I/0S2SSE98YgKleq6Uqx6ndTy9yaL9qFxJL7eiA/R3SEfTaW1SBoSITIu+EEkXff+Pv8NHOk7N57rcGk1w0ZzRrQDkXTOXFN2iHYIzAAZPIOY45Lsh+A4slpgnDiaOvRtlQYCt97nmPLuTipOJ8Qc5pM7ZsOsAPPrCQL7nK0I7aPrFDF0q4ziUUKettzW8MrAaiVfmbD1/VkmLNVqqZVvBCtRblXb5FHmtS8FxnqCzYP4WFvz3T0TcrOqwLX1M/DQvcHaGGw0B0y4bZMs7lVScGBFxMj3vbFi2SRKbKhaitxHfYHAOAa0X7/MSS0RNAjdwoyGHeOepXOKY+h3iHeqCvgOH6LOifdHf/1aaZNwSkGotYnYScW8Yx63LnSwba7+hESrtPa/huRmB9KWvMCKbDThL/nne14hnL277EDCSocPu3rOSYjuB9gKSOdVmWsj9Dxb/iZIe+S6AiG29Esm+/eUacSba0k8wn5HhHg9d4tIcixrxveflc8vi2/wNQGVFNsGO6tB5WF0xf/plngOvQ1/ivGV/C1Qpdhzznh0ExAVJ6dwzNg7qIEBaw+BzTJTUuRcPk92Sn6QDn2Pu3mpONaEumacjW4w6ipPnPw+g2TfywJjeEcpSZaP4Q3YV5HG8D6UjWA4GSkBKculWpdCMadx0usMomsSS/74QgpYqcPkmamB4nVv1JxczYITIqItIKjD35IGKAUwAA=="
                )
            }
        }


        install(HttpCookies) {
            storage = this@WechatSession.storage
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }

//        install(Logging) {
//            logger = Logger.DEFAULT
//            level = LogLevel.HEADERS
//        }
    }

    suspend fun logout() {
        check(isInited()) { "this account was logout" }
        net.post("https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxlogout?redirect=1&type=0&skey=${info!!.sKey}") {
            contentType(ContentType.Application.Json)
            setBody(
                JSONObject().apply {
                    put("uin", info!!.uin)
                    put("sid", info!!.wxSid)
                }.toString()
            )
        }
        info = null
        currentUser = null
    }

    suspend fun doOAuth(
        auth: String,
        store: CookiesStorage = AcceptAllCookiesStorage(),
        builder: HttpClientConfig<CIOEngineConfig>.() -> Unit = {},
    ): HttpClient { //返回新的HttpClient
        val info = info!!
        val currentUser = currentUser!!
        val oauth = net.get(auth).headers["Location"] //根据oauth连接获取location地址
        check((oauth ?: "").startsWith("https://open.weixin.qq.com/connect/oauth2/authorize")) { "not a oauth link" }

        val poc1 = net.get(
            "https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxcheckurl?requrl=${
                URLEncoder.encode(
                    oauth, StandardCharsets.UTF_8
                )
            }&skey=${info.sKey}&pass_ticket=${info.passTicket}&opcode=2&scene=1&username=${currentUser.username}"
        ).headers["Location"]!!

        val client = HttpClient(CIO) {
            install(HttpCookies) {
                storage = store
            }
            builder(this)
        }
        client.get(poc1)
        return client
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        //创建一个Session，并阻塞到扫码登录完成或失败，成功返回session，失败返回null
        suspend fun createNewSession(solver: WechatSessionLoginListener, replayCount: Int = 3): WechatSession? {
            var replay = 0
            while (replay < replayCount) {
                val session = WechatSession(AcceptAllCookiesStorage())
                var uuid = session.getUUID()
                val bytes = session.net.get("https://login.weixin.qq.com/qrcode/$uuid").readBytes()
                solver.onQRCodeReceived(bytes)

                var confirmCalled = false
                while (true) {
                    solver.onInterval()
                    //200: 成功
                    //201：扫描成功，但未点确认
                    //408：未扫描
                    //400：未知
                    //500： login poll srv exception
                    //<注>: 400时网页会刷新整个页面，意为二维码已过期
                    val (msg, code) = session.fetchStatus(uuid)
                    when (code) {
                        200 -> {
                            val target: String = msg[1].split("=", limit = 2)[1].split("\"")[1]
                            val info = json.decodeFromString<LoginInfo>(
                                XML.toJSONObject(
                                    session.net.get("$target&fun=new&version=v2&mod=desktop&lang=zh_CN").bodyAsText()
                                ).getJSONObject("error").toString()
                            )
                            session.info = info


                            val user = session.net.post(
                                "https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxinit?r=${
                                    Random(900000000 - 1).nextInt(100000000)
                                }&pass_ticket=" + info.passTicket
                            ) {
                                contentType(ContentType.Application.Json)
                                setBody(
                                    JSONObject().apply {
                                        put("BaseRequest", JSONObject().apply {
                                            put("uin", info.uin)
                                            put("Sid", info.wxSid)
                                            put("Skey", "")
                                            put("DeviceID", session.randomDeviceID())
                                        })
                                    }.toString()
                                )
                            }

                            session.currentUser = user.bodyAsText().run {
                                val ele = json.parseToJsonElement(this).jsonObject["User"]!!
                                json.decodeFromJsonElement<User>(ele)
                            }

                            solver.onLoginComplete(true, "")
                            return session
                        }

                        201 -> {
                            if (!confirmCalled) {
                                val b64: String = Pattern.compile("window.userAvatar='(.*)';").matcher(msg[1]).run {
                                    find()
                                    group(1).replace("data:img/jpg;base64,", "")
                                }
                                solver.onQRCodeScanned(Base64.getDecoder().decode(b64))
                                confirmCalled = true
                            }
                        }

                        400,500 -> {
                            replay++
                            break
                        }

//                        500 -> {
//                            solver.onLoginComplete(false, "login poll srv exception")
//                        }
                    }
                }
            }
            solver.onLoginComplete(false, "长时间未通过二维码验证")
            return null
        }
    }
}

private suspend fun WechatSession.getUUID(): String {
    val str =
        net.get("https://login.wx2.qq.com/jslogin?appid=wx782c26e4c19acffb&redirect_uri=https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage?mod=desktop&fun=new&lang=zh_CN&_=${System.currentTimeMillis()}")
            .bodyAsText()

    return Pattern.compile("window.QRLogin.code = 200; window.QRLogin.uuid = \"(.*)\";").matcher(str).run {
        find()
        group(1)
    }
}

private suspend fun WechatSession.fetchStatus(uuid: String): Pair<List<String>, Int> {
    net.get("https://login.wx2.qq.com/cgi-bin/mmwebwx-bin/login?loginicon=true&uuid=${uuid}&tip=1&appid=wx782c26e4c19acffb&_=${System.currentTimeMillis()}")
        .bodyAsText().run {
            val msg = replace(" ", "").split(";", limit = 2)
            return msg to msg[0].split("=")[1].toInt()
        }
}

private fun WechatSession.randomDeviceID(): String = "e" + String.format("%.15f", Math.random()).substring(2, 17)

interface WechatSessionLoginListener {
    suspend fun onQRCodeReceived(bytes: ByteArray)
    suspend fun onQRCodeScanned(bytes: ByteArray)
    suspend fun onLoginComplete(success: Boolean, message: String)
    suspend fun onInterval()
}