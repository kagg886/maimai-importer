package top.kagg886.maimai.data

import kotlinx.serialization.Serializable

@Serializable
data class LogMessage(
    val msg:String,
    val level:Int
)

@Serializable
data class WechatShowImage(
    val bytes:ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WechatShowImage

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

@Serializable
data class SessionRestore(val id:Long)

@Serializable
data class RequireMessage(
    val msg:RequireMessageInfo
)

enum class RequireMessageInfo {
    PROTOCOL_CONFIG,
    WECHAT_UID
}
