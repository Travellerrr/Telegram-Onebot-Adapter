package cn.travellerr.onebotApi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMessage(
    @SerialName("time")
    val time: Long,

    @SerialName("self_id")
    val self_id: Long,

    @SerialName("post_type")
    val post_type: String,

    @SerialName("message_type")
    val message_type: String,

    @SerialName("sub_type")
    val sub_type: String,

    @SerialName("message_id")
    val message_id: Int,

    @SerialName("group_id")
    val group_id: Long,

    @SerialName("user_id")
    val user_id: Long,

    @SerialName("anonymous")
    val anonymous: Anonymous?,


    @SerialName("raw_message")
    val raw_message: String,

    @SerialName("font")
    val font: Int,

    @SerialName("sender")
    val sender: Sender? = null,

    @SerialName("message_format")
    val message_format: String
)

@Serializable
data class Data(
    val echo: Int,
    val message: String = "",
    val retcode: Int = 0,
    val status: String = "ok",
    val wording: String = ""
) {
    constructor(echo: Int) : this(echo, "", 0, "ok", "")

    companion object {

        fun parse(json: String): Data {
            return Json.decodeFromString(serializer(), json)
        }
    }

    override fun toString() : String {
        return Json.encodeToString(serializer(), this)
    }
}