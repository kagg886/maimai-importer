package top.kagg886.maimai.upload

import kotlinx.serialization.Serializable

abstract class UploadProtocol<T>(val config: T) {
    abstract suspend fun auth()
    abstract suspend fun upload(diff: Int, sourceHTML: String)
}

@Serializable
data class ProtocolDescription(
    val name: String,
    val url: String,
    val require: List<RequireArgs>,
    val className:String
)

@Serializable
data class RequireArgs(
    val name: String,
    val type: Int,
) {
    constructor(name: String, type: RequireArgType) : this(name, type.i)
}

enum class RequireArgType(val i: Int) {
    TEXT(0), LIST(1);
}