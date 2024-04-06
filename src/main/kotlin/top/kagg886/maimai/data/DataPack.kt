package top.kagg886.maimai.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DataPack(val clazz: String, val data: String) {
    inline fun <reified T> content(): T {
        return Json.decodeFromString(data)
    }

    inline fun <reified T> contentOrNull(): T? {
        return kotlin.runCatching {
            Json.decodeFromString<T>(data)
        }.getOrNull()
    }

    fun isInstance(clazz: Class<*>): Boolean = clazz.name == this.clazz

    fun encode() = Json.encodeToString(this)

    companion object {
        inline fun <reified T> build(t: T): DataPack = DataPack(T::class.java.name, Json.encodeToString(t))

        fun decode(s: String): DataPack = Json.decodeFromString(s)
    }
}