package top.kagg886.maimai.data

import kotlinx.serialization.Serializable

@Serializable
data class LogMessage(
    val msg:String,
    val level:Int
)

@Serializable
data class WechatShowImage(
    val uid:String?,
)

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
