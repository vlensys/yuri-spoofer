package vlensys.yurispoofer.client.spoof

import net.minecraft.network.chat.Component

object SlayerSpoofer {
    private val CATEGORIES = Slayers.ALL.joinToString("|") { it.name }

    private val SLAYER_ID = Regex("($CATEGORIES) Slayer")
    private val BOSS_LVL = Regex("(($CATEGORIES) Slayer:(?:§.|\\s)*LVL(?:§.|\\s)*)(\\d+)")
    private val CURRENT_LVL = Regex("(Current LVL:(?:§.|\\s)*)(\\d+)")
    private val XP_TO_LVL = Regex("(($CATEGORIES) Slayer XP to LVL(?:§.|\\s)*)(\\d+)")
    private val FRACTION = Regex("([0-9][0-9,]*(?:\\.[0-9]+)?)((?:§.)*/(?:§.)*)([0-9][0-9,.]*[kKmMbB]?)")

    fun enabled(): Boolean = SpoofConfig.masterEnabled && SpoofConfig.spoofSlayers

    fun rewriteTooltip(lines: MutableList<Component>) {
        if (!enabled() || lines.isEmpty()) return

        var slayer: Slayers.Slayer? = null
        for (line in lines) {
            val m = SLAYER_ID.find(stripFormatting(line.string)) ?: continue
            slayer = Slayers.byName(m.groupValues[1])
            if (slayer != null) break
        }
        val sl = slayer ?: return
        val target = SpoofConfig.slayerLevel(sl) ?: return
        val maxed = target >= sl.cap

        var seenXpHeader = false
        var rescaled = false

        for (i in lines.indices) {
            val legacy = Spoofer.toLegacy(lines[i])

            val boss = BOSS_LVL.find(legacy)
            if (boss != null) {
                lines[i] = Spoofer.fromLegacy(replaceLastGroup(legacy, boss, target.toString()))
                continue
            }

            val current = CURRENT_LVL.find(legacy)
            if (current != null) {
                lines[i] = Spoofer.fromLegacy(replaceLastGroup(legacy, current, target.toString()))
                continue
            }

            val header = XP_TO_LVL.find(legacy)
            if (header != null) {
                lines[i] = if (maxed) Spoofer.fromLegacy("§7${sl.name} Slayer: §aMAXED!")
                    else Spoofer.fromLegacy(replaceLastGroup(legacy, header, (target + 1).toString()))
                seenXpHeader = true
                continue
            }

            if (seenXpHeader && !rescaled) {
                val fraction = FRACTION.find(legacy)
                if (fraction != null) {
                    if (maxed) {
                        lines[i] = Component.empty()
                    } else {
                        val realCur = parseNum(fraction.groupValues[1])
                        val realNeed = parseNum(fraction.groupValues[3])
                        val frac = if (realNeed > 0) (realCur / realNeed).coerceIn(0.0, 1.0) else 0.0
                        val need = sl.totalFor(target + 1)
                        val replaced = legacy.substring(0, fraction.range.first) +
                            commaInt(frac * need) + fraction.groupValues[2] + shortNum(need) +
                            legacy.substring(fraction.range.last + 1)
                        lines[i] = Spoofer.fromLegacy(replaced)
                    }
                    rescaled = true
                    continue
                }
            }
        }
    }

    private fun replaceLastGroup(legacy: String, m: MatchResult, newVal: String): String {
        val range = m.groups[m.groupValues.size - 1]!!.range
        return legacy.substring(0, range.first) + newVal + legacy.substring(range.last + 1)
    }

    private fun stripFormatting(s: String): String = s.replace(Regex("§."), "")

    private fun parseNum(s: String): Double {
        val t = s.replace(",", "").trim()
        if (t.isEmpty()) return 0.0
        val mult = when (t.last().lowercaseChar()) {
            'k' -> 1_000.0
            'm' -> 1_000_000.0
            'b' -> 1_000_000_000.0
            else -> 1.0
        }
        val digits = if (mult != 1.0) t.dropLast(1) else t
        return (digits.toDoubleOrNull() ?: 0.0) * mult
    }

    private fun commaInt(v: Double): String = "%,d".format(Math.round(v))

    private fun shortNum(v: Long): String = when {
        v >= 1_000_000 -> trimDec(v / 1_000_000.0) + "M"
        v >= 1_000 -> trimDec(v / 1_000.0) + "k"
        else -> v.toString()
    }

    private fun trimDec(d: Double): String =
        if (d == Math.floor(d)) d.toLong().toString() else "%.1f".format(d)
}
