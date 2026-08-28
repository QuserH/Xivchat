# 石之家接口抓取脚本

这几个脚本用来从石之家网页版自己的 JS 产物里读出接口形状——路径、方法、
请求体字段名。**不需要抓包，也不需要登录**：站点是 Vite 分包的 Vue 应用，
chunk 全是公开静态资源。

接口改版之后重跑一遍就能拿到新的形状，不用手动上机操作。

结论表在 `app/src/main/java/com/quserh/eorzeaphone/data/shizhijia/API_WRITE_ENDPOINTS.md`。

## 用法

```bash
# 1. 拉入口页，取出 entry bundle 的文件名
curl -s "https://ff14risingstones.web.sdo.com/pc/index.html" -o /tmp/rs_index.html
grep -o 'src="./static/js/index\.[^"]*\.js"' /tmp/rs_index.html

# 2. 拉 entry；它里面以字符串形式列着全部 chunk 名
curl -s "https://ff14risingstones.web.sdo.com/pc/static/js/index.<hash>.js" -o /tmp/rs_main.js
grep -o '"\./[A-Za-z0-9_.-]*\.js"' /tmp/rs_main.js | tr -d '"' | sed 's|^\./||' | sort -u > /tmp/rs_chunks.txt

# 3. 拉全部 chunk（155 个，约 2.4 MB）
bash fetch_all.sh

# 4. 扫出所有端点（路径 + 方法 + 能直接读到的字段名）
python extract_api.py

# 5. 请求体是变量（扫不出字段名）的，顺着 export 别名找调用点
python find_bodies.py posts/comment posts/like
python find_callers.py posts.PdYvHlRm.js h posts/comment
```

`extract_api.py` 和 `find_callers.py` 里的 `/tmp` 路径在 Windows 上要换成
`cygpath -w /tmp` 的结果（Git Bash 的 /tmp 和 Windows 版 Python 看到的不是同一个）。

## 原理

请求都是这个形状（axios 包一层）：

```js
function p(o){return t({url:"".concat(s,"posts/like"),method:"post",data:o})}
```

所以正则扫 `url:...method:...data/params` 三元组就能拿到路径和方法。
请求体是对象字面量时字段名直接可读；是变量时要顺着
`export{p as P}` 的别名找到 `import{P as X}` 的消费方，再看它构造的对象。

`index` bundle 里的 axios 拦截器还交代了两件容易踩的事：

```js
"post"==t.method && (t.data={...t.data,tempsuid:b()}, t.data=y.stringify(t.data))
```

- POST 的 body 里**也要**有一个 `tempsuid`（和 query 里的是两个不同的 uuid）
- body 是 **form-urlencoded**（qs.stringify），不是 JSON
