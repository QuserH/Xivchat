#!/usr/bin/env bash
# Fetch every chunk listed in the entry bundle. Public static JS only.
set -u
cd /tmp
mkdir -p rs_js
BASE="https://ff14risingstones.web.sdo.com/pc/static/js"
REF="https://ff14risingstones.web.sdo.com/pc/index.html"
ok=0; fail=0
while read -r f; do
  [ -z "$f" ] && continue
  if [ -s "rs_js/$f" ]; then ok=$((ok+1)); continue; fi
  code=$(curl -s -m 45 "$BASE/$f" -H "User-Agent: Mozilla/5.0" -H "Referer: $REF" -o "rs_js/$f" -w "%{http_code}")
  if [ "$code" = "200" ]; then ok=$((ok+1)); else fail=$((fail+1)); rm -f "rs_js/$f"; echo "FAIL $code $f"; fi
done < rs_chunks.txt
echo "fetched ok=$ok fail=$fail"
du -sh rs_js
