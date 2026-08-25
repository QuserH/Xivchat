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

/** Glamour feed card (glamour/glamoursList | glamoursFollowList). */
data class ShizhijiaGlamourCard(
    val id: String,
    val title: String,
    val mainImage: String,
    val likes: Int,
    val favorites: Int,
    val uuid: String,
    val characterName: String,
    val areaName: String,
    val groupName: String,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaGlamourCard = ShizhijiaGlamourCard(
            id = o.optString("id"),
            title = o.optString("title"),
            mainImage = cleanAvatar(o.optString("main_image")),
            likes = o.optInt("likes"),
            favorites = o.optInt("favorites"),
            uuid = o.optString("uuid"),
            characterName = o.optString("character_name"),
            areaName = o.optString("area_name"),
            groupName = o.optString("group_name"),
        )

        fun fromArray(arr: JSONArray): List<ShizhijiaGlamourCard> =
            buildList(arr.length()) { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(fromJson(it)) } }
    }
}

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

/** One job row of a player's career list (userInfo/getUserInfo -> careerLevel[]). */
data class ShizhijiaCareer(
    val name: String,
    val level: Int,
    val type: String,
)

/**
 * Another player's profile (userInfo/getUserInfo?uuid=). Defensive parse:
 * characterDetail is an array; avatar may be null -> race portrait fallback
 * is resolved by the caller via ShizhijiaApi.
 */
data class ShizhijiaUserProfile(
    val uuid: String,
    val name: String,
    val areaName: String,
    val groupName: String,
    val avatar: String,
    val profile: String,
    val fansNum: Int,
    val followNum: Int,
    val likedNum: Int,
    val race: Int,
    val tribe: Int,
    val gender: Int,
    val createTime: String,
    val playTime: String,
    val lastLoginTime: String,
    val guildName: String,
    val guildTag: String,
    val houseInfo: String,
    val washingNum: Int,
    val killTimes: Int,
    val crystalRank: String,
    val fishTimes: Int,
    val treasureTimes: Int,
    val newrank: Int,
    val careers: List<ShizhijiaCareer>,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaUserProfile {
            val det = o.optJSONArray("characterDetail")?.optJSONObject(0)
                ?: o.optJSONObject("characterDetail")
                ?: JSONObject()
            val fans = o.optJSONObject("followFansiNum")
            fun txt(v: String?): String = v?.takeUnless { it.isBlank() || it == "null" }?.trim().orEmpty()
            val careers = mutableListOf<ShizhijiaCareer>()
            o.optJSONArray("careerLevel")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i) ?: continue
                    careers.add(
                        ShizhijiaCareer(
                            name = c.optString("career"),
                            level = c.optInt("character_level"),
                            type = c.optString("career_type"),
                        ),
                    )
                }
            }
            return ShizhijiaUserProfile(
                uuid = o.optString("uuid"),
                name = o.optString("character_name"),
                areaName = o.optString("area_name"),
                groupName = o.optString("group_name"),
                avatar = cleanAvatar(o.optString("avatar")),
                profile = txt(o.optString("profile")),
                fansNum = fans?.optInt("fansNum") ?: 0,
                followNum = fans?.optInt("followNum") ?: 0,
                likedNum = o.optInt("beLikedNum"),
                race = det.optInt("race"),
                tribe = det.optInt("tribe"),
                gender = det.optInt("gender", -1),
                createTime = txt(det.optString("create_time")),
                playTime = txt(det.optString("play_time")),
                lastLoginTime = txt(det.optString("last_login_time")),
                guildName = txt(det.optString("guild_name")),
                guildTag = txt(det.optString("guild_tag")),
                houseInfo = txt(det.optString("house_info")),
                washingNum = det.optInt("washing_num"),
                killTimes = det.optInt("kill_times"),
                crystalRank = txt(det.optString("crystal_rank")),
                fishTimes = det.optInt("fish_times"),
                treasureTimes = det.optInt("treasure_times"),
                newrank = det.optInt("newrank"),
                careers = careers,
            )
        }
    }
}

