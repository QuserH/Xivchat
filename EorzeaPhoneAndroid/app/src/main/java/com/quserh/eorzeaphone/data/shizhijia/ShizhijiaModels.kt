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

/**
 * 接口里空值有三种写法：缺字段、空串、字面量 "null"。三种一律归成空串。
 * （原先是 ShizhijiaGlamourDetail.fromJson 里的局部函数，招募解析也要用，提到顶层。）
 */
internal fun cleanField(v: String?): String =
    v?.takeUnless { it.isBlank() || it == "null" || it == "NULL" }?.trim().orEmpty()

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

/** 特殊成就 (userInfo/getUserInfo -> achieveInfo[])。图标: ffstones/medal/medal{medalId}.png */
data class ShizhijiaAchievement(
    val medalId: String,
    val medalType: String,
    val name: String,
    val detail: String,
    val time: String,
)

/** 游戏近况 (userInfo/getResently)。图标: ffstones/recent/r{typeId}.png */
data class ShizhijiaRecentEvent(
    val typeId: String,
    val eventType: String,
    val detail: String,
    val logTime: String,
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
            profile = o.optString("profile").takeUnless { it.isBlank() || it == "null" }.orEmpty(),
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
    val achievements: List<ShizhijiaAchievement>,
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
            val achievements = mutableListOf<ShizhijiaAchievement>()
            o.optJSONArray("achieveInfo")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val a = arr.optJSONObject(i) ?: continue
                    achievements.add(
                        ShizhijiaAchievement(
                            medalId = a.optString("medal_id"),
                            medalType = a.optString("medal_type"),
                            name = a.optString("achieve_name"),
                            detail = a.optString("achieve_detail"),
                            time = a.optString("achieve_time"),
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
                achievements = achievements,
            )
        }
    }
}


/** 染剂（名称 + 十六进制颜色），顺序与游戏染色孔 1/2 一一对应。 */
data class ShizhijiaGlamourDye(val name: String, val color: String)

/** One equipment slot of a glamour outfit (glamour/glamourDetail -> equipments[]). */
data class ShizhijiaGlamourEquip(
    val slot: String,
    val name: String,
    val dyes: List<ShizhijiaGlamourDye>,
    val iconUrl: String,
    val mallUrl: String,
    val dyeHoleCount: Int,
) {
    val isMallItem: Boolean get() = mallUrl.isNotBlank()

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

        fun fromJson(o: JSONObject): ShizhijiaGlamourEquip {
            // 染剂必须按 dye_ids 的顺序渲染（双染剂先后顺序影响效果），
            // 所以先按 id 建索引，再按 dye_ids 依次取出。
            val dyeById = mutableMapOf<String, ShizhijiaGlamourDye>()
            val leftovers = mutableListOf<ShizhijiaGlamourDye>()
            o.optJSONArray("dyes")?.let { da ->
                for (k in 0 until da.length()) {
                    val d = da.optJSONObject(k) ?: continue
                    val dye = ShizhijiaGlamourDye(name = d.optString("name"), color = d.optString("color"))
                    val id = d.optString("id")
                    if (id.isNotBlank()) dyeById[id] = dye else leftovers.add(dye)
                }
            }
            val ordered = mutableListOf<ShizhijiaGlamourDye>()
            val holeCount = o.optJSONArray("dye_ids")?.length() ?: 0
            o.optJSONArray("dye_ids")?.let { ids ->
                for (k in 0 until ids.length()) {
                    dyeById.remove(ids.optString(k))?.let { ordered.add(it) }
                }
            }
            ordered.addAll(leftovers)
            return ShizhijiaGlamourEquip(
                slot = o.optString("slot"),
                name = o.optString("name"),
                dyes = ordered,
                iconUrl = iconUrlFor(o.optString("icon_id")),
                mallUrl = o.optString("sqmall_url").takeUnless { it.isBlank() || it == "null" }.orEmpty(),
                dyeHoleCount = holeCount,
            )
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
                    equips.add(ShizhijiaGlamourEquip.fromJson(e))
                }
            }
            val author = o.optJSONObject("userInfo")
            val ort = o.optJSONObject("ortInfo")
            val glassesIcon = ort?.optString("glasses_icon").orEmpty()
            val ornamentIcon = ort?.optString("ornament_icon").orEmpty()
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

