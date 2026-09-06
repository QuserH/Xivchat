"""Repair bundled recipe caps from the statics percentage factors, preserving all other data."""
import argparse
import gzip
import io
import json
from pathlib import Path
import sqlite3
import urllib.request
import zipfile


PROJECT = Path(__file__).resolve().parents[1]
ASSET = PROJECT / "app/src/main/assets/craft.dbz"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--statics", help="local statics pack; otherwise download the app's data source")
    args = parser.parse_args()
    if args.statics:
        payload = Path(args.statics).read_bytes()
    else:
        with urllib.request.urlopen("https://5p.nbb.ffxiv.cn/statics/statics.json", timeout=30) as response:
            payload = response.read()
    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        recipes = json.loads(archive.read("recipe_ja"))
    db = sqlite3.connect(":memory:")
    db.deserialize(gzip.decompress(ASSET.read_bytes()))
    columns = {row[1] for row in db.execute("PRAGMA table_info(recipes)")}
    for column in ("difficulty_factor", "quality_factor"):
        if column not in columns:
            db.execute(f"ALTER TABLE recipes ADD COLUMN {column} INTEGER NOT NULL DEFAULT 100")
    rows = db.execute("SELECT r.id, r.item_id, r.rlv, t.difficulty, t.quality FROM recipes r JOIN rltable t ON t.rlv=r.rlv").fetchall()
    assert len(rows) == db.execute("SELECT count(*) FROM recipes").fetchone()[0], "missing level data"
    changed = 0
    for recipe_id, item_id, rlv, difficulty, quality in rows:
        source = recipes[str(recipe_id)]
        assert source["it"] == item_id and source["rlv"] == rlv, f"source mismatch: {recipe_id}"
        progress_factor, quality_factor, _ = source["sp1"]
        progress_max = difficulty * progress_factor // 100
        quality_max = quality * quality_factor // 100
        before = db.execute("SELECT pmax, qmax FROM recipes WHERE id=?", (recipe_id,)).fetchone()
        changed += before != (progress_max, quality_max)
        db.execute("UPDATE recipes SET pmax=?, qmax=?, difficulty_factor=?, quality_factor=? WHERE id=?",
                   (progress_max, quality_max, progress_factor, quality_factor, recipe_id))
    assert db.execute("SELECT pmax,qmax FROM recipes WHERE id=35836").fetchone() == (7500, 16500)
    db.execute("INSERT OR REPLACE INTO meta VALUES('schema_version','4')")
    db.execute("INSERT OR REPLACE INTO meta VALUES('recipe_caps','level_base_times_recipe_factor')")
    db.commit()
    assert db.execute("PRAGMA integrity_check").fetchone()[0] == "ok"
    ASSET.write_bytes(gzip.compress(db.serialize(), compresslevel=9, mtime=0))
    db.close()
    print(f"Validated {len(rows)} recipes; corrected {changed} caps. Scepter: 7500 / 16500.")


if __name__ == "__main__":
    main()
