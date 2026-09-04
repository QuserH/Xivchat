package com.quserh.eorzeaphone.craft.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 购物车（临时清单）+ 搜索历史，随制作清单一起持久化。
 * Cart behaves like a shopping cart: items accumulate here from the recipe
 * search screen and are later moved into a real crafting list.
 */
class CartStore(context: Context) {

    val items = mutableStateListOf<ListEntry>()
    val history = mutableStateListOf<String>()

    private val file = File(context.filesDir, "craft_cart.json")

    init {
        load()
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText())
            val cart = root.getJSONArray("cart")
            for (i in 0 until cart.length()) {
                val e = cart.getJSONObject(i)
                items.add(ListEntry(e.getInt("itemId"), e.getInt("qty")))
            }
            val historyArr = root.getJSONArray("history")
            for (i in 0 until historyArr.length()) history.add(historyArr.getString(i))
        }
    }

    @Synchronized
    fun save() {
        val root = JSONObject()
        val cart = JSONArray()
        for (e in items) cart.put(JSONObject().put("itemId", e.itemId).put("qty", e.qty))
        root.put("cart", cart)
        val historyArr = JSONArray()
        for (h in history) historyArr.put(h)
        root.put("history", historyArr)
        file.writeText(root.toString())
    }

    fun add(itemId: Int, qty: Int) {
        val index = items.indexOfFirst { it.itemId == itemId }
        if (index >= 0) {
            items[index] = items[index].copy(qty = items[index].qty + qty)
        } else {
            items.add(ListEntry(itemId, qty))
        }
        save()
    }

    fun setQty(itemId: Int, qty: Int) {
        val index = items.indexOfFirst { it.itemId == itemId }
        if (index < 0) return
        if (qty <= 0) items.removeAt(index) else items[index] = items[index].copy(qty = qty)
        save()
    }

    fun remove(itemId: Int) {
        items.removeAll { it.itemId == itemId }
        save()
    }

    fun removeIds(ids: Set<Int>) {
        if (ids.isEmpty()) return
        items.removeAll { it.itemId in ids }
        save()
    }

    fun addHistory(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        history.remove(q)
        history.add(0, q)
        while (history.size > 20) history.removeAt(history.lastIndex)
        save()
    }

    fun removeHistory(query: String) {
        history.remove(query)
        save()
    }
}
