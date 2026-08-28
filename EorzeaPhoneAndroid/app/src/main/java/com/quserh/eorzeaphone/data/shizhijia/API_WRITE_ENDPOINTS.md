# 石之家写操作接口

来源：站点自己的 JS 产物（`https://ff14risingstones.web.sdo.com/pc/static/js/`，
Vite 分包，共 155 个 chunk，公开静态资源，不带 cookie 即可读）。
不是猜的——每条都能在下面标注的 chunk 里找到构造请求体的那段代码。

抓法记在 `.buildtmp/`：`fetch_all.sh`（拉全部 chunk）、`extract_api.py`
（扫 `url:...method:...data/params` 三元组，得到 269 个端点）、
`find_callers.py`（顺着 export 别名找到调用点，读出字段名）。
接口变了重跑一遍就行。

基址同现有代码：`https://apiff14risingstones.web.sdo.com/api/home/`

成功码 `code == 10000`。`code == 10403` = 未登录/未绑定角色
（站内提示"请登录并绑定角色后再操作"）。

## 帖子

| 操作 | 方法 | 路径 | 请求体 |
|---|---|---|---|
| 点赞（切换） | POST | `posts/like` | `{id, type}` |
| 收藏（切换） | POST | `posts/star` | `{posts_id}` |
| 评论 / 回复 | POST | `posts/comment` | `{posts_id, content, parent_id, root_parent, comment_pic, atInfo}` |
| 删评论 | DELETE | `posts/deleteComment` | `{comment_id}` |
| 转发到动态 | POST | `posts/relay` | `{atInfo, content, scope, posts_id}` |

- `type`：**1 = 帖子，2 = 评论**。同一个 `posts/like` 两用。
  （`PostDetail.DNNmRVUo.js` 里 `{id:Ha.value,type:1}`；
  `Comment.CMSwEVrf.js` 里 `{id:e.id,type:2}`）
- **点赞和收藏都是切换，不是幂等的 set。** 返回 `data == 1` 表示已赞/已收，
  `data == -1` 表示已取消。前端就是照这个 ±1 改本地计数的。
- 评论：顶层评论 `parent_id = "0"`、`root_parent = "0"`；
  回复某条评论时 `parent_id` = 被回复的评论 id，`root_parent` = 楼层根 id
  （被回复的那条本身是顶层时，两者相同）。
  `comment_pic` 是逗号拼接的图片 URL 串，没有图就空字符串。
  `atInfo` 是 @ 的人的数组，没有就空数组。
  `content` 是 HTML（表情写成 `[emoN]`，和读取时同一套）。
  （`CommentBox.CI5TVcRq.js`，按 `uptype` 分 dynamic/guild/posts/wiki/recruit 五路，
  我们要的是 `posts` 那一路）

## 幻化

基址 `.../api/home/glamour/`

| 操作 | 方法 | 路径 | 请求体 |
|---|---|---|---|
| 点赞（切换） | POST | `glamour/like` | `{id}` |
| 收藏 | POST | `glamour/favorite` | `{id, favorite_id}` |
| 取消收藏 | POST | `glamour/cancelFavorite` | `{id}` |
| 我的收藏夹 | GET | `glamour/myFavoritesList` | `{page, limit}` |
| 建收藏夹 | POST | `glamour/createFavorites` | — |

- `glamour/like` 同样是切换，返回 `data` 1 / -1。
- **收藏要先有收藏夹**：`favorite_id` 是收藏夹 id，不是幻化 id。
  网页版的流程（`GlamourItem.vue_...B_8yoUfl.js`）：
  已收藏 → `cancelFavorite{id}`；
  未收藏 → 先 `myFavoritesList{page:1,limit:2}`，
  如果 `count == 1 && rows[0].is_default == 1` 就直接用这个默认夹的 id 收，
  否则弹收藏夹选择框。
- **没有分享接口。** 网页版的"分享"是 `toClipboard(...)` 复制页面链接，
  纯前端。所以 App 里的分享也只能是复制链接 / 转系统分享，不发请求。

## 招募响应

| 招募类型 | 方法 | 路径 | 请求体 |
|---|---|---|---|
| 副本 | POST | `responseRecruitFb` | `{id, contact_info}` |
| 部队 | POST | `responseRecruitGuild` | `{id, contact_info}` |
| 其他 | POST | `responseRecruitOther` | `{id, contact_info}` |
| 新人 | POST | `responseNoviceEntertain` | `{id, contact_info}` |

- 四种招募各一个端点，请求体一样：`id` 是招募 id（字符串），
  `contact_info` 是**你自己**留给对方的联系方式，不能为空
  （网页版空的时候提示"请填写您的联系信息"）。
- **响应成功后返回 `data.recruit_contact_info` —— 发布者的联系方式。**
  这就是"响应"的实际作用：交换联系方式。
