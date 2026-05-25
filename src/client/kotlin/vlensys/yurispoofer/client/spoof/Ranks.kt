package vlensys.yurispoofer.client.spoof

object Ranks {
    const val NONE = "None"
    const val CUSTOM = "Custom"

    val ORDER: List<String> = listOf(
        NONE,
        "VIP",
        "VIP+",
        "MVP",
        "MVP+",
        "MVP++",
        "YOUTUBE",
        "HELPER",
        "MOD",
        "GM",
        "ADMIN",
        "OWNER",
        "MOJANG",
        CUSTOM,
    )

    private val PREFIX: Map<String, String> = mapOf(
        NONE to "",
        "VIP" to "§a[VIP]",
        "VIP+" to "§a[VIP§6+§a]",
        "MVP" to "§b[MVP]",
        "MVP+" to "§b[MVP§c+§b]",
        "MVP++" to "§6[MVP§c++§6]",
        "YOUTUBE" to "§c[§fYOUTUBE§c]",
        "HELPER" to "§9[HELPER]",
        "MOD" to "§2[MOD]",
        "GM" to "§2[GM]",
        "ADMIN" to "§c[ADMIN]",
        "OWNER" to "§c[OWNER]",
        "MOJANG" to "§6[MOJANG]",
    )

    fun prefixFor(preset: String, customText: String): String = when (preset) {
        CUSTOM -> customText.replace('&', '§')
        else -> PREFIX[preset] ?: ""
    }

    fun nameColorFor(preset: String, customText: String): String {
        val prefix = prefixFor(preset, customText)
        if (prefix.isEmpty()) return "§7"
        return firstColorCode(prefix) ?: "§f"
    }

    private val TAB_VISIBLE = setOf("YOUTUBE", "HELPER", "MOD", "GM", "ADMIN", "OWNER", "MOJANG", CUSTOM)

    fun showInTab(preset: String): Boolean = preset in TAB_VISIBLE

    private fun firstColorCode(s: String): String? {
        var i = 0
        while (i + 1 < s.length) {
            if (s[i] == '§' && s[i + 1].lowercaseChar() in "0123456789abcdef") {
                return "§" + s[i + 1]
            }
            i++
        }
        return null
    }
}
