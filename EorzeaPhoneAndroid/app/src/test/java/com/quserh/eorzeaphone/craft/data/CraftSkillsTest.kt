package com.quserh.eorzeaphone.craft.data

import com.quserh.eorzeaphone.data.GameCraftSkill
import org.junit.Assert.*
import org.junit.Test

class CraftSkillsTest {
    @Test
    fun allJobsHaveFullCatalogAndUniqueCorrectlyIdentifiedActions() {
        assertEquals(36, CraftSkills.ALL.size)
        val allIds = mutableSetOf<Long>()
        for (job in 0..7) {
            val skills = CraftSkills.forJob(job)
            assertEquals(36, skills.size)
            assertEquals(36, skills.map { it.cn }.distinct().size)
            for (skill in skills) {
                assertTrue(allIds.add(skill.id))
                assertEquals(skill.canonicalId, CraftSkills.byId(skill.id)?.canonicalId)
                assertTrue(skill.icon > 0)
                assertTrue(skill.desc.isNotBlank())
            }
        }
    }

    @Test
    fun cnNamesMatchGameInsteadOfLiteralTranslations() {
        assertEquals("制作", CraftSkills.byId(100001)!!.cn)
        assertEquals("模范制作", CraftSkills.byId(100203)!!.cn)
        assertEquals("精修", CraftSkills.byId(100003)!!.cn)
        assertEquals("坚信", CraftSkills.byId(100379)!!.cn)
        assertEquals("专心致志", CraftSkills.byId(100419)!!.cn)
        assertEquals("巧夺天工", CraftSkills.byId(100467)!!.cn)
    }

    @Test
    fun jobSpecificIdsAndIconsStayPaired() {
        val goldsmith = CraftSkills.forJob(3)
        val basic = goldsmith.first { it.canonicalId == 100001L }
        assertEquals(100075L, basic.id)
        assertEquals(1651, basic.icon)
        val standard = goldsmith.first { it.canonicalId == 100004L }
        assertEquals(100078L, standard.id)
        assertEquals(1665, standard.icon)
        assertEquals(19002L, CraftSkills.forJob(6).first { it.canonicalId == 4639L }.id)
        assertEquals(265L, CraftSkills.forJob(4).first { it.canonicalId == 260L }.id)
        assertEquals(264L, CraftSkills.forJob(5).first { it.canonicalId == 260L }.id)
    }

    @Test
    fun liveSheetOverridesExactRowsWithoutMixingJobs() {
        val rows = listOf(
            GameCraftSkill(100001, "制作", 1501, "From game", 0),
            GameCraftSkill(100075, "制作", 1651, "Goldsmith description", 0),
        )
        val skill = CraftSkills.forJob(3, rows).first { it.canonicalId == 100001L }
        assertEquals("Goldsmith description", skill.desc)
        assertEquals(1651, skill.icon)
        assertEquals(100075L, skill.id)
        val fallback = CraftSkills.forJob(3, rows.take(1)).first { it.canonicalId == 100001L }
        assertEquals(1651, fallback.icon)
        assertEquals("From game", fallback.desc)
    }

    @Test
    fun incompletePluginCatalogDoesNotHideNewSkills() {
        assertEquals(36, CraftSkills.forJob(0, listOf(GameCraftSkill(100001, "", 0, "", -1))).size)
        assertEquals(32, CraftSkills.byId(100004)!!.cp)
        assertEquals(46, CraftSkills.byId(100411)!!.cp)
        assertEquals(3, CraftSkills.ALL.count { it.specialist })
    }
}
