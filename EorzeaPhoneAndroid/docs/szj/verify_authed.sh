#!/usr/bin/env bash
# 用一份登录 cookie 验证需要登录才能读的接口，只**读**，不写。
#
# 用法：
#   bash verify_authed.sh /c/Users/Administrator/szj_cookie.txt
#
# cookie 文件里放**一行**：整条 Cookie 头的值（形如 ff14risingstones=...;...）。
# 注意：
#   - 这个文件请放在**仓库外面**，不要提交，也不要贴进对话里。
#   - 脚本从头到尾不打印 cookie 内容，只打印接口返回的字段名/值。
#   - 输出的 JSON 落在 /tmp/szj_authed/，看完自己删。
set -u

COOKIE_FILE="${1:-}"
if [ -z "$COOKIE_FILE" ] || [ ! -s "$COOKIE_FILE" ]; then
  echo "用法: bash verify_authed.sh <cookie文件路径>" >&2
  echo "（文件里一行，内容是 Cookie 头的值；放仓库外面）" >&2
  exit 2
fi

COOKIE=$(tr -d '\r\n' < "$COOKIE_FILE")
BASE="https://apiff14risingstones.web.sdo.com/api/home"
UA="Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
REF="https://ff14risingstones.web.sdo.com/"
OUT=/tmp/szj_authed
mkdir -p "$OUT"

# tempsuid：站点每个请求带一个新 uuid，query 和 body 各一个。
newuuid() { python -c "import uuid;print(uuid.uuid4())"; }

# get <输出名> <路径带query>
get() {
  local name="$1" path="$2" sep="?"
  case "$path" in *\?*) sep="&";; esac
  local code
  code=$(curl -s -m 40 "${BASE}/${path}${sep}tempsuid=$(newuuid)" \
    -H "User-Agent: $UA" -H "Referer: $REF" -H "Cookie: $COOKIE" \
    -o "$OUT/$name.json" -w "%{http_code}")
  printf "%-26s http=%s bytes=%s\n" "$name" "$code" "$(wc -c < "$OUT/$name.json")"
}

echo "== 拉取 =="
get login        "groupAndRole/getCharacterBindInfo?platform=2"
get me           "userInfo/getUserInfo"
get starPosts1   "userInfo/myStarPosts?type=1&page=1&limit=5"
get starPosts0   "userInfo/myStarPosts?page=1&limit=5"
get starStrats   "userInfo/myStarPosts?type=2&page=1&limit=5"
get starRp       "recruit/homePageStarRecruitRp"
get favFolders   "glamour/myFavoritesList?page=1&limit=50"

echo
echo "== 解读 =="
python - "$OUT" <<'PY'
import io, json, os, sys

out = sys.argv[1]

def load(n):
    p = os.path.join(out, n + ".json")
    if not os.path.exists(p):
        return None
    try:
        return json.load(io.open(p, encoding="utf-8"))
    except Exception as e:
        return {"_parse_error": str(e)}

def head(n):
    d = load(n)
    if d is None:
        print(f"[{n}] 没有文件")
        return None
    print(f"[{n}] code={d.get('code')} msg={json.dumps(d.get('msg'), ensure_ascii=False)}")
    return d

def keys_of(d):
    data = d.get("data")
    if isinstance(data, dict):
        rows = data.get("rows")
        if isinstance(rows, list):
            return ("rows", len(rows), sorted(rows[0].keys()) if rows else [])
        return ("obj", 1, sorted(data.keys()))
    if isinstance(data, list):
        return ("arr", len(data), sorted(data[0].keys()) if data and isinstance(data[0], dict) else [])
    return ("?", 0, [])

# 1) 登录有没有生效
d = head("login")
if d and d.get("code") == 10000:
    info = d.get("data") or {}
    print("   登录有效。uuid =", info.get("uuid"), "角色 =", json.dumps(info.get("character_name"), ensure_ascii=False))
else:
    print("   !! 登录没生效，下面的结果都不用看了（cookie 过期或不完整）")

# 2) 主页有没有 ip_location —— 这次要回答的问题
d = head("me")
if d and d.get("code") == 10000:
    kind, n, ks = keys_of(d)
    print("   userInfo/getUserInfo 字段:", ks)
    ipish = [k for k in ks if "ip" in k.lower() or "locat" in k.lower() or "region" in k.lower()]
    print("   >>> 疑似属地字段:", ipish if ipish else "无")
    data = d.get("data") or {}
    for k in ipish:
        print("       ", k, "=", json.dumps(data.get(k), ensure_ascii=False))

# 3) myStarPosts 的 type 到底必不必填
for n in ("starPosts1", "starPosts0", "starStrats"):
    d = head(n)
    if d and d.get("code") == 10000:
        kind, cnt, ks = keys_of(d)
        print(f"   {kind} 条数={cnt}")
        if ks:
            print("   行字段:", ks)
            print("   >>> 行里有 ip_location:", "ip_location" in ks)

# 4) RP 收藏
d = head("starRp")
if d and d.get("code") == 10000:
    kind, cnt, ks = keys_of(d)
    print(f"   {kind} 条数={cnt}")
    if ks:
        print("   行字段:", ks)

# 5) 幻化收藏夹 → 夹子里的内容（第二层要用第一层的 id，所以这里只报夹子）
d = head("favFolders")
if d and d.get("code") == 10000:
    kind, cnt, ks = keys_of(d)
    print(f"   {kind} 条数={cnt}")
    if ks:
        print("   夹子字段:", ks)
    rows = ((d.get("data") or {}).get("rows")) or []
    for r in rows[:5]:
        print("       夹子 id=%s name=%s is_default=%s" % (
            r.get("id"), json.dumps(r.get("name") or r.get("title"), ensure_ascii=False), r.get("is_default")))
    if rows:
        fid = rows[0].get("id")
        default = next((r for r in rows if str(r.get("is_default")) == "1"), None)
        if default:
            fid = default.get("id")
        io.open(os.path.join(out, "folder_id.txt"), "w").write(str(fid))
        print("   >>> 第二层用这个夹子 id:", fid)
PY

# 6) 幻化收藏夹里的内容：要第一层的 id，所以单独跑一趟
if [ -s "$OUT/folder_id.txt" ]; then
  FID=$(cat "$OUT/folder_id.txt")
  echo
  echo "== 幻化收藏夹内容（favorite_id=$FID）=="
  get favItems "glamour/myFavoriteItemsList?favorite_id=${FID}&page=1&limit=8"
  python - "$OUT" <<'PY'
import io, json, os, sys
out = sys.argv[1]
p = os.path.join(out, "favItems.json")
d = json.load(io.open(p, encoding="utf-8"))
print("code=%s msg=%s" % (d.get("code"), json.dumps(d.get("msg"), ensure_ascii=False)))
rows = ((d.get("data") or {}).get("rows")) or []
print("条数 =", len(rows))
if rows:
    print("行字段:", sorted(rows[0].keys()))
    print(">>> 有 glamour_id:", "glamour_id" in rows[0], " 有 id:", "id" in rows[0])
    for r in rows[:5]:
        print("    id=%s glamour_id=%s is_valid=%s title=%s" % (
            r.get("id"), r.get("glamour_id"), r.get("is_valid"),
            json.dumps(r.get("title"), ensure_ascii=False)))
PY
fi

echo
echo "完成。JSON 在 $OUT（含你的账号数据，看完删掉：rm -rf $OUT）"
