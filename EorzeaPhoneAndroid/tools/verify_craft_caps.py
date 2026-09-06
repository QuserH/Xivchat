"""Offline regression checks against the shipped recipe database, not a mock."""
import gzip
from pathlib import Path
import re
import sqlite3
import unittest

PROJECT = Path(__file__).resolve().parents[1]


class CraftCapsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.db = sqlite3.connect(":memory:")
        cls.db.deserialize(gzip.decompress((PROJECT / "app/src/main/assets/craft.dbz").read_bytes()))

    @classmethod
    def tearDownClass(cls):
        cls.db.close()

    def test_scepter_is_16500_not_15000(self):
        self.assertEqual((7500, 16500, 110), self.db.execute("SELECT pmax,qmax,quality_factor FROM recipes WHERE id=35836").fetchone())

    def test_every_recipe_applies_its_own_factors(self):
        rows = self.db.execute("SELECT r.id,pmax,qmax,difficulty,quality,difficulty_factor,quality_factor FROM recipes r JOIN rltable t ON t.rlv=r.rlv").fetchall()
        self.assertEqual(13393, len(rows))
        for recipe_id, pmax, qmax, difficulty, quality, pf, qf in rows:
            self.assertEqual(difficulty * pf // 100, pmax, recipe_id)
            self.assertEqual(quality * qf // 100, qmax, recipe_id)

    def test_cached_v3_is_replaced_and_network_repair_retains_factors(self):
        source = (PROJECT / "app/src/main/java/com/quserh/eorzeaphone/craft/data/RecipeDb.kt").read_text(encoding="utf-8")
        version = re.search(r'EXPECTED_SCHEMA = "(\d+)"', source).group(1)
        self.assertEqual("4", version)
        self.assertEqual((version,), self.db.execute("SELECT v FROM meta WHERE k='schema_version'").fetchone())
        self.assertIn('"sp1" -> factors = readIntArray(reader)', source)
        self.assertIn("recipeCap(rl.second, qualityFactor)", source)
        self.assertEqual("ok", self.db.execute("PRAGMA integrity_check").fetchone()[0])


if __name__ == "__main__":
    unittest.main(verbosity=2)
