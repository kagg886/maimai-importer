package top.kagg886.maimai.upload

import io.ktor.client.*
import kotlinx.serialization.Serializable
import top.kagg886.maimai.ws.Connection

abstract class UploadProtocol<T>(val conn: Connection, val config: T) {
    abstract suspend fun auth()
    abstract suspend fun upload(maimaiClient: HttpClient)
}

@Serializable
data class ProtocolDescription(
    val name: String,
    val url: String,
    val require: List<RequireArgs>,
    val className: String,
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