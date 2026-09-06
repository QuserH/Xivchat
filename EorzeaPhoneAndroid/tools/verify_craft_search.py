"""Check the actual bundled SQLite data and the app's search projection off-device."""

import gzip
from pathlib import Path
import sqlite3
import unittest


PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT / "app/src/main/java/com/quserh/eorzeaphone/craft/data/RecipeDb.kt"


class CraftSearchTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.database = sqlite3.connect(":memory:")
        cls.database.deserialize(gzip.decompress((PROJECT / "app/src/main/assets/craft.dbz").read_bytes()))
        cls.database.row_factory = sqlite3.Row
        cls.source = SOURCE.read_text(encoding="utf-8")
        cls.sql = cls.source.split('val sql = """', 1)[1].split('""".trimIndent()', 1)[0]
        cls.sql = cls.sql.replace("$esc", "ESCAPE char(92)").replace("$limit", "80")

    @classmethod
    def tearDownClass(cls):
        cls.database.close()

    def search(self, query):
        return self.database.execute(
            self.sql, (query,) * 3 + (query + "%",) * 3 + ("%" + query + "%",) * 3
        ).fetchall()

    def test_search_retains_actual_job_categories_in_all_match_modes(self):
        samples = self.database.execute(
            "SELECT id, name_cn, jobs FROM items WHERE jobs IN (38, 41, 44, 47, 50, 9) "
            "GROUP BY jobs ORDER BY jobs"
        ).fetchall()
        self.assertEqual(6, len(samples))
        for item in samples:
            for query in (item["name_cn"], item["name_cn"][:2], item["name_cn"][-2:]):
                for result in self.search(query):
                    expected = self.database.execute("SELECT jobs FROM items WHERE id=?", (result["id"],)).fetchone()[0]
                    self.assertEqual(expected, result["jobs"])
            exact = next(result for result in self.search(item["name_cn"]) if result["id"] == item["id"])
            self.assertEqual(item["jobs"], exact["jobs"])
            self.assertEqual(0, exact["rank_val"])

    def test_equipment_job_labels_are_present(self):
        labels = dict(self.database.execute("SELECT id, label FROM jobcat"))
        self.assertIn("骑士", labels[38])
        self.assertIn("武僧", labels[41])
        self.assertIn("战士", labels[44])
        self.assertIn("刻木匠", labels[9])

    def test_android_cursor_uses_named_jobs_column_and_sql_order(self):
        search_source = self.source.split("fun search(", 1)[1].split("fun item(", 1)[0]
        self.assertIn('cur.getColumnIndexOrThrow("jobs")', search_source)
        self.assertIn("cur.getInt(jobsColumn)", search_source)
        self.assertNotIn("rows.sortedWith", search_source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
