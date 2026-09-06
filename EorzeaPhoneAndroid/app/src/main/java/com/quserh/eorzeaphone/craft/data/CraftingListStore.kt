package com.quserh.eorzeaphone.craft.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Crafting lists ("制作清单"), persisted as one JSON file. Follows the plain
 * JSON-file pattern the phone app uses instead of a Room dependency.
 */
class CraftingListStore(context: Context) {

    private val file = File(context.filesDir, "craft_lists.json")
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private val writeVersion = AtomicLong(0)

    /** All lists; Compose-observable so the UI recomposes on edits. */
    val lists = mutableStateListOf<CraftList>()

    init {
        load()
        if (lists.isEmpty()) {
            addList("我的第一个清单")
        }
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText())
            val arr = root.getJSONArray("lists")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val list = CraftList(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    updatedMs = obj.optLong("updated", 0L),
                )
                val entries = obj.getJSONArray("entries")
                for (j in 0 until entries.length()) {
                    val e = entries.getJSONObject(j)
                    list.entries.add(ListEntry(e.getInt("itemId"), e.getInt("qty")))
                }
                lists.add(list)
            }
        }
    }

    /**
     * Persist a snapshot off the main thread. The old implementation wrote the
     * complete JSON document synchronously on every quantity tap, which made a
     * long edit sequence visibly hitch. Newer snapshots supersede older queued
     * writes; the temporary file keeps a killed write from corrupting the store.
     */
    @Synchronized
    fun save() {
        val payload = JSONObject().apply {
            put("lists", JSONArray().apply {
                lists.forEach { list ->
                    put(JSONObject().apply {
                        put("id", list.id)
                        put("name", list.name)
                        put("updated", list.updatedMs)
                        put("entries", JSONArray().apply {
                            list.entries.forEach { entry ->
                                put(JSONObject().put("itemId", entry.itemId).put("qty", entry.qty))
                            }
                        })
                    })
                }
            })
        }.toString()
        val version = writeVersion.incrementAndGet()
        ioScope.launch {
            writeMutex.withLock {
                if (version != writeVersion.get()) return@withLock
                val tmp = File(file.parentFile, "${file.name}.tmp")
                runCatching {
                    tmp.writeText(payload)
                    if (!tmp.renameTo(file)) {
                        file.delete()
                        check(tmp.renameTo(file)) { "无法替换制作清单文件" }
                    }
                }
            }
        }
    }

    fun addList(name: String): CraftList {
        val list = CraftList(id = UUID.randomUUID().toString().take(8), name = name)
        lists.add(list)
        save()
        return list
    }

    fun removeList(id: String) {
        lists.removeAll { it.id == id }
        save()
    }

    fun renameList(id: String, name: String) {
        lists.firstOrNull { it.id == id }?.let { it.name = name; it.updatedMs = System.currentTimeMillis() }
        save()
    }

    fun listById(id: String): CraftList? = lists.firstOrNull { it.id == id }

    fun addEntry(listId: String, itemId: Int, qty: Int = 1) {
        val list = listById(listId) ?: return
        val existing = list.entries.firstOrNull { it.itemId == itemId }
        if (existing != null) {
            val index = list.entries.indexOf(existing)
            list.entries[index] = existing.copy(qty = existing.qty + qty)
        } else {
            list.entries.add(ListEntry(itemId, qty))
        }
        list.updatedMs = System.currentTimeMillis()
        save()
    }

    fun setQty(listId: String, itemId: Int, qty: Int) {
        val list = listById(listId) ?: return
        val existing = list.entries.firstOrNull { it.itemId == itemId } ?: return
        val index = list.entries.indexOf(existing)
        if (qty <= 0) list.entries.removeAt(index) else list.entries[index] = existing.copy(qty = qty)
        list.updatedMs = System.currentTimeMillis()
        save()
    }

    fun removeEntry(listId: String, itemId: Int) {
        listById(listId)?.let { list ->
            list.entries.removeAll { it.itemId == itemId }
            list.updatedMs = System.currentTimeMillis()
        }
        save()
    }

    /** Remove several entries in one transaction, used by the list detail multi-select UI. */
    fun removeEntries(listId: String, itemIds: Set<Int>) {
        if (itemIds.isEmpty()) return
        listById(listId)?.let { list ->
            list.entries.removeAll { it.itemId in itemIds }
            list.updatedMs = System.currentTimeMillis()
        }
        save()
    }
}