/** One equipment slot of a glamour outfit (glamour/glamourDetail -> equipments[]). */
data class ShizhijiaGlamourEquip(
    val slot: String,
    val name: String,
    val dyes: List<String>,
    val iconUrl: String,
) {
    companion object {
        // Item icons live on the EO CDN, sharded: {base}/{floor(id/1000)}000
        // zero-padded to 6 / {id} zero-padded to 6 + "_hr1.png" (mirrors the
        // web ItemIcon component).
        private const val ICON_BASE = "https://ff14-eo.web.sdo.com/ffstones/item/icon/dcsvv4fowz2m"

        fun iconUrlFor(iconId: String): String {
            val id = iconId.toIntOrNull() ?: return ""
            if (id <= 0) return ""
            val folder = (id / 1000).toString() + "000"
            return "$ICON_BASE/${folder.padStart(6, '0')}/${id.toString().padStart(6, '0')}_hr1.png"
        }
    }
}

/** Full glamour post (glamour/glamourDetail). */
data class ShizhijiaGlamourDetail(
    val id: String,
    val title: String,
    val desc: String,
    val images: List<String>,
    val likes: Int,
    val favorites: Int,
    val createdAt: String,
    val jobs: List<String>,
    val races: List<String>,
    val gender: Int,
    val authorName: String,
    val authorUuid: String,
    val authorAvatar: String,
    val areaName: String,
    val groupName: String,
    val equips: List<ShizhijiaGlamourEquip>,
    val glassesName: String,
    val glassesIconUrl: String,
    val ornamentName: String,
    val ornamentIconUrl: String,
) {
    companion object {
        fun fromJson(o: JSONObject): ShizhijiaGlamourDetail {
            val pics = mutableListOf<String>()
            cleanAvatar(o.optString("main_image")).takeIf { it.isNotBlank() }?.let { pics.add(it) }
            pics.addAll(splitPictures(o.optString("images")))
            val jobs = mutableListOf<String>()
            o.optJSONArray("job_ids")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let { jobs.add(it) } }
            val races = mutableListOf<String>()
            o.optJSONArray("race_ids")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let { races.add(it) } }
            val equips = mutableListOf<ShizhijiaGlamourEquip>()
            o.optJSONArray("equipments")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val e = arr.optJSONObject(i) ?: continue
                    val name = e.optString("name")
                    if (name.isBlank() || name == "null") continue
                    val dyes = mutableListOf<String>()
                    e.optJSONArray("dyes")?.let { da ->
                        for (k in 0 until da.length()) {
                            val d = da.optJSONObject(k) ?: continue
                            d.optString("name").takeIf { it.isNotBlank() }?.let { dyes.add(it) }
                        }
                    }
                    equips.add(
                        ShizhijiaGlamourEquip(
                            slot = e.optString("slot"),
                            name = name,
                            dyes = dyes,
                            iconUrl = ShizhijiaGlamourEquip.iconUrlFor(e.optString("icon_id")),
                        ),
                    )
                }
            }
            val author = o.optJSONObject("userInfo")
            val ort = o.optJSONObject("ortInfo")
            val glassesIcon = ort?.optString("glasses_icon").orEmpty()
            val ornamentIcon = ort?.optString("ornament_icon").orEmpty()
            fun cleanField(v: String?): String = v?.takeUnless { it.isBlank() || it == "null" }.orEmpty()
            return ShizhijiaGlamourDetail(
                id = o.optString("id"),
                title = o.optString("title"),
                desc = o.optString("desc"),
                images = pics,
                likes = o.optInt("likes"),
                favorites = o.optInt("favorites"),
                createdAt = o.optString("created_at"),
                jobs = jobs,
                races = races,
                gender = o.optJSONArray("gender_ids")?.optInt(0, 0) ?: 0,
                authorName = author?.optString("character_name").orEmpty().ifBlank { o.optString("character_name") },
                authorUuid = author?.optString("uuid").orEmpty().ifBlank { o.optString("uuid") },
                authorAvatar = cleanAvatar(author?.optString("avatar").orEmpty()),
                areaName = o.optString("area_name"),
                groupName = o.optString("group_name"),
                equips = equips,
                glassesName = cleanField(ort?.optString("glasses_name")),
                glassesIconUrl = ShizhijiaGlamourEquip.iconUrlFor(glassesIcon),
                ornamentName = cleanField(ort?.optString("ornament_name")),
                ornamentIconUrl = ShizhijiaGlamourEquip.iconUrlFor(ornamentIcon),
            )
        }
    }
}