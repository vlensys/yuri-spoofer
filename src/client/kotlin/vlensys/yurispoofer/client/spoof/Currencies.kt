package vlensys.yurispoofer.client.spoof

object Currencies {
    data class Currency(val name: String, val labels: List<String>) {
        val regex: Regex = run {
            val alt = labels.joinToString("|") { Regex.escape(it) }
            Regex("((?:$alt)(?:§.|\\s)*:(?:§.|\\s)*)([0-9][0-9,]*(?:\\.[0-9]+)?)")
        }
    }

    val ALL: List<Currency> = listOf(
        Currency("Purse", listOf("Purse", "Piggy")),
        Currency("Bank", listOf("Bank Balance", "Balance")),
        Currency("Bits", listOf("Bits")),
        Currency("Copper", listOf("Copper")),
        Currency("Gems", listOf("Gems")),
        Currency("Motes", listOf("Motes")),
    )

    private val BY_NAME = ALL.associateBy { it.name.lowercase() }

    fun byName(name: String): Currency? = BY_NAME[name.lowercase()]
}
