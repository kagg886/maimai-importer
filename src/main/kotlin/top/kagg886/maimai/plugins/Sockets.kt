package top.kagg886.maimai.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import top.kagg886.maimai.data.DataPack
import top.kagg886.maimai.util.Statics
import top.kagg886.maimai.ws.Connection
import java.time.Duration
import java.util.*

val list = Collections.synchronizedSet<Connection>(LinkedHashSet())

val logger = KtorSimpleLogger("CloseReason")

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {

        webSocket("/ws") {
            val session = Connection(this)
            list.add(session)
            Statics.staticConnection()
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    println(frame.readText())
                    session.emit(DataPack.decode(frame.readText()))
                }
            }
            val cause = this.closeReason.await()
            logger.info("user:${session.hashCode()} -> 会话断开链接(${cause?.code})，原因:${cause?.message}")
            if (cause!!.code == 4001.toShort()) {
                list.remove(session)
            }
        }
    }
}
//非正常退出，不要移除会话
val MAIMAI_ERROR = { msg: String ->
    CloseReason(4000, msg)
}

//正常退出，可以移除会话
val MAIMAI_SUCCESS = { msg: String ->
    CloseReason(4001, msg)
}