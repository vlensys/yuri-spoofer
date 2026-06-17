package vlensys.yurispoofer.client.spoof

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// skull database
object Skulls {
    const val CUSTOM = "custom"

    data class Head(
        @SerializedName("n") val id: String,
        @SerializedName("d") val display: String,
        @SerializedName("t") val tex: String,
    )

    @Volatile private var loaded = false
    private val all = ArrayList<Head>()

    @JvmStatic
    fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            try {
                val stream = Skulls::class.java.getResourceAsStream("/assets/yuri-spoofer/skulls.json")
                if (stream != null) {
                    val text = stream.use { it.readBytes().toString(Charsets.UTF_8) }
                    val arr = Gson().fromJson(text, Array<Head>::class.java)
                    if (arr != null) all.addAll(arr)
                }
            } catch (_: Exception) {
            }
            loaded = true
        }
    }

    fun count(): Int { ensureLoaded(); return all.size }

    fun byId(id: String): Head? {
        ensureLoaded()
        return all.firstOrNull { it.id == id }
    }

    // skull search
    fun search(query: String, limit: Int): List<Head> {
        ensureLoaded()
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all.take(limit)
        return all.asSequence()
            .filter { it.display.lowercase().contains(q) || it.id.lowercase().contains(q) }
            .take(limit)
            .toList()
    }
}
