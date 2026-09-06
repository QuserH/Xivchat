package com.quserh.eorzeaphone.data

internal fun isChatChannelNotificationEnabled(
    channel: Int,
    tellNotifications: Boolean,
    localNotifySay: Boolean,
    localNotifyYell: Boolean,
    localNotifyShout: Boolean,
): Boolean = when (channel) {
    // Public also includes the Novice Network. Local-chat switches must only
    // gate their exact channels, not every message in that category.
    10, 81 -> localNotifySay
    11, 82 -> localNotifyYell
    30, 83 -> localNotifyShout
    else -> ChatCategory.fromChannel(channel) != ChatCategory.Tell || tellNotifications
}
