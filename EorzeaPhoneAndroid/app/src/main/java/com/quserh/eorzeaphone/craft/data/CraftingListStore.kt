package com.quserh.eorzeaphone.craft.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Crafting lists ("制作清单"), persisted as one JSON file. Follows the plain
 * JSON-file pattern the phone app uses instead of a Room dependency.
 */
class CraftingListStore(context: Context) {

    private val file = File(context.filesDir, "craft_lists.json")

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

    @Synchronized
    fun save() {
        val root = JSONObject()
        val arr = JSONArray()
        for (list in lists) {
            val obj = JSONObject()
            obj.put("id", list.id)
            obj.put("name", list.name)
            obj.put("updated", list.updatedMs)
            val entries = JSONArray()
            for (e in list.entries) {
                entries.put(JSONObject().put("itemId", e.itemId).put("qty", e.qty))
            }
            obj.put("entries", entries)
            arr.put(obj)
        }
        root.put("lists", arr)
        file.writeText(root.toString())
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
}
