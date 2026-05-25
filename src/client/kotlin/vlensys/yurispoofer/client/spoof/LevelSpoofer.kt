package vlensys.yurispoofer.client.spoof

import net.minecraft.network.chat.Component

object LevelSpoofer {
    private val LEVEL_LINE = Regex("(Your SkyBlock Level:(?:§.|\\s)*\\[(?:§.)*)(\\d+)((?:§.)*])")
    private val PROGRESS_LINE = Regex("(Progress to Level(?:§.|\\s)*)(\\d+)")

    private fun target(): Int? {
        if (!SpoofConfig.masterEnabled || !SpoofConfig.spoofLevel) return null
        val plain = SpoofConfig.levelText.replace('&', '§').replace(Regex("§."), "")
        return Regex("\\d+").find(plain)?.value?.toIntOrNull()
    }

    fun rewriteTooltip(lines: MutableList<Component>) {
        val target = target() ?: return
        if (lines.none { stripFormatting(it.string).contains("SkyBlock Level") }) return

        for (i in lines.indices) {
            val legacy = Spoofer.toLegacy(lines[i])
            val lvl = LEVEL_LINE.find(legacy)
            if (lvl != null) {
                lines[i] = Spoofer.fromLegacy(replaceGroup(legacy, lvl, 2, target.toString()))
                continue
            }
            val prog = PROGRESS_LINE.find(legacy)
            if (prog != null) {
                lines[i] = Spoofer.fromLegacy(replaceGroup(legacy, prog, 2, (target + 1).toString()))
            }
        }
    }

    private fun replaceGroup(legacy: String, m: MatchResult, group: Int, newVal: String): String {
        val r = m.groups[group]!!.range
        return legacy.substring(0, r.first) + newVal + legacy.substring(r.last + 1)
    }

    private fun stripFormatting(s: String): String = s.replace(Regex("§."), "")
}
