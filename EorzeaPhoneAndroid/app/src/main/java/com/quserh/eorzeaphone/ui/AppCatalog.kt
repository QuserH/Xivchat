package com.quserh.eorzeaphone.ui

import androidx.compose.ui.graphics.Color
import com.quserh.eorzeaphone.R

private val teal = Color(0xFF26AAA7)
private val cyan = Color(0xFF279FD6)
private val blue = Color(0xFF5E75E7)
private val green = Color(0xFF25B85D)
private val lime = Color(0xFF6A9D18)
private val orange = Color(0xFFE97A23)
private val gold = Color(0xFFC88B0C)
private val coral = Color(0xFFF05E58)
private val pink = Color(0xFFE91E63)
private val purple = Color(0xFF8365E9)
private val grey = Color(0xFF8A8C95)

object AppCatalog {
    val firstPage = listOf(
        PhoneAppItem("chirper", "叽叽", R.drawable.app_chirper, cyan),
        PhoneAppItem("aethergram", "以太图集", R.drawable.app_aethergram, coral),
        PhoneAppItem("velvet", "Velvet", R.drawable.app_velvet, pink),
        PhoneAppItem("polls", "投票", R.drawable.app_polls, blue),
        PhoneAppItem("announcements", "公告", R.drawable.app_announcements, orange),
        PhoneAppItem("camera", "相机", R.drawable.app_camera, grey),
        PhoneAppItem("photos", "照片", R.drawable.app_photos, gold),
        PhoneAppItem("feedback", "反馈", R.drawable.app_feedback, teal),
        PhoneAppItem("maps", "地图", R.drawable.app_maps, teal),
        PhoneAppItem("venues", "场馆", R.drawable.app_venues, purple),
        PhoneAppItem("games", "游戏", R.drawable.app_games, coral),
        PhoneAppItem("market", "市场", R.drawable.app_market, gold),
        // 石之家的图标用它自己的金（#c4a86a），不是蓝。桌面上其它 App 的颜色各不
        // 相同是对的（真手机就是这样），但石之家这一格代表的就是石之家。
        PhoneAppItem("shizhijia", "石之家", R.drawable.app_news, Color(0xFFC4A86A)),
        PhoneAppItem("appstore", "App Store", R.drawable.app_appstore, cyan),
    )

    val secondPage = listOf(
        PhoneAppItem("skywatcher", "天气预报", R.drawable.app_skywatcher, teal),
        PhoneAppItem("collections", "收藏馆", R.drawable.app_collections, blue),
        PhoneAppItem("inventory", "物品栏", R.drawable.app_inventory, orange),
        PhoneAppItem("fishing", "捕鱼", R.drawable.app_fishing, teal),
        PhoneAppItem("clock", "时钟", R.drawable.app_clock, coral),
        PhoneAppItem("notes", "备忘录", R.drawable.app_notes, gold),
        PhoneAppItem("calculator", "计算器", R.drawable.app_calculator, grey),
        PhoneAppItem("timers", "计时器", R.drawable.app_timers, lime),
        PhoneAppItem("shortcuts", "快捷指令", R.drawable.app_shortcuts, blue),
        PhoneAppItem("wallet", "钱包", R.drawable.app_wallet, green),
        PhoneAppItem("submarine", "潜水艇", R.drawable.app_timers, cyan),
        PhoneAppItem("dailies", "日常", R.drawable.app_dailies, teal),
        PhoneAppItem("calendar", "日历", R.drawable.app_calendar, coral),
        PhoneAppItem("character", "活跃度", R.drawable.app_character, cyan),
        PhoneAppItem("notifications", "通知", R.drawable.app_notifications, coral),
        PhoneAppItem("jobs", "职业", R.drawable.app_jobs, blue),
        PhoneAppItem("health", "健康", R.drawable.app_health, lime),
        PhoneAppItem("coin", "Aether Coin", R.drawable.app_coin, gold),
        PhoneAppItem("gamba", "Gamba", R.drawable.app_games, green),
        PhoneAppItem("muster", "Muster", R.drawable.app_muster, teal),
        PhoneAppItem("yellowpages", "黄页", R.drawable.app_yellowpages, gold),
        PhoneAppItem("housing", "房屋", R.drawable.app_housing, teal),
        PhoneAppItem("wiki", "WiKi", R.drawable.app_appstore, teal),
        PhoneAppItem("gatherclock", "采集时钟", R.drawable.app_timers, lime),
    )

    val dock = listOf(
        PhoneAppItem("gamechat", "聊天", R.drawable.app_messages, orange, PhoneScreen.Chat),
        PhoneAppItem("contacts", "联系人", R.drawable.app_contacts, green, PhoneScreen.Contacts),
        PhoneAppItem("settings", "设置", R.drawable.app_settings, grey, PhoneScreen.Settings),
    )
}
