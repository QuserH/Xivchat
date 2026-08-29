package com.quserh.eorzeaphone.data.wiki

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 「选一个任务，把它前面所有的前置线完整展开」。
 *
 * 和 [QuestDb.tree] 的区别：那个画的是**一个块**（同 Category + 共享入口），
 * 只带块内的边和一层入口，所以往上只看得到一级前置。这里走的是
 * `quest_prereq` 的**传递闭包**，跨块，一直走到没有前置为止。
 *
 * ## 上溯到**前置主线**就停
 *
 * 这是用户明确要的：「只希望展示那些非主线任务的任务链，顶部是前置主线就可以了。
 * 如果搜索的是主线的，那主线任务链就让玩家自己展开」。
 *
 * 主线一路往上是贯穿所有资料片的长链，展开它没有意义。所以 BFS 碰到主线任务
 * （`category` 含「主线」，全库 1054 个）就把它收进「顶部」并**停止**，不再往上。
 *
 * 这一条把规模从「不可看」变成「一眼看完」（实测，见 §6.12）：
 *
 * | | 全闭包（做过一版，弃用） | 到主线为止 |
 * | --- | --- | --- |
 * | 中位节点数 | 387 | **3** |
 * | P90 | 908 | **19** |
 * | ≤40 个节点 | — | **98.3%** |
 *
 * ## 还是保留了折叠长直链
 *
 * 剩下 75 个非主线任务超过 40 个节点，最多 194 个 —— 那是相关武器
 * （注入武器的灵魂 / 黄道武器那一串）的强化链，它**真的**有一百多个
 * 非主线前置。对这些把连续直链压成「⋯ N 个任务 ⋯」（可展开），
 * 194 → 116，比现在最大的块（255）还少，画得动。
 *
 * 分叉点、汇合点、顶部主线、以及选中的那个任务都不折叠 ——
 * 那些是结构信息，折了就看不出递进关系。
 *
 * ## 为什么在运行时算而不是构建期
 *
 * 块布局是 468 个固定的，构建期算好存库。祖先树是**每个任务一份**（5360 份），
 * 预存要 50 万行。而算一次只是一趟 BFS + 一趟拓扑，几毫秒，运行时算更划算。
 */

/** 祖先树里的一个格子：要么是一个任务，要么是一段被折叠的直链。 */
sealed interface AncestorCell {
    val col: Int
    val row: Int

    /** 在「选中任务的直接上溯路径」上。UI 用它加粗主线。两个分支都有，所以提到接口。 */
    val onPath: Boolean

    /**
     * 一个具体任务。
     *
     * [isTarget] = 用户选中的那个；[isEntry] = 没有前置的起点；
     * [isMsqTop] = 顶部的前置主线（上溯到这里就停了，它自己的前置没有展开）。
     */
    data class Quest(
        val node: QuestNode,
        override val col: Int,
        override val row: Int,
        val isTarget: Boolean,
        val isEntry: Boolean,
        val isMsqTop: Boolean,
        override val onPath: Boolean,
    ) : AncestorCell

    /**
     * 一段折叠掉的直链。[count] 个任务，[headId] 是这段最靠上（最早）的那个，
     * [tailId] 是最靠下的。点开后 UI 可以列出 [ids]。
     */
    data class Run(
        val ids: List<Int>,
        /** 和 [ids] 一一对应的任务名。展开时要逐个列出来，只存头尾不够。 */
        val names: List<String>,
        override val col: Int,
        override val row: Int,
        override val onPath: Boolean,
    ) : AncestorCell {
        val count: Int get() = ids.size
        val headName: String get() = names.firstOrNull().orEmpty()
        val tailName: String get() = names.lastOrNull().orEmpty()
    }
}

/** 祖先树上的一条边，端点是 [AncestorCell] 的下标。 */
data class AncestorEdge(val fromCell: Int, val toCell: Int, val onPath: Boolean)

/**
 * 一棵可画的祖先树。
 *
 * [cells] 已经带好了 (col, row)，[cols]/[rows] 是画布尺寸（格数）。
 */
data class AncestorTree(
    val target: QuestNode,
    val cells: List<AncestorCell>,
    val edges: List<AncestorEdge>,
    val cols: Int,
    val rows: Int,
    /** 展开前的真实祖先数，用来告诉用户「折叠了多少」。 */
    val totalAncestors: Int,
    val collapsedCount: Int,
    /** 顶部那几个前置主线的任务 ID。UI 提供「展开主线」的入口。 */
    val msqTops: List<Int>,
    /** true = 选中的这个本身就是主线任务，它的链默认不展开。 */
    val targetIsMsq: Boolean,
)

