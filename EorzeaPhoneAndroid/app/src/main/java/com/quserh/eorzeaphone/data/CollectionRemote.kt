package com.quserh.eorzeaphone.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CollectionSource(val type: String, val text: String)
data class CollectionRemoteDetail(
    val description: String,
    val patch: String,
    val tradeable: Boolean?,
    val rarity: String,
    val sources: List<CollectionSource>,
)

object CollectionRemote {
    suspend fun fetch(category: Int, id: Long): CollectionRemoteDetail? = withContext(Dispatchers.IO) {
        val path = when (category) {
            0 -> "mounts"
            1 -> "minions"
            2 -> "emotes"
            3 -> "orchestrions"
            4 -> "hairstyles"
            5 -> "fashions"
            7 -> "triad/cards"
            else -> return@withContext null
        }
        runCatching {
            val connection = URL("https://ffxivcollect.com/api/$path/$id").openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 7_000
            connection.setRequestProperty("Accept", "application/json")
            connection.inputStream.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                val sourcesJson = json.optJSONArray("sources")
                val sources = buildList {
                    if (sourcesJson != null) for (index in 0 until sourcesJson.length()) {
                        val source = sourcesJson.optJSONObject(index) ?: continue
                        add(CollectionSource(source.optString("type", "获取来源"), source.optString("text")))
                    }
                }
                CollectionRemoteDetail(
                    description = json.optString("description"),
                    patch = json.opt("patch")?.toString().orEmpty(),
                    tradeable = if (json.has("tradeable")) json.optBoolean("tradeable") else null,
                    rarity = json.opt("rarity")?.toString().orEmpty(),
                    sources = sources,
                )
            }.also { connection.disconnect() }
        }.getOrNull()
    }
}
