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

    /**
     * 交互高亮：从选中的格子**向上可达的全部祖先**，以及可达集内部的全部边。
     *
     * 需求是「点一个任务，它的所有前置线都连到顶」，**不是最短路径树**。
     * 一个任务有两个前置就该亮两条 —— 这是用户明确要的。
     *
     * ## 为什么不再用 Dijkstra
     *
     * 上一版是最短路径树（`bestEdge` 每个节点存一条入边）。它有个更早的
     * 历史包袱：最初 BFS 的「首达边」在深层选中时会选错，于是改成按距离松弛。
     * 但**只要是「树」就注定一个节点只留一条边**，而这里需要的是子图不是树。
     *
     * 更要紧的是 `bestEdge` 的 key 是**源节点**，所以一个前置只能留一条出边。
     * 这个 bug 长期存在但不可触发：`build()` 里那条「一源一边剪枝」原本无条件
     * 删边，删完每个节点出度必然 ≤ 1，key 永不冲突。我给剪枝加上可达性检查
     * （兄弟边不再被误删，见 step 3.5）之后出度可以 > 1，它就暴露了。
     *
     * 实测（不折叠的树，即 ≤ [NO_COLLAPSE_LIMIT] 个节点）：出度 > 1 的
     * 437 棵（其中非主线 435 棵 + 主线 2 棵），**全部丢边，无一例外**。
     * 最惨「友谊地久天长」应亮 51 条只亮 37 条 —— `石卫塔霸主` 喂 5 个已亮任务，
     * 只有 `来自深海` 那条边活下来，其余画成暗线。
     * 诊断由另一个会话（claude-ee）提出，我独立复现后确认。
     *
     * ## 为什么「集内边全亮」不会多亮
     *
     * 边的方向是 前置 → 后继。取到一条边 x→y 时：y 在可达集里，说明 y 是
     * fromCell 的祖先（或它本身）；x 是 y 的前置。那么 x→y→…→fromCell
     * 本身就是一条上溯路径，所以这条边天然在路径上。
     * 全库实测：漏亮 0、多亮 0，高亮**节点集**与旧版逐棵一致（3348/3348）。
     *
     * 碰到顶部主线就停（它自己的前置不在图里），但指进主线的边照亮 ——
     * 「高亮线要连到顶部主线」靠的就是这些边。
     */
    fun pathFrom(tree: AncestorTree, fromCell: Int): Pair<Set<Int>, Set<Pair<Int, Int>>> {
        if (fromCell !in tree.cells.indices) return emptySet<Int>() to emptySet()
        val isTop = HashSet<Int>()
        tree.cells.forEachIndexed { i, c ->
            if (c is AncestorCell.Quest && c.isMsqTop) isTop.add(i)
        }
        val pre = HashMap<Int, MutableList<Int>>()
        tree.edges.forEach { e -> pre.getOrPut(e.toCell) { mutableListOf() }.add(e.fromCell) }

        // 向上可达集
        val lit = HashSet<Int>()
        lit.add(fromCell)
        val stack = ArrayDeque<Int>()
        stack.add(fromCell)
        while (stack.isNotEmpty()) {
            val u = stack.removeFirst()
            if (u in isTop && u != fromCell) continue   // 主线：终点，不再上溯
            for (a in pre[u].orEmpty()) if (lit.add(a)) stack.add(a)
        }

        val litEdges = HashSet<Pair<Int, Int>>()
        tree.edges.forEach { e ->
            if (e.fromCell in lit && e.toCell in lit) litEdges.add(e.fromCell to e.toCell)
        }
        return lit to litEdges
    }

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
                        // 顶部主线：收进来当树顶，不展开它的前置 ——
                        // 上一版改成「穿过主线继续收集」把整条主线历史都列出来了，
                        // 那不是需求；需求只是高亮不断线，树本身仍然到主线为止。
                        tops.add(p)
                        continue
                    }
                    if (anc.add(p)) queue.add(p)
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
        // 边全部保留，包括指进顶部主线的：用户要的是「点任意任务，
        // 高亮线直接连到顶部主线」——掐掉进主线的边，那条线就画不出来。
        // 主线自己的前置不在 sub 里（BFS 碰到主线就停了），不会被展开。
        val cPre = HashMap<Key, MutableSet<Key>>()
        val cNext = HashMap<Key, MutableSet<Key>>()
        for (q in sub) {
            for (p in preIn(q)) {
                val a = keyOf(p)
                val b = keyOf(q)
                if (a == b) continue          // 段内部的边，吃掉
                cPre.getOrPut(b) { mutableSetOf() }.add(a)
                cNext.getOrPut(a) { mutableSetOf() }.add(b)
            }
        }

        // --- 3.2 顶部主线之间的边不画：主线一律待在第 0 层 ---
        //
        // 用户原话：「主线就老老实实放在一层，不要跑到二层去了」。
        //
        // 机制：BFS 碰到主线就停，但那只挡住「从这条主线继续往上」。若两条主线
        // 各自被不同的非主线后代收进 tops，而它们彼此又有前置关系，那条边照样
        // 会被建出来（建边是对 sub 内所有对做的）。于是后一条主线有了入边、
        // 不再是 root，被压到第 1 层，再往下第 2 层。
        //
        // 实测最深的「丈夫的危机」：闪耀的明星 → 试炼的第一步 → 模儿一族
        // 三条主线串成一条，被排在第 0/1/2 层。
        //
        // 主线内部的推进顺序本来就不是这一页要表达的东西（主线不展开），
        // 画出来等于「主线被部分展开」—— 正是造成困惑的原因。去掉之后所有 top
        // 都没有入边，自然全部落在第 0 层。
        //
        // 全库实测（4402 棵不折叠的树）：受影响 69 棵，去掉 96 条边，
        // 之后「仍有 top 不在第 0 层」的 **0 棵**、target 深度变化 **0 棵**、
        // 「top 变得从 target 到不了」的 **0 棵** —— 每个 top 仍然经非主线路径
        // 连得上，高亮不断线（那 96 条里原本有 88 条是亮的，但它们连的是
        // 主线↔主线，不是 target 上溯路径的必要部分）。
        //
        // 这个现象和我改剪枝无关：修前 66 棵、修后 69 棵，差的 3 棵只是
        // 保住的兄弟边让更多主线进了同一棵树。
        // 顶部主线永不折叠（见上面 kept 的 filter），所以 !isRun 就够判定
        fun isTopKey(k: Key) = !k.isRun && k.id in topSet
        if (topSet.size > 1) {
            for (a in cNext.keys.toList()) {
                if (!isTopKey(a)) continue
                for (b in cNext[a].orEmpty().toList()) {
                    if (!isTopKey(b)) continue
                    cNext[a]?.remove(b)
                    cPre[b]?.remove(a)
                    if (cPre[b]?.isEmpty() == true) cPre.remove(b)
                }
            }
        }

        // --- 3.5 一源一边剪枝（用户规则）：一个前置 gated **一条线上**的多个任务时，
        // 只连「最早的那一个」—— 比如残酷的真相同时前置龙诗之始和好想回家，
        // 只画 残酷的真相→龙诗之始（最早），它经由虎口拔牙照样包含好想回家，
        // 直连好想回家的重复边不再出现。
        //
        // **删边前必须确认「绕路也能到」**，也就是只做传递归约。
        //
        // 这一步原来是无条件删的：一个源有多个后继就只留 preDepth 最小的那个。
        // 但「一条线上」是这条规则成立的前提 —— 两个后继是**兄弟**（互不可达）时，
        // 被删的那个就失去了全部入边，于是在下面第 4 步被当成 root、画到第 0 层，
        // 和顶部主线并排。
        //
        // 用户报的就是这个：「它们的目标」的两个前置 莫古呜咔的不安 / 莫古欧库的预感
        // 都由 紧急情报 gate，preDepth 都是 8，按 id 比较留下了前者，
        // 后者被删成孤儿 → depth 0 → 跑到主线那一层，而且它自己的前置
        // （紧急情报）也就画不出来了。
        //
        // 全库实测：修前 4477 棵树里有 460 棵中招，808 个节点被误顶到第 0 层；
        // 加上可达性检查后是 0。边数 28910 → 30201（+4.5%，就是这些该留的边）。
        // 复现与回归见 `开发/WIKI/wiki-feature/validate_ancestry_prune.py`。
        val preDepth = HashMap<Key, Int>()
        val preIndeg = keys.associateWithTo(HashMap()) { cPre[it]?.size ?: 0 }
        val pdq = ArrayDeque(keys.filter { preIndeg[it] == 0 })
        keys.forEach { preDepth[it] = 0 }
        while (pdq.isNotEmpty()) {
            val u = pdq.removeFirst()
            for (v in cNext[u].orEmpty()) {
                preDepth[v] = maxOf(preDepth[v] ?: 0, (preDepth[u] ?: 0) + 1)
                preIndeg[v] = (preIndeg[v] ?: 1) - 1
                if ((preIndeg[v] ?: 0) == 0) pdq.add(v)
            }
        }

        /** 不走 a→b 这条直接边，b 还能从 a 到达吗。图是 DAG，深度有限。 */
        fun reachableWithout(a: Key, b: Key): Boolean {
            val stack = ArrayDeque(cNext[a].orEmpty().filter { it != b })
            val seen = HashSet(stack)
            while (stack.isNotEmpty()) {
                val u = stack.removeFirst()
                if (u == b) return true
                for (v in cNext[u].orEmpty()) if (seen.add(v)) stack.add(v)
            }
            return false
        }

        for (a in cNext.keys.sortedWith(compareBy({ it.isRun }, { it.id }))) {
            val bs = cNext[a].orEmpty().toList()
            if (bs.size <= 1) continue
            // preDepth 深的先试删：留下的自然是「最早的那一个」，
            // 和原规则的意图一致。判定跑在**当前**边集上，所以不会把
            // 互为理由的两条边同时删掉。
            for (b in bs.sortedWith(compareByDescending<Key> { preDepth[it] ?: 0 }
                    .thenByDescending { it.id })) {
                if ((cNext[a]?.size ?: 0) <= 1) break
                if (!reachableWithout(a, b)) continue   // 兄弟节点，删了就成孤儿
                cNext[a]?.remove(b)
                cPre[b]?.remove(a)
                if (cPre[b]?.isEmpty() == true) cPre.remove(b)
            }
        }

        // --- 4. 流程图布局：根（顶部主线/入口）在最上一行，链垂直下落 ---
        // 深度 = 从根往下的最长路径。目标任务自然落在它所在的层，
        // 其他更长的分支可以垂得更深 —— 流程图本来就是这样的。
        val depth = HashMap<Key, Int>()
        val roots = keys.filter { (cPre[it]?.size ?: 0) == 0 }
        val indeg = keys.associateWithTo(HashMap()) { cPre[it]?.size ?: 0 }
        val dq = ArrayDeque(roots)
        roots.forEach { depth[it] = 0 }
        var processed = 0
        while (dq.isNotEmpty()) {
            val u = dq.removeFirst()
            processed++
            for (v in cNext[u].orEmpty()) {
                depth[v] = maxOf(depth[v] ?: 0, (depth[u] ?: 0) + 1)
                indeg[v] = (indeg[v] ?: 1) - 1
                if ((indeg[v] ?: 0) == 0) dq.add(v)
            }
        }
        // 有环兜底：没处理到的挂到最底
        if (processed < keys.size) {
            val maxD = depth.values.maxOrNull() ?: 0
            keys.filter { depth[it] == null }.forEach { depth[it] = maxD + 1 }
        }

        // --- 5. 选中任务的直接上溯路径（UI 加粗这条） ---
        //
        // **这一条只留一个父**（`maxByOrNull { depth }`），所以它不是「全部前置线」，
        // 一个任务有两个前置时只会标出一条。真正的高亮走 [pathFrom]（可达集语义），
        // `AncestorCell.onPath` / `AncestorEdge.onPath` 只是 `highlightEdges` 为空时的
        // 兜底（`QuestTreeScreen.kt` 那处 `if (highlightEdges.isEmpty()) e.onPath`）。
        //
        // 实测那个兜底走不到：4402 棵不折叠的树里，「有边但 pathFrom 返回空边集」
        // 的有 **0 棵** —— 只要画得出线，pathFrom 就一定给出非空边集。
        // 所以这里保持单父不影响显示。**但别拿这两个字段当「完整前置线」用**，
        // 要那个语义就调 pathFrom。
        val targetKey = keyOf(target.id)
        val onPath = HashSet<Key>()
        run {
            val stack = ArrayDeque<Key>()
            stack.add(targetKey)
            onPath.add(targetKey)
            while (stack.isNotEmpty()) {
                val u = stack.removeFirst()
                val best = cPre[u].orEmpty().maxByOrNull { depth[it] ?: 0 }
                if (best != null && onPath.add(best)) stack.add(best)
            }
        }

        // --- 6. 列：按深度从深到浅领列（叶先领、父取孩子中位；同格被占就右移） ---
        // 两个顶部主线同时只喂一个节点时，按中位会算出同一列、同一层 ——
        // 直接重叠。领列后查占位，被占就整体右移一格。
        val colOf = HashMap<Key, Int>()
        var nextCol = 0
        val occupied = HashSet<Long>()
        val ordered = keys.sortedByDescending { depth[it] ?: 0 }
        for (k in ordered) {
            val r = depth[k] ?: 0
            val kids = cNext[k].orEmpty().mapNotNull { colOf[it] }
            var c = if (kids.isEmpty()) nextCol++ else (kids.min() + kids.max()) / 2
            while ((r.toLong() * 100000L + c) in occupied) c++
            occupied.add(r.toLong() * 100000L + c)
            colOf[k] = c
        }
        // --- 7. 出 cells / edges ---
        val order = keys.toList()
        val idx = order.withIndex().associate { (i, k) -> k to i }
        val cells = order.map { k ->
            val c = colOf[k] ?: 0
            val r = depth[k] ?: 0
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
            cols = nextCol,
            rows = (depth.values.maxOrNull() ?: 0) + 1,
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