/**
 * 折叠图里一个节点的标识：[isRun] 时 [id] 是折叠段的序号，否则是任务 ID。
 *
 * 提成顶层类型（而不是 build() 里的局部 data class）纯粹是为了能在
 * 辅助函数里直接读字段 —— 局部类型只能靠反射拿，又慢又易碎。
 */
private data class Key(val isRun: Boolean, val id: Int)

object QuestAncestry {
    /** 安全上限。实测最大 1090，留一倍余量防脏数据成环。 */
    private const val MAX_ANCESTORS = 2500

    /** 一层最多几个，超了折行。和 quest_chain.py 的 WRAP 一致。 */
    private const val WRAP = 10

    /**
     * 这个规模以内**完全不折叠** —— 每个任务都单独显示。
     *
     * 上溯到主线为止之后，98.3% 的非主线任务在 40 个节点以内（中位 3）。
     * 那种规模屏幕装得下，折叠反而把用户想看的链藏起来了：真机上
     * 「永远的梦园」只有 6 个前置，却被折成「⋯ 5 个 ⋯」一个盒子，
     * 等于什么都没说。
     *
     * 折叠只为救那 75 个相关武器强化链（最多 194 个节点）。
     */
    private const val NO_COLLAPSE_LIMIT = 40

    /**
     * 取 [questId] 的完整前置树。
     *
     * 一次把需要的行全捞出来（边表 + 涉及到的任务），然后纯内存算，
     * 避免在递归里反复查库。
     */
    suspend fun of(context: Context, questId: Int): AncestorTree? =
        withContext(Dispatchers.IO) {
            val target = QuestDb.byId(context, questId) ?: return@withContext null
            val db = WikiDb.open(context)

            // 1. 反向 BFS 收祖先。边表整张读进内存 —— 只有 5374 行，
            //    比逐层发 SQL 快得多，也不用担心递归深度。
            val preOf = HashMap<Int, MutableList<Int>>(6000)
            val nextOf = HashMap<Int, MutableList<Int>>(6000)
            db.rawQuery("SELECT quest_id, pre_id FROM quest_prereq", null).use { c ->
                while (c.moveToNext()) {
                    val q = c.getInt(0)
                    val p = c.getInt(1)
                    preOf.getOrPut(q) { mutableListOf() }.add(p)
                    nextOf.getOrPut(p) { mutableListOf() }.add(q)
                }
            }

            // 2. 主线任务集合。碰到它就停，不再往上 —— 主线一路上溯是贯穿
            //    所有资料片的长链，展开没意义。判据是 category 含「主线」
            //    （1054 个，实测是 type 判据的严格超集）。
            val msq = HashSet<Int>(1200)
            db.rawQuery(
                "SELECT id FROM quests WHERE category LIKE '%主线%'", null,
            ).use { c -> while (c.moveToNext()) msq.add(c.getInt(0)) }

            val targetIsMsq = questId in msq

            val anc = LinkedHashSet<Int>()
            val tops = LinkedHashSet<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(questId)
            while (queue.isNotEmpty() && anc.size < MAX_ANCESTORS) {
                val u = queue.removeFirst()
                for (p in preOf[u].orEmpty()) {
                    if (p in msq) {
                        // 顶部主线：收进来当树顶，但不展开它的前置
                        tops.add(p)
                    } else if (anc.add(p)) {
                        queue.add(p)
                    }
                }
            }

            if (anc.isEmpty() && tops.isEmpty()) {
                // 没有前置：只画它自己
                return@withContext AncestorTree(
                    target = target,
                    cells = listOf(
                        AncestorCell.Quest(
                            target, 0, 0, isTarget = true, isEntry = true,
                            isMsqTop = false, onPath = true,
                        ),
                    ),
                    edges = emptyList(), cols = 1, rows = 1,
                    totalAncestors = 0, collapsedCount = 0,
                    msqTops = emptyList(), targetIsMsq = targetIsMsq,
                )
            }

            val sub = HashSet(anc).apply { addAll(tops); add(questId) }
            val nodes = loadNodes(db, sub)

            build(
                target, sub, nodes, preOf, nextOf,
                totalAncestors = anc.size + tops.size,
                msqTops = tops.toList(),
                targetIsMsq = targetIsMsq,
            )
        }

