package top.kagg886.maimai.ws

import io.ktor.client.plugins.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import top.kagg886.maimai.data.*
import top.kagg886.maimai.plugins.MAIMAI_ERROR
import top.kagg886.maimai.plugins.MAIMAI_SUCCESS
import top.kagg886.maimai.plugins.buildImg
import top.kagg886.maimai.plugins.list
import top.kagg886.maimai.upload.DivingFishUploadProtocol
import top.kagg886.maimai.upload.LxnsUploadProtocol
import top.kagg886.maimai.util.Statics
import top.kagg886.maimai.weixin.WechatSession
import top.kagg886.maimai.weixin.WechatSessionLoginListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Connection(private var session: DefaultWebSocketSession) {
    private val flow = MutableSharedFlow<DataPack>()
    var wxUin: Long = -1

    suspend fun emit(s: DataPack) {
        flow.emit(s)
    }

    suspend fun sendPacket(pack: DataPack) {
        if (session.isActive) {
            session.send(pack.encode())
        }
    }


    private val scope =
        CoroutineScope(Dispatchers.IO) + CoroutineName("ConnectionManager") + CoroutineExceptionHandler { _, throwable ->
            CoroutineScope(Dispatchers.IO).launch {
                logError("协议出错，自动关闭连接", throwable)
                if (this@Connection.wxUin != -1L) {
                    session.close(MAIMAI_ERROR("未catch的协程错误"))
                    return@launch
                }
                session.close(MAIMAI_SUCCESS("未catch的协程错误"))
            }
        }

    init {
        scope.launch {
            logInfo("连接成功，本次会话id为：${hashCode()}")

            logInfo("等待客户端推送查分器配置...(3s)")

            sendPacket(DataPack.build(RequireMessage(RequireMessageInfo.PROTOCOL_CONFIG)))
            val protocol = flow.awaitNewMessage(timeout = 3.seconds)?.let {
                when {
                    it.isInstance(DivingFishUploadProtocol.DivingFishUploadConfig::class.java) -> {
                        DivingFishUploadProtocol(this@Connection, it.content())
                    }

                    it.isInstance(LxnsUploadProtocol.LxnsUploadConfig::class.java) -> {
                        LxnsUploadProtocol(this@Connection, it.content())
                    }

                    else -> null
                }
            }
            if (protocol == null) {
                logError("查分器配置获取超时或配置不正确")
                session.close(MAIMAI_SUCCESS("查分器配置获取超时或配置不正确"))
                return@launch
            }
            logInfo("查分器配置获取成功: ${protocol::class.java.name}")
            runCatching {
                protocol.auth()
            }.onFailure {
                logError("查分器配置验证失败: ${it.message}")
                session.close(MAIMAI_SUCCESS("查分器配置验证失败: ${it.message}"))
                return@launch
            }
            logInfo("查分器配置验证成功")

            logInfo("等待推送微信uid...(3s)")
            sendPacket(DataPack.build(RequireMessage(RequireMessageInfo.PROTOCOL_CONFIG)))
            flow.awaitNewMessage(timeout = 3.seconds)?.apply {
                val store = contentOrNull<SessionRestore>()
                if (store == null) {
                    logWarn("微信uid未推送，将作为新会话开始导入流程")
                    return@apply
                }

                val uin = store.id
                list.find { it.wxUin == uin }?.let {
                    //当前微信用户已登录，转接session并终止该函数
                    it.session.apply {
                        if (isActive) {
                            it.session.close(MAIMAI_ERROR("只能同时有一个用户绑定socket"))
                        }
                    }
                    it.session = session
                    list.remove(this@Connection)
                    logInfo("当前用户已登录微信，日志已转接到会话id: ${it.hashCode()}")
                    return@launch
                }
            }
            //微信二维码登录
            val wx = WechatSession.createNewSession(object : WechatSessionLoginListener {
                override suspend fun onQRCodeReceived(bytes: ByteArray) {
                    logInfo("成功获取二维码")

                    sendPacket(DataPack.build(WechatShowImage(buildImg(bytes))))
                }

                override suspend fun onQRCodeScanned(bytes: ByteArray) {
                    logInfo("二维码已扫描")
                    sendPacket(DataPack.build(WechatShowImage(buildImg(bytes))))
                }

                override suspend fun onLoginComplete(success: Boolean, message: String) {
                    if (!success) {
                        logError("微信扫码登录失败:${message}")
                    }
                }

                override suspend fun onInterval() = Unit
            })
            if (wx == null) {
                if (session.isActive) {
                    session.close(MAIMAI_SUCCESS("微信扫码登录失败"))
                }
                //若用户放弃扫码，最终一定会执行到这里，因此需要在这里手动清理connection
                list.remove(this@Connection)
                return@launch
            }
            //传null代表不需要导入图片了
            sendPacket(DataPack.build(WechatShowImage(null)))
            Statics.staticScanSuccess()

            val uin = wx.currentUser!!.uin
            list.find { it.wxUin == uin }?.let {
                //当前微信用户已登录，转接session并终止该函数
                it.session.apply {
                    if (isActive) {
                        it.session.close(MAIMAI_ERROR("只能同时有一个用户绑定socket"))
                    }
                }
                it.session = session
                list.remove(this@Connection)
                logInfo("当前用户已登录微信，日志已转接到会话id: ${it.hashCode()}")
                return@launch
            }

            this@Connection.wxUin = uin
            logInfo("开始登录舞萌net")
            val maimai = wx.doOAuth("https://tgk-wcaime.wahlap.com/wc_auth/oauth/authorize/maimai-dx") {
                install(HttpTimeout) {
                    requestTimeoutMillis = 10.minutes.inWholeMilliseconds
                    connectTimeoutMillis = 10.minutes.inWholeMilliseconds
                    socketTimeoutMillis = 10.minutes.inWholeMilliseconds
                }
            }
            wx.logout()
            logInfo("舞萌net登录完毕，微信已退出")
            runCatching {
                protocol.upload(maimai)
            }.onFailure {
                logError("maimai数据更新失败", it)
                session.close(MAIMAI_SUCCESS("maimai数据更新失败:${it.message}"))
                return@launch
            }
            logInfo("maimai数据更新完成!")
            session.close(MAIMAI_SUCCESS("maimai数据更新完毕"))
            Statics.staticImportSuccess()
        }
    }
}

suspend fun <T> MutableSharedFlow<T>.awaitNewMessage(timeout: Duration = Duration.INFINITE): T? =
    withTimeoutOrNull(timeout) {
        first()
    }

private val log = KtorSimpleLogger("WebSocketConnection")
suspend fun Connection.logInfo(msg: String) {
    sendPacket(DataPack.build(LogMessage(msg, 1)))
    log.info("user:${hashCode()} -> $msg")
}

suspend fun Connection.logWarn(msg: String) {
    sendPacket(DataPack.build(LogMessage(msg, 2)))
    log.warn("user:${hashCode()} -> $msg")
}

suspend fun Connection.logError(msg: String, ex: Throwable? = null) {
    sendPacket(DataPack.build(LogMessage(msg.let {
        return@let it + (ex?.stackTraceToString()?.let { err ->
            "\n$err"
        } ?: "")
    }, 3)))
}