// ---------------------------------------------------------------------------
// 招募（recruit/*）
//
// 五类招募各有自己的行结构，但列表页要展示的东西高度重合：谁发的、在哪个服、
// 标题/正文摘要、一串标签、时间。所以统一成一个 ShizhijiaRecruit，
// 由各自的 fromXxx 把差异塞进 tags/lines，界面只认这一种卡片。
// 字段名以实测响应为准（见 开发/石之家OX/03-招募系统.md）。
// ---------------------------------------------------------------------------

/** 招募分类。id 用于路由到对应的 list 接口。 */
enum class ShizhijiaRecruitKind(val label: String) {
    Fb("副本组队"),
    Novice("新人招待"),
    Guild("部队招募"),
    Other("其他"),
    Rp("RP 俱乐部"),
}

data class ShizhijiaRecruit(
    val id: String,
    val kind: ShizhijiaRecruitKind,
    val uuid: String,
    val title: String,
    val characterName: String,
    val areaName: String,
    val groupName: String,
    /** 招募面向的服务器（和发布者所在服可能不同）。 */
    val targetServer: String,
    val avatar: String,
    val coverPic: String,
    /** 摘要（招募正文是富文本，这里已去标签）。 */
    val summary: String,
    /** 副标题行，例如 "绝境战 · 满编小队" 或 "工作日 20:00-00:00"。 */
    val lines: List<String>,
    /** 标签（副本标签 / 玩法风格 / 分类名 / RP 自定义标签）。 */
    val tags: List<String>,
    /** 已报名人数，没有这个概念时为 -1。 */
    val responseNum: Int,
    val createdAt: String,
) {
    companion object {
        /** 副本组队：fb_type/fb_name/进度/时间 + 各位置报名数。 */
        fun fromFb(o: JSONObject): ShizhijiaRecruit {
            val lines = buildList {
                listOf(o.optString("fb_type"), o.optString("fb_name"))
                    .filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
                    ?.let { add(it.joinToString(" · ")) }
                val time = cleanField(o.optString("fb_time"))
                val comp = cleanField(o.optString("team_composition"))
                listOf(comp, time).filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
                    ?.let { add(it.joinToString(" · ")) }
                cleanField(o.optString("progress")).takeIf { it.isNotBlank() }?.let { add("进度 $it") }
            }
            return ShizhijiaRecruit(
                id = o.optString("id"),
                kind = ShizhijiaRecruitKind.Fb,
                uuid = o.optString("uuid"),
                // 副本组队没有 title 字段，用副本名当标题。
                title = listOf(o.optString("fb_name"), o.optString("fb_type"))
                    .firstOrNull { it.isNotBlank() }.orEmpty().ifBlank { "副本组队" },
                characterName = o.optString("character_name"),
                areaName = o.optString("area_name"),
                groupName = o.optString("group_name"),
                targetServer = cleanField(o.optString("target_area_name")),
                avatar = cleanAvatar(o.optString("avatar")),
                coverPic = "",
                summary = cleanField(o.optString("strategy")).takeIf { it.isNotBlank() }?.let { "攻略参考：$it" }.orEmpty(),
                lines = lines,
                tags = namesOf(o.optJSONArray("labelInfo"), "name") +
                    splitPictures(cleanField(o.optString("custom_label"))),
                responseNum = o.optInt("response_num", -1),
                createdAt = o.optString("created_at"),
            )
        }

        /** 新人招待：identity 决定是豆芽找导师还是导师找学员。 */
        fun fromNovice(o: JSONObject): ShizhijiaRecruit {
            val identity = when (o.optInt("identity")) {
                1 -> "找导师"
                2 -> "找学员"
                else -> ""
            }
            val lines = buildList {
                identity.takeIf { it.isNotBlank() }?.let { add(it) }
                val wd = cleanField(o.optString("weekday_time"))
                val we = cleanField(o.optString("weekend_time"))
                if (wd.isNotBlank()) add("工作日 $wd")
                if (we.isNotBlank()) add("周末 $we")
            }
            return ShizhijiaRecruit(
                id = o.optString("id"),
                kind = ShizhijiaRecruitKind.Novice,
                uuid = o.optString("uuid"),
                title = o.optString("title").ifBlank { "新人招待" },
                characterName = o.optString("character_name"),
                areaName = o.optString("area_name"),
                groupName = o.optString("group_name"),
                targetServer = listOf(o.optString("target_area_name"), o.optString("target_group_name"))
                    .map { cleanField(it) }.filter { it.isNotBlank() }.joinToString(" "),
                avatar = cleanAvatar(o.optString("avatar")),
                coverPic = "",
                summary = stripHtml(o.optString("detail_mask")),
                lines = lines,
                tags = namesOf(o.optJSONArray("styleInfo"), "style"),
                responseNum = -1,
                createdAt = o.optString("created_at"),
            )
        }

        /** 其他招募：category_name 就是"好友/住宅/RP"这类分类。 */
        fun fromOther(o: JSONObject): ShizhijiaRecruit = ShizhijiaRecruit(
            id = o.optString("id"),
            kind = ShizhijiaRecruitKind.Other,
            uuid = o.optString("uuid"),
            title = o.optString("title").ifBlank { "其他招募" },
            characterName = o.optString("character_name"),
            areaName = o.optString("area_name"),
            groupName = o.optString("group_name"),
            targetServer = listOf(o.optString("target_area_name"), o.optString("target_group_name"))
                .map { cleanField(it) }.filter { it.isNotBlank() }.joinToString(" "),
            avatar = cleanAvatar(o.optString("avatar")),
            coverPic = cleanField(o.optString("cover_pic")),
            summary = stripHtml(o.optString("detail_mask")),
            lines = emptyList(),
            tags = listOfNotNull(cleanField(o.optString("category_name")).takeIf { it.isNotBlank() }),
            responseNum = -1,
            createdAt = o.optString("created_at"),
        )

        /** RP 俱乐部：有评分、地址、营业时间。 */
        fun fromRp(o: JSONObject): ShizhijiaRecruit {
            val lines = buildList {
                cleanField(o.optString("open_time")).takeIf { it.isNotBlank() }?.let { add("营业 $it") }
                cleanField(o.optString("address")).takeIf { it.isNotBlank() }?.let { add(it) }
                val score = cleanField(o.optString("comment_score"))
                val votes = o.optInt("score_count")
                if (score.isNotBlank() && score != "0" && votes > 0) add("评分 $score（$votes 人）")
            }
            return ShizhijiaRecruit(
                id = o.optString("id"),
                kind = ShizhijiaRecruitKind.Rp,
                uuid = o.optString("uuid"),
                title = o.optString("rp_name").ifBlank { "RP 俱乐部" },
                characterName = o.optString("character_name"),
                areaName = o.optString("area_name"),
                groupName = o.optString("group_name"),
                targetServer = listOf(o.optString("rp_area_name"), o.optString("rp_group_name"))
                    .map { cleanField(it) }.filter { it.isNotBlank() }.joinToString(" "),
                avatar = cleanAvatar(o.optString("avatar")),
                coverPic = cleanField(o.optString("cover_h5_pic")).ifBlank { cleanField(o.optString("cover_pic")) },
                summary = stripHtml(o.optString("profile")),
                lines = lines,
                // rp_type 是数组，自定义标签是逗号串。
                tags = stringsOf(o.optJSONArray("rp_type")) +
                    splitPictures(cleanField(o.optString("custom_label"))),
                responseNum = -1,
                createdAt = o.optString("created_at"),
            )
        }

        /**
         * 部队招募：接口需登录，本机无会话时拿不到样本，
         * 所以这里按文档字段做尽量宽松的解析——取得到就显示，取不到留空，
         * 不会因为字段名不符而崩。
         */
        fun fromGuild(o: JSONObject): ShizhijiaRecruit {
            val lines = buildList {
                listOf("guild_name", "guildName").firstNotNullOfOrNull { k ->
                    cleanField(o.optString(k)).takeIf { it.isNotBlank() }
                }?.let { add(it) }
                listOf("activity", "act_content", "recruit_require").forEach { k ->
                    cleanField(o.optString(k)).takeIf { it.isNotBlank() }?.let { add(it) }
                }
            }
            return ShizhijiaRecruit(
                id = o.optString("id"),
                kind = ShizhijiaRecruitKind.Guild,
                uuid = o.optString("uuid"),
                title = listOf("title", "guild_name", "guildName")
                    .firstNotNullOfOrNull { k -> o.optString(k).takeIf { it.isNotBlank() } }
                    .orEmpty().ifBlank { "部队招募" },
                characterName = o.optString("character_name"),
                areaName = o.optString("area_name"),
                groupName = o.optString("group_name"),
                targetServer = listOf(o.optString("target_area_name"), o.optString("target_group_name"))
                    .map { cleanField(it) }.filter { it.isNotBlank() }.joinToString(" "),
                avatar = cleanAvatar(o.optString("avatar")),
                coverPic = cleanField(o.optString("cover_pic")),
                summary = stripHtml(o.optString("detail_mask").ifBlank { o.optString("detail") }),
                lines = lines.take(2),
                tags = namesOf(o.optJSONArray("labelInfo"), "name"),
                responseNum = o.optInt("response_num", -1),
                createdAt = o.optString("created_at"),
            )
        }

        fun fromArray(arr: JSONArray, kind: ShizhijiaRecruitKind): List<ShizhijiaRecruit> =
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        when (kind) {
                            ShizhijiaRecruitKind.Fb -> fromFb(o)
                            ShizhijiaRecruitKind.Novice -> fromNovice(o)
                            ShizhijiaRecruitKind.Guild -> fromGuild(o)
                            ShizhijiaRecruitKind.Other -> fromOther(o)
                            ShizhijiaRecruitKind.Rp -> fromRp(o)
                        }
                    )
                }
            }
    }
}