    /**
     * 折叠直链 + 分层 + 摆位。
     *
     * [sub] = 祖先 ∪ {target}，度数**只在 sub 内**算 —— 块外的后继不算分叉，
     * 否则几乎每个任务都成了分叉点，折叠就失效了。
     */
    private fun build(
        target: QuestNode,
        sub: Set<Int>,
        nodes: Map<Int, QuestNode>,
        preOf: Map<Int, List<Int>>,
        nextOf: Map<Int, List<Int>>,
        totalAncestors: Int,
        msqTops: List<Int>,
        targetIsMsq: Boolean,
    ): AncestorTree {
        val topSet = msqTops.toHashSet()
        fun preIn(id: Int) = preOf[id].orEmpty().filter { it in sub }
        fun nextIn(id: Int) = nextOf[id].orEmpty().filter { it in sub }

        // --- 1. 哪些可以折叠 ---
        // 小树完全不折（见 NO_COLLAPSE_LIMIT）：装得下就把每个任务都显示出来，
        // 那才是「完整展示任务链」。
        // 大树才折：单前置 + 单后继，且它的前置也只有它这一个后继
        // （保证是一条直链的中段）。target、顶部主线、入口都永不折叠。
        val foldable = if (sub.size <= NO_COLLAPSE_LIMIT) {
            HashSet()
        } else {
            sub.filter { id ->
                if (id == target.id || id in topSet) return@filter false
                val p = preIn(id)
                if (p.size != 1 || nextIn(id).size != 1) return@filter false
                nextIn(p[0]).size == 1
            }.toHashSet()
        }

        // --- 2. 把相邻的可折叠节点串成段（沿唯一前置往上走） ---
        val runOf = HashMap<Int, Int>()          // questId -> 段号
        val runs = mutableListOf<MutableList<Int>>()
        for (id in sub) {
            if (id !in foldable || id in runOf) continue
            // 找到这一段最靠上的那个
            var head = id
            while (true) {
                val p = preIn(head).firstOrNull() ?: break
                if (p in foldable && p !in runOf) head = p else break
            }
            // 从 head 往下收
            val seg = mutableListOf<Int>()
            var cur: Int? = head
            while (cur != null && cur in foldable && cur !in runOf) {
                seg.add(cur)
                runOf[cur] = runs.size
                cur = nextIn(cur).firstOrNull()
            }
            if (seg.isNotEmpty()) runs.add(seg)
        }

        // --- 3. 折叠后的图：节点 = 保留的任务 + 每段一个 ---
        val kept = sub.filter { it !in foldable }

        fun keyOf(questId: Int): Key =
            runOf[questId]?.let { Key(true, it) } ?: Key(false, questId)

        val keys = LinkedHashSet<Key>()
        kept.forEach { keys.add(Key(false, it)) }
        runs.indices.forEach { keys.add(Key(true, it)) }

        // 折叠图的边。
        //
        // **不画指向顶部主线的边**：既然主线不展开，就不该显示「什么喂给它」。
        // 会漏出来是因为某个非主线祖先恰好也是这条主线的前置（实测有 2 例，
        // 「深宇宙探查学的实地研习」那两个），那条边画出来就像主线被部分展开了。
        // 节点本身留着是对的 —— 它经非主线路径确实在这棵树里。
        val cPre = HashMap<Key, MutableSet<Key>>()
        val cNext = HashMap<Key, MutableSet<Key>>()
        for (q in sub) {
            if (q in topSet) continue
            for (p in preIn(q)) {
                val a = keyOf(p)
                val b = keyOf(q)
                if (a == b) continue          // 段内部的边，吃掉
                cPre.getOrPut(b) { mutableSetOf() }.add(a)
                cNext.getOrPut(a) { mutableSetOf() }.add(b)
            }
        }

        // --- 4. 分层：最长路径，从入口往下 ---
        val depth = HashMap<Key, Int>()
        val indeg = keys.associateWithTo(HashMap()) { cPre[it]?.size ?: 0 }
        val dq = ArrayDeque(keys.filter { indeg[it] == 0 })
        keys.forEach { depth[it] = 0 }
        var processed = 0
        while (dq.isNotEmpty()) {
            val u = dq.removeFirst()
            processed++
            for (v in cNext[u].orEmpty()) {
                depth[v] = maxOf(depth[v] ?: 0, (depth[u] ?: 0) + 1)
                indeg[v] = (indeg[v] ?: 1) - 1
                if (indeg[v] == 0) dq.add(v)
            }
        }
        // 有环兜底：没处理到的挂到最底下
        if (processed < keys.size) {
            val maxD = depth.values.maxOrNull() ?: 0
            keys.filter { (indeg[it] ?: 0) > 0 }.forEach { depth[it] = maxD + 1 }
        }
        // target 强制放最后一层，它是「终点」
        val targetKey = keyOf(target.id)
        val bottom = (depth.values.maxOrNull() ?: 0)
        depth[targetKey] = bottom

        // --- 5. 选中任务的直接上溯路径（UI 加粗这条） ---
        val onPath = HashSet<Key>()
        run {
            val stack = ArrayDeque<Key>()
            stack.add(targetKey)
            onPath.add(targetKey)
            // 只沿「最长路径的前驱」上溯一条主线，避免整张图都被标成主线
            while (stack.isNotEmpty()) {
                val u = stack.removeFirst()
                val best = cPre[u].orEmpty().maxByOrNull { depth[it] ?: 0 }
                if (best != null && onPath.add(best)) stack.add(best)
            }
        }

        // --- 6. 摆位：按层排，层内宽了折行 ---
        val byDepth = keys.groupBy { depth[it] ?: 0 }.toSortedMap()
        val pos = HashMap<Key, Pair<Int, Int>>()
        var row = 0
        var maxCol = 0
        for ((_, group) in byDepth) {
            // 主线优先放最左，其余按名字稳定排序，保证同一次打开顺序一致
            val ordered = group.sortedWith(
                compareByDescending<Key> { it in onPath }
                    .thenBy { labelOf(it, nodes, runs) },
            )
            ordered.forEachIndexed { i, k ->
                val c = i % WRAP
                pos[k] = c to (row + i / WRAP)
                if (c > maxCol) maxCol = c
            }
            row += ((ordered.size + WRAP - 1) / WRAP).coerceAtLeast(1)
        }

        // --- 7. 出 cells / edges ---
        val order = keys.toList()
        val idx = order.withIndex().associate { (i, k) -> k to i }
        val cells = order.map { k ->
            val (c, r) = pos[k] ?: (0 to 0)
            if (k.isRun) {
                val seg = runs[k.id]
                AncestorCell.Run(
                    ids = seg,
                    names = seg.map { nodes[it]?.name.orEmpty() },
                    col = c, row = r, onPath = k in onPath,
                )
            } else {
                val n = nodes[k.id] ?: target
                AncestorCell.Quest(
                    node = n, col = c, row = r,
                    isTarget = k.id == target.id,
                    isEntry = preIn(k.id).isEmpty(),
                    isMsqTop = k.id in topSet,
                    onPath = k in onPath,
                )
            }
        }
        val edges = buildList {
            for ((a, bs) in cNext) {
                val ai = idx[a] ?: continue
                for (b in bs) {
                    val bi = idx[b] ?: continue
                    add(AncestorEdge(ai, bi, a in onPath && b in onPath))
                }
            }
        }
        return AncestorTree(
            target = target,
            cells = cells,
            edges = edges,
            cols = maxCol + 1,
            rows = row,
            totalAncestors = totalAncestors,
            collapsedCount = runs.sumOf { it.size },
            msqTops = msqTops,
            targetIsMsq = targetIsMsq,
        )
    }

    /** 层内排序用的稳定键，保证同一次打开顺序一致。 */
    private fun labelOf(
        k: Key,
        nodes: Map<Int, QuestNode>,
        runs: List<List<Int>>,
    ): String =
        if (k.isRun) nodes[runs[k.id].firstOrNull() ?: 0]?.name.orEmpty()
        else nodes[k.id]?.name.orEmpty()

    private fun loadNodes(
        db: android.database.sqlite.SQLiteDatabase,
        ids: Set<Int>,
    ): Map<Int, QuestNode> {
        val out = HashMap<Int, QuestNode>(ids.size * 2)
        // SQLite 的参数上限是 999，分批
        ids.chunked(900).forEach { chunk ->
            val ph = chunk.joinToString(",") { "?" }
            db.rawQuery(
                "SELECT ${QuestDb.colsForAncestry()} FROM quests WHERE id IN ($ph)",
                chunk.map { it.toString() }.toTypedArray(),
            ).use { c ->
                while (c.moveToNext()) {
                    val n = QuestDb.readNodePublic(c)
                    out[n.id] = n
                }
            }
        }
        return out
    }
}