- 招募详情里的 `is_response == 1` 表示你已经响应过；
  `contact_info_mask` 是打码的发布者联系方式（响应前只能看到这个）。
  （`RecruitMiniView.D5jvq1ZW.js`，三处结构相同）

## 顺带确认的读接口

- `userRelation/fansList` **确实存在**（`GET`）。之前代码里这条路径是照
  `followList` 对称猜的，标着"未验证"——现在从 JS 里读到了，是真的。
  同组还有 `userRelation/follow` / `cancelFollow` / `blockUser` /
  `cancelBlock` / `blockList` / `bulkFollow` / `getUnFollowFriend`。
- `posts/postsSubCommentDetail`：楼中楼（子评论）分页，现在没用上。
- `posts/vote` `{posts_id, options}`：帖子投票。
- `sysMsg/*`：站内消息（`commentMsg` / `likeMyMsg` / `atMyMsg` /
  `myRecruitResponse` …），整块都没做。

## 收藏列表（四类，各走各的接口）

官网 `MeCollections.BT2oNOnP.js` / `MeGlamourCollections.Bv6Lmh2v.js`。
四个标签是 `posts` / `strats` / `rp` / `glamour`，但**不是一个接口切参数**：

| 标签 | 方法 | 路径 | 参数 |
|---|---|---|---|
| 帖子 | GET | `userInfo/myStarPosts` | `{type:1, page, limit}` |
| 攻略 | GET | `userInfo/myStarPosts` | `{type:2, page, limit}` |
| RP | GET | `recruit/homePageStarRecruitRp` | 无 |
| 幻化 | GET | `glamour/myFavoriteItemsList` | `{favorite_id, page, limit, uuid?}` |

- **`type` 是必填的。** 漏了服务端回 `"Type不正确"`，不是空列表 ——
  之前我的实现就漏了这个，所以收藏页一直显示"没读取到 / Type不正确"。
  取值和 `posts/postsList` 的 type 一致（1 帖子 / 2 攻略）。
- RP 那条**不带任何参数、没有分页**，一次返回全部。官网拿到 rows 之后
  自己补 `is_star:true`（收藏列表里的必然已收藏，接口不重复说）。
- 幻化收藏是**两层**：先 `glamour/myFavoritesList` `{page, limit, uuid?}`
  拿收藏夹（`rows[].id / name / is_default`），再用夹子 id 查
  `myFavoriteItemsList`。没有收藏夹就是没收藏过，是空不是错。
  行结构和幻化列表同构（`GlamourItem` 复用）。
- `uuid` 只在看**别人**的收藏时才带；看自己的官网传的是 `undefined`。
- 同组还有 `glamour/deleteFavorites`（DELETE `{id}`）建/删收藏夹，没做。

### 收藏夹里的幻化行：主键是 `glamour_id`，不是 `id`

`glamour/myFavoriteItemsList` 的行**和幻化列表不同构**，别复用同一个解析：

| 字段 | 说明 |
|---|---|
| `glamour_id` | 幻化 id。**查详情要用这个**，官网链接是 `/glamour/detail/{glamour_id}` |
| `id` | 收藏记录自己的 id（拿它查详情查不到） |
| `title` / `main_image` / `uuid` | 有 |
| `is_valid` | 0 = 原作已删。官网这时不让点，标"已失效" |
| `added_at` | 收藏时间 |

**没有** `likes` / `favorites` / `character_name` / `area_name` / `group_name`
——所以卡片上的计数和作者行要按"没有这个数"处理，不能画 0。

（实测踩过：用 `id` 建卡片，列表能画出来但点进去详情是空的。）

## IP 属地：`ip_location`

**是省级地区名，不是 IP 地址。** 实测值：`中国上海市` / `中国浙江省` /
`中国广西壮族自治区`。

| 接口 | 有没有 |
|---|---|
| `posts/postsDetail` | **有**（`data.ip_location`） |
| `posts/postsCommentDetail` | **有**，每条评论各一个 |
| `posts/postsList` | 没有 |
| 招募详情 | 有 |
| `userInfo/getUserInfo` | 需登录，没验证 |

- 有些记录是空字符串，空就别显示。
- **没有"常驻 IP"这种字段。** 帖子详情里内嵌的 `userInfo` 里一个 IP 相关的
  字段都没有，只有一堆 `*_publish` 开关（`washing_num_publish`、
  `house_info_publish` …，那些是资料页各项的可见性开关）。
- 官网 PC 端 155 个 chunk 里 `ip_location` **一次都没出现**，移动端只在
  "发完评论本地先插一条"的占位对象里写了 `ip_location:""`。
  也就是说两端都拿到了这个字段但都没显示出来 —— 我们显示它是主动选择。
