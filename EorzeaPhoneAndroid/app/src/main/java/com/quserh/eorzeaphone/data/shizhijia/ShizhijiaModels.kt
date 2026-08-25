package com.quserh.eorzeaphone.data.shizhijia

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data models for the 石之家 (FF14 Rising Stones official community) web API.
 *
 * Field names deliberately mirror the backend JSON keys so parsing stays 1:1.
 * Everything is parsed defensively (opt* with type coercion) because the server
 * sometimes returns numbers as strings; a failed field simply degrades to a
 * sensible default instead of crashing the list/detail UI.
 */

/** A forum partition ("分区"), e.g. 冒险者行会 / 同人创作 / 剧情讨论. */
data class ShizhijiaPostPart(
    val id: String,
    val name: String,
    val parentId: String,
    val type: Int,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaPostPart = ShizhijiaPostPart(
            id = o.optString("id"),
            name = o.optString("name"),
            parentId = o.optString("parent_id"),
            type = o.optInt("type"),
        )
    }
}

/**
 * One row in the post list feed (posts/postsList -> data.rows[]).
 * `cover_pic` is a comma-joined list of image URLs, so it is split into
 * `coverPics` for the multi-thumbnail card (mirrors the official mini-app).
 */
data class ShizhijiaPostCard(
    val postsId: String,
    val uuid: String,
    val title: String,
    val partName: String,
    val partParentName: String,
    val characterName: String,
    val areaName: String,
    val groupName: String,
    val coverPics: List<String>,
    val avatar: String,
    val readCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val isTop: Boolean,
    val isRefine: Boolean,
    val createdAt: String,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaPostCard = ShizhijiaPostCard(
            postsId = o.optString("posts_id"),
            uuid = o.optString("uuid"),
            title = o.optString("title"),
            partName = o.optString("part_name"),
            partParentName = o.optString("part_parent_name"),
            characterName = o.optString("character_name"),
            areaName = o.optString("area_name"),
            groupName = o.optString("group_name"),
            coverPics = splitPictures(o.optString("cover_pic")),
            avatar = cleanAvatar(o.optString("avatar")),
            readCount = o.optLong("read_count"),
            likeCount = o.optLong("like_count"),
            commentCount = o.optLong("comment_count"),
            isTop = o.optInt("is_top") == 1,
            isRefine = o.optInt("is_refine") == 1,
            createdAt = o.optString("created_at"),
        )

        fun fromArray(arr: JSONArray): List<ShizhijiaPostCard> =
            buildList(arr.length()) { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(fromJson(it)) } }
    }
}

/** `cover_pic` fields carry several image URLs separated by commas. */
internal fun splitPictures(raw: String): List<String> =
    raw.split(',').map { it.trim() }.filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

/**
 * Avatar fields arrive as "", JSON null rendered as the literal string "null",
 * or a real URL. Normalize to "" so the UI can fall back to a letter avatar.
 */
internal fun cleanAvatar(raw: String): String =
    raw.trim().takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }.orEmpty()

/** Full post detail (posts/postsDetail -> data). `contentHtml` is the article body. */
data class ShizhijiaPostDetail(
    val id: String,
    val uuid: String,
    val title: String,
    val characterName: String,
    val areaName: String,
    val groupName: String,
    val avatar: String,
    val coverPics: List<String>,
    val createdAt: String,
    val readCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val relayCount: Long,
    val starCount: Long,
    val contentHtml: String,
    val partName: String,
    val isLike: Boolean,
) {
    val hasCover: Boolean get() = coverPics.isNotEmpty()
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaPostDetail {
            val author = o.optJSONObject("userInfo")
            val info = o.optJSONObject("contentInfo")
            val part = o.optJSONObject("partInfo")
            return ShizhijiaPostDetail(
                id = o.optString("id"),
                uuid = o.optString("uuid"),
                title = o.optString("title"),
                characterName = o.optString("character_name"),
                areaName = o.optString("area_name"),
                groupName = o.optString("group_name"),
                avatar = cleanAvatar(author?.optString("avatar").orEmpty().ifBlank { o.optString("avatar") }),
                coverPics = splitPictures(o.optString("cover_pic")),
                createdAt = o.optString("created_at"),
                readCount = o.optLong("read_count"),
                likeCount = o.optLong("like_count"),
                commentCount = o.optLong("comment_count"),
                relayCount = o.optLong("relay_count"),
                starCount = o.optLong("star_count"),
                contentHtml = info?.optString("content").orEmpty(),
                partName = part?.optString("name").orEmpty().ifBlank { o.optString("part_name") },
                isLike = o.optInt("is_like") == 1,
            )
        }
    }
}

