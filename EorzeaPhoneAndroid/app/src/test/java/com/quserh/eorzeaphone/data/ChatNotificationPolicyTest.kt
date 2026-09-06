package com.quserh.eorzeaphone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatNotificationPolicyTest {
    @Test
    fun noviceNetworkIsNotMutedByLocalChatSwitches() {
        for (channel in listOf(27, 75, 94)) {
            assertEquals(ChatCategory.Public, ChatCategory.fromChannel(channel))
            for (switches in 0..15) {
                assertTrue("channel=$channel switches=$switches", enabled(channel, switches))
            }
        }
    }

    @Test
    fun sayChannelsOnlyFollowSaySwitch() = assertSwitch(listOf(10, 81), 2)

    @Test
    fun yellChannelsOnlyFollowYellSwitch() = assertSwitch(listOf(11, 82), 4)

    @Test
    fun shoutChannelsOnlyFollowShoutSwitch() = assertSwitch(listOf(30, 83), 8)

    @Test
    fun tellChannelsOnlyFollowTellSwitch() = assertSwitch(listOf(12, 13, 80), 1)

    @Test
    fun otherGroupChannelsRemainIndependentOfLocalAndTellSwitches() {
        val channels = listOf(14, 15, 24, 32, 36, 37, 84, 85) + (16..23) + (86..93) + (101..107)
        for (channel in channels) {
            for (switches in 0..15) {
                assertTrue("channel=$channel switches=$switches", enabled(channel, switches))
            }
        }
    }

    private fun assertSwitch(channels: List<Int>, mask: Int) {
        for (channel in channels) {
            for (switches in 0..15) {
                assertEquals("channel=$channel switches=$switches", switches and mask != 0, enabled(channel, switches))
            }
        }
    }

    private fun enabled(channel: Int, switches: Int) = isChatChannelNotificationEnabled(
        channel = channel,
        tellNotifications = switches and 1 != 0,
        localNotifySay = switches and 2 != 0,
        localNotifyYell = switches and 4 != 0,
        localNotifyShout = switches and 8 != 0,
    )
}
