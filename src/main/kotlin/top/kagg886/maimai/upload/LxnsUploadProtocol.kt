package top.kagg886.maimai.upload

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import top.kagg886.maimai.util.bodyAsDocument
import top.kagg886.maimai.util.bodyAsJson
import top.kagg886.maimai.util.toMaimaiDifficult
import top.kagg886.maimai.ws.Connection
import top.kagg886.maimai.ws.logInfo
import top.kagg886.maimai.ws.logWarn
import kotlin.time.measureTimedValue


private val songMap: MutableMap<String, Int> = mutableMapOf()

class LxnsUploadProtocol(conn0: Connection, config: LxnsUploadConfig) :
    UploadProtocol<LxnsUploadProtocol.LxnsUploadConfig>(conn0, config) {

    @Serializable
    data class LxnsUploadConfig(
        val diff: List<Int>,
    )

    private val net = HttpClient {
        install(DefaultRequest) {
            headers {
                header("Authorization", "YVwMADiPN9Av2K3MtsqKrIiV_lX4grg3rlKbYSA7-i0=")
            }
        }

        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.DEFAULT
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }


    override suspend fun auth() = conn.logWarn("落雪查分器不支持在查分器配置验证阶段Auth，该步骤跳过")


    override suspend fun upload(maimaiClient: HttpClient) {
        coroutineScope {
            if (songMap.isEmpty()) {
                net.get("https://maimai.lxns.net/api/v0/maimai/song/list")
                    .bodyAsJson().jsonObject["songs"]!!.jsonArray.map {
                    //{
                    //            "id": 8,
                    //            "title": "True Love Song",
                    //            "artist": "Kai/クラシック「G線上のアリア」",
                    //            "genre": "maimai",
                    //            "bpm": 150,
                    //            "version": 10000,
                    val song = it.jsonObject
                    return@map song["title"]!!.jsonPrimitive.content to song["id"]!!.jsonPrimitive.int
                }.toMap().apply {
                    songMap.putAll(this)
                }
            }

            val doc =
                maimaiClient.get("https://maimai.wahlap.com/maimai-mobile/friend/userFriendCode/").bodyAsDocument()
            val friendCode = doc.getElementsByClass("see_through_block m_t_5 m_b_5 p_5 t_c f_15")[0].text()
            conn.logInfo("您的friend-code为：${friendCode}")

            //权限验证
            //TODO 待测试
            val playerInfo = net.get("https://maimai.lxns.net/api/v0/maimai/player/${friendCode}").bodyAsJson()
            check(playerInfo.jsonObject["success"]!!.jsonPrimitive.boolean) {
                "获取玩家信息失败:${playerInfo.jsonObject["message"]!!.jsonPrimitive.content}"
            }
            val structure = playerInfo.jsonObject["data"]!!.jsonObject
            val permissionResult = net.post("https://maimai.lxns.net/api/v0/maimai/player") {
                contentType(ContentType.Application.Json)
                setBody(structure.toString())
            }.body<LxnsAPIResult>()
            check(permissionResult.success) {
                "请在落雪查分器的设置页面开启：允许第三方写入数据权限！"
            }

            net.get("https://maimai.lxns.net/api/v0/maimai/player/${friendCode}").body<LxnsAPIResult>().apply {
                check(success) {
                    """
                        无法获取好友码对应的游戏数据:${message}
                        1. 落雪必须要先使用官方的代理工具导入后，才能使用该工具的二次导入
                        2. 可能是舞萌net网页问题，请稍后再试
                        3. 落雪查分器的api-key配置错误，请联系网站管理员
                    """.trimIndent()
                }
            }
            conn.logInfo("成功查找到落雪查分器好友码对应的user数据，现在开始准备导入")

            config.diff.map {
                val diff = it.toMaimaiDifficult()
                async {
                    //TODO 待测试
                    conn.logInfo("开始获取${diff}难度数据")
                    val (value, time) = measureTimedValue {
                        maimaiClient.get("https://maimai.wahlap.com/maimai-mobile/record/musicGenre/search/?genre=99&diff=$it")
                            .bodyAsDocument()
                    }
                    conn.logInfo("获取${diff}难度数据完成!用时:${time}")

                    val uploadResult = net.post("https://maimai.lxns.net/api/v0/maimai/player/${friendCode}/scores") {
                        contentType(ContentType.Application.Json)
                        val list: List<Score> =
                            value.getElementsByClass("w_450 m_15 p_r f_0").toList().mapNotNull { ele ->
                                val achievements = ele.getElementsByClass("music_score_block w_120 t_r f_l f_12").let {
                                    if (it.size == 0) {
                                        return@let null //过滤未游玩曲目
                                    }
                                    return@let it[0].text().replace("%", "").toFloat()
                                }
                                //id	int	曲目 ID
                                //level_index	LevelIndex	难度
                                //achievements	float	达成率 √
                                //fc	FCType	值可空，FULL COMBO 类型 √
                                //fs	FSType	值可空，FULL SYNC 类型 √
                                //dx_score	int	DX 分数 √
                                //type	SongType	谱面类型 √
                                if (achievements == null) {
                                    return@mapNotNull null
                                }
                                val status = ele.getElementsByClass("h_30 f_r")

                                //如此过滤，必有一个值
                                val fsType = status.mapNotNull {
                                    //https://maimai.wahlap.com/maimai-mobile/img/music_icon_back.png?ver=1.30
                                    val fsStr = it.attr("src")
                                        .replace("https://maimai.wahlap.com/maimai-mobile/img/music_icon_", "")
                                        .split(".")[0]
                                    if (fsStr == "back") return@mapNotNull null
                                    FSType.entries.find { it.name == fsStr }
                                }.let {
                                    if (it.isEmpty()) null else it[0]
                                }

                                val fcType = status.mapNotNull {
                                    //https://maimai.wahlap.com/maimai-mobile/img/music_icon_back.png?ver=1.30
                                    val fcStr = it.attr("src")
                                        .replace("https://maimai.wahlap.com/maimai-mobile/img/music_icon_", "")
                                        .split(".")[0]
                                    if (fcStr == "back") return@mapNotNull null
                                    FCType.entries.find { it.name == fcStr }
                                }.let {
                                    if (it.isEmpty()) null else it[0]
                                }

                                val dxScore = runCatching {
                                    ele.getElementsByClass("music_score_block w_180 t_r f_l f_12").text()
                                        .replace(" ", "")
                                        .replace(",", "")
                                        .split("/")[0].toInt()
                                }.onFailure {
                                    println(it)
                                }.getOrThrow()
                                val songName = ele.getElementsByClass("music_name_block t_l f_13 break").text()

                                //https://maimai.wahlap.com/maimai-mobile/img/music_dx.png
                                //https://maimai.wahlap.com/maimai-mobile/img/music_standard.png
                                val songType = if (ele.toString()
                                        .contains("https://maimai.wahlap.com/maimai-mobile/img/music_dx.png")
                                ) SongType.dx else SongType.standard

                                val levelIndex = LevelIndex.entries.find { s -> s.diff == it }!!


                                //TODO 关于DON't STOP ROCK IN，net返回数据中分割为1空格，落雪为2空格
                                return@mapNotNull Score(
                                    id = songMap[songName.let {
                                        if (it == "D✪N’T ST✪P R✪CKIN’") {
                                            return@let "D✪N’T  ST✪P  R✪CKIN’"
                                        }
                                        return@let it
                                    }].let {
                                        it!!
                                    },
                                    levelIndex = levelIndex,
                                    achievements = achievements,
                                    fc = fcType,
                                    fs = fsType,
                                    dxScore = dxScore,
                                    type = songType
                                )

                            }
                        setBody(
                            mapOf(
                                "scores" to list
                            )
                        )
                    }.body<LxnsAPIResult>()

                    check(uploadResult.success) {
                        "上传${diff}难度数据失败!: ${uploadResult.message}"
                    }

                    conn.logInfo("上传${diff}难度数据完成!")
                }
            }.awaitAll()


        }
    }

    companion object {
        fun getDescription() = ProtocolDescription(
            name = "落雪查分器",
            url = "https://maimai.lxns.net/",
            className = LxnsUploadConfig::class.java.name,
            require = listOf(
                RequireArgs(
                    name = "diff",
                    type = RequireArgType.LIST
                )
            )
        )
    }
}