/** A top-level comment on a post (posts/postsCommentDetail -> data.rows[]). */
data class ShizhijiaComment(
    val id: String,
    val uuid: String,
    val childrenCount: Int,
    val contentHtml: String,
    val commentPic: String,
    val characterName: String,
    val areaName: String,
    val groupName: String,
    val createdAt: String,
    val ipLocation: String,
    val likeCount: Long,
    val avatar: String,
    val isPostsAuthor: Boolean,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaComment = ShizhijiaComment(
            id = o.optString("id"),
            uuid = o.optString("uuid"),
            childrenCount = o.optInt("children_count"),
            contentHtml = o.optString("mask_content"),
            commentPic = cleanAvatar(o.optString("comment_pic")),
            characterName = o.optString("character_name"),
            areaName = o.optString("area_name"),
            groupName = o.optString("group_name"),
            createdAt = o.optString("created_at"),
            ipLocation = o.optString("ip_location"),
            likeCount = o.optLong("like_count"),
            avatar = cleanAvatar(o.optString("avatar")),
            isPostsAuthor = o.optInt("is_posts_author") == 1,
        )

        fun fromArray(arr: JSONArray): List<ShizhijiaComment> =
            buildList(arr.length()) { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(fromJson(it)) } }
    }
}

/** Hot search suggestion (common/getHotSearchList). Kept flexible because the server shape varies. */
data class ShizhijiaHotWord(val text: String, val value: String = "")

/** A dynamic (动态) feed row. Parsed defensively; not every field is present on fresh account. */
data class ShizhijiaDynamic(
    val id: String,
    val uuid: String,
    val characterName: String,
    val avatar: String,
    val areaName: String,
    val groupName: String,
    val contentText: String,
    val images: List<String>,
    val createdAt: String,
    val likeCount: Long,
    val commentCount: Long,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaDynamic {
            // The dynamic payload typically carries content as a JSON string or an object;
            // handle both shapes so the parser survives server-side changes.
            val text = when (val c = o.opt("content")) {
                is String -> c
                is JSONObject -> c.optString("text")
                else -> ""
            }
            val pics = when (val p = o.opt("pics")) {
                is JSONArray -> buildList(p.length()) { for (i in 0 until p.length()) p.optString(i) }
                is String -> listOf(p).filter { it.isNotBlank() }
                else -> emptyList()
            }
            return ShizhijiaDynamic(
                id = o.optString("id"),
                uuid = o.optString("uuid"),
                characterName = o.optString("character_name"),
                avatar = cleanAvatar(o.optString("avatar")),
                areaName = o.optString("area_name"),
                groupName = o.optString("group_name"),
                contentText = text,
                images = pics,
                createdAt = o.optString("created_at"),
                likeCount = o.optLong("like_count"),
                commentCount = o.optLong("comment_count"),
            )
        }

        fun fromArray(arr: JSONArray): List<ShizhijiaDynamic> =
            buildList(arr.length()) { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(fromJson(it)) } }
    }
}

/** Generic page wrapper — most list endpoints return { rows: [...], pageTime: "..." }. */
data class ShizhijiaPage<T>(
    val rows: List<T>,
    val pageTime: String,
) {
    val hasMore: Boolean get() = rows.isNotEmpty()
}

/**
 * One row of the cumulative check-in reward table (sign/signRewardList).
 * `isGet`: -1 = days not reached yet ("未满足"), 0 = claimable ("领取"),
 * 1 = already claimed ("已领取"). `rule` is the day count required.
 */
data class ShizhijiaSignReward(
    val id: String,
    val itemName: String,
    val itemPic: String,
    val rule: Int,
    val isGet: Int,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaSignReward = ShizhijiaSignReward(
            id = o.optString("id"),
            itemName = o.optString("item_name"),
            itemPic = o.optString("item_pic"),
            rule = o.optInt("rule"),
            isGet = o.optInt("is_get", -1),
        )
    }
}

/** One month of check-ins (sign/mySignLog): total count + the signed dates. */
data class ShizhijiaSignLog(
    val count: Int,
    val days: List<String>,
)

/** User search hit (common/search type=6). */
data class ShizhijiaSearchUser(
    val uuid: String,
    val name: String,
    val avatar: String,
    val areaName: String,
    val groupName: String,
    val profile: String,
    val fansNum: Int,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaSearchUser = ShizhijiaSearchUser(
            uuid = o.optString("uuid"),
            name = o.optString("character_name"),
            avatar = cleanAvatar(o.optString("avatar")),
            areaName = o.optString("area_name"),
            groupName = o.optString("group_name"),
            profile = o.optString("profile"),
            fansNum = o.optInt("fansNum"),
        )

        fun fromArray(arr: JSONArray): List<ShizhijiaSearchUser> =
            buildList(arr.length()) { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(fromJson(it)) } }
    }
}

/** Glamour search hit (common/search type=7). */
data class ShizhijiaSearchGlamour(
    val id: String,
    val title: String,
    val desc: String,
    val mainImage: String,
    val likes: Int,
    val favorites: Int,
    val characterName: String,
    val areaName: String,
    val groupName: String,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaSearchGlamour = ShizhijiaSearchGlamour(
            id = o.optString("id"),
            title = o.optString("title"),
            desc = o.optString("desc"),
            mainImage = cleanAvatar(o.optString("main_image")),
            likes = o.optInt("likes"),
            favorites = o.optInt("favorites"),
            characterName = o.optString("character_name"),
            areaName = o.optString("area_name"),
            groupName = o.optString("group_name"),
        )
    }
}