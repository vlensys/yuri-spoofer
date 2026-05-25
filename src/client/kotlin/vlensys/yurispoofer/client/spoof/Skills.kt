package vlensys.yurispoofer.client.spoof

object Skills {
    data class Skill(val name: String, val cap: Int, private val steps: LongArray) {
        fun stepFrom(level: Int): Long = if (level in 0 until steps.size) steps[level] else 0L
    }

    private val GENERAL = longArrayOf(
        50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500,
        5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000,
        300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000,
        1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000,
        2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000,
        4300000, 4600000, 4900000, 5200000, 5500000, 5800000, 6100000, 6400000, 6700000, 7000000,
    )

    private val RUNECRAFTING = longArrayOf(
        50, 100, 125, 160, 200, 250, 315, 400, 500, 625,
        785, 1000, 1250, 1600, 2000, 2465, 3125, 4000, 5000, 6200,
        7800, 9800, 12200, 15300, 19050,
    )

    val ALL: List<Skill> = listOf(
        Skill("Farming", 60, GENERAL),
        Skill("Mining", 60, GENERAL),
        Skill("Combat", 60, GENERAL),
        Skill("Foraging", 50, GENERAL),
        Skill("Fishing", 50, GENERAL),
        Skill("Enchanting", 60, GENERAL),
        Skill("Alchemy", 50, GENERAL),
        Skill("Taming", 50, GENERAL),
        Skill("Carpentry", 50, GENERAL),
        Skill("Runecrafting", 25, RUNECRAFTING),
    )

    private val BY_NAME = ALL.associateBy { it.name.lowercase() }

    fun byName(name: String): Skill? = BY_NAME[name.lowercase()]

    fun toRoman(n: Int): String {
        if (n <= 0) return n.toString()
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        val sb = StringBuilder()
        var x = n
        for (i in values.indices) {
            while (x >= values[i]) {
                sb.append(symbols[i])
                x -= values[i]
            }
        }
        return sb.toString()
    }

    fun parseRoman(s: String): Int? {
        val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var total = 0
        var prev = 0
        for (c in s.uppercase().reversed()) {
            val v = map[c] ?: return null
            if (v < prev) total -= v else { total += v; prev = v }
        }
        return if (total > 0) total else null
    }
}