@Serializable
data class LxnsAPIResult(
    val success: Boolean,
    val code: Int,
    val message: String? = null,
)


//Score
//游玩成绩
//
//字段名	类型	说明
//id	int	曲目 ID
//level_index	LevelIndex	难度
//achievements	float	达成率
//fc	FCType	值可空，FULL COMBO 类型
//fs	FSType	值可空，FULL SYNC 类型
//dx_score	int	DX 分数
//type	SongType	谱面类型
@Serializable
data class Score(
    val id: Int,
    @SerialName("level_index")
    val levelIndex: LevelIndex,
    val achievements: Float,
    val fc: FCType? = null,
    val fs: FSType? = null,
    @SerialName("dx_score")
    val dxScore: Int,
    val type: SongType,
)

//LevelIndex
//难度
//
//值	类型	说明
//0	int	BASIC
//1	int	ADVANCED
//2	int	EXPERT
//3	int	MASTER
//4	int	Re:MASTER

object LevelIndexSerializer : KSerializer<LevelIndex> {
    override val descriptor = PrimitiveSerialDescriptor("LevelIndex", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: LevelIndex) {
        encoder.encodeInt(value.diff)
    }

    override fun deserialize(decoder: Decoder): LevelIndex {
        val diff = decoder.decodeInt()
        return LevelIndex.values().find { it.diff == diff } ?: throw IllegalArgumentException("Invalid diff value")
    }
}

@Serializable(with = LevelIndexSerializer::class)
enum class LevelIndex(val diff: Int) {
    BASIC(0),
    ADVANCED(1),
    EXPERT(2),
    MASTER(3),
    RE_MASTER(4)
}

//FCType
//FULL COMBO 类型
//
//值	类型	说明
//app	string	AP+
//ap	string	AP
//fcp	string	FC+
//fc	string	FC
enum class FCType {
    app, ap, fcp, fc
}

//FSType
//FULL SYNC 类型
//
//值	类型	说明
//fsdp	string	FSD+
//fsd	string	FSD
//fsp	string	FS+
//fs	string	FS
enum class FSType {
    fsdp, fsd, fsp, fs
}

//SongType
//谱面类型
//
//值	类型	说明
//standard	string	标准谱面
//dx	string	DX 谱面
enum class SongType {
    standard, dx
}