/**
 * 账号绑定的角色（groupAndRole/getCharacterBindInfo）。
 * 字段名按官方其他接口的习惯猜取多个候选——本机无会话无法实测，
 * 所以每个字段都给几种可能的键名，取到哪个算哪个。
 */
data class ShizhijiaBoundCharacter(
    val name: String,
    val areaName: String,
    val groupName: String,
    val race: Int,
    val tribe: Int,
    val gender: Int,
    val avatar: String,
    val isCurrent: Boolean,
) {
    companion object {
        fun fromJson(o: JSONObject, fallbackName: String = ""): ShizhijiaBoundCharacter {
            fun pick(vararg keys: String): String =
                keys.firstNotNullOfOrNull { k -> cleanField(o.optString(k)).takeIf { it.isNotBlank() } }.orEmpty()
            return ShizhijiaBoundCharacter(
                name = pick("character_name", "characterName", "name").ifBlank { cleanField(fallbackName) },
                areaName = pick("area_name", "areaName", "AreaName"),
                groupName = pick("group_name", "groupName", "GroupName"),
                race = o.optInt("race"),
                tribe = o.optInt("tribe"),
                gender = o.optInt("gender", -1),
                avatar = cleanAvatar(pick("avatar", "face")),
                // 官方用 is_default / is_current 之类标记当前角色，两个都认。
                isCurrent = o.optInt("is_current") == 1 || o.optInt("is_default") == 1 ||
                    o.optBoolean("current", false),
            )
        }

        fun fromArray(arr: JSONArray): List<ShizhijiaBoundCharacter> =
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { add(fromJson(it)) }
                }
            }.filter { it.name.isNotBlank() }
    }
}

/** 取 [{key: "..."}] 里的 key 值，用于 labelInfo/styleInfo 这类字典数组。 */
internal fun namesOf(arr: JSONArray?, key: String): List<String> {
    arr ?: return emptyList()
    return buildList(arr.length()) {
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.optString(key)?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
}

/** 取纯字符串数组（rp_type 这种）。 */
internal fun stringsOf(arr: JSONArray?): List<String> {
    arr ?: return emptyList()
    return buildList(arr.length()) {
        for (i in 0 until arr.length()) {
            arr.optString(i).takeIf { it.isNotBlank() && it != "null" }?.let { add(it) }
        }
    }
}

/**
 * 招募正文是富文本，列表只要摘要，所以直接去标签压空白。
 * 帖子详情那种需要保留结构的仍然走 ShizhijiaRichContent。
 */
internal fun stripHtml(raw: String): String {
    if (raw.isBlank() || raw == "null") return ""
    return raw
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("</(p|div|li|h[1-6])>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace(Regex("\\s+"), " ")
        .trim()
}