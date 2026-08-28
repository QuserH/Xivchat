#!/usr/bin/env bash
# Pull the Rising Stones web chunks that own the write features, then dump every
# API-looking string. Read-only: fetches public static JS, no cookie, no login.
set -u
cd /tmp
mkdir -p rs_js
BASE="https://ff14risingstones.web.sdo.com/pc/static/js"
REF="https://ff14risingstones.web.sdo.com/pc/index.html"

want="posts.PdYvHlRm.js
glamour.BluDJ2WR.js
glamour.Cb3LPeUp.js
glamour.ZEqKOMxb.js
Comment.CMSwEVrf.js
CommentBox.CI5TVcRq.js
GlamourDetail.BWfpekNi.js
GlamourItem.vue_vue_type_style_index_0_lang.B_8yoUfl.js
Recruit.CDUm8zU-.js
RecruitDefaultView.BUl9II0w.js
RecruitParty.2-BYKGMk.js
RecruitOthers.Btbsj9CX.js"

for f in $want; do
  if [ -s "rs_js/$f" ]; then continue; fi
  code=$(curl -s -m 45 "$BASE/$f" -H "User-Agent: Mozilla/5.0" -H "Referer: $REF" -o "rs_js/$f" -w "%{http_code}")
  printf "%-70s %s %s\n" "$f" "$code" "$(wc -c < "rs_js/$f" 2>/dev/null)"
done
