package vlensys.yurispoofer.client.spoof

object Slayers {
    data class Slayer(val name: String, val boss: String, val cap: Int, private val cumulative: LongArray) {
        fun totalFor(level: Int): Long = when {
            level <= 0 -> 0L
            level >= cumulative.size -> cumulative.last()
            else -> cumulative[level - 1]
        }

        fun stepFrom(level: Int): Long = (totalFor(level + 1) - totalFor(level)).coerceAtLeast(0L)
    }

    private val ZOMBIE = longArrayOf(5, 15, 200, 1000, 5000, 20000, 100000, 400000, 1000000)
    private val SPIDER = longArrayOf(5, 25, 200, 1000, 5000, 20000, 100000, 400000, 1000000)
    private val WOLF = longArrayOf(10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000)

    val ALL: List<Slayer> = listOf(
        Slayer("Zombie", "Revenant", 9, ZOMBIE),
        Slayer("Spider", "Tarantula", 9, SPIDER),
        Slayer("Wolf", "Sven", 9, WOLF),
        Slayer("Enderman", "Voidgloom", 9, WOLF),
        Slayer("Blaze", "Inferno", 9, WOLF),
    )

    private val BY_KEY: Map<String, Slayer> = buildMap {
        for (s in ALL) {
            put(s.name.lowercase(), s)
            put(s.boss.lowercase(), s)
        }
    }

    fun byName(name: String): Slayer? = BY_KEY[name.lowercase()]
}
