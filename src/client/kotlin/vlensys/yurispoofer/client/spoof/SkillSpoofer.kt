package vlensys.yurispoofer.client.spoof

import net.minecraft.network.chat.Component

object SkillSpoofer {
    private val SKILL_NAMES = Skills.ALL.joinToString("|") { it.name }

    private val NAME = Regex("^([A-Za-z]+) ([IVXLC]+)$")
    private val DESC = Regex("($SKILL_NAMES) XP")
    private val PROGRESS = Regex("(Progress to Level )([IVXLC0-9]+)(.*?)([0-9]+(?:\\.[0-9]+)?)(%)")
    private val REWARDS = Regex("(Level )([IVXLC0-9]+)( Rewards)")
    private val PERK_TRAILING = Regex("\\s([IVXLC]+)$")
    private val XP_FRACTION = Regex("([0-9][0-9,]*(?:\\.[0-9]+)?)((?:§.)*/(?:§.)*)([0-9][0-9,.]*[kMB]?)")

    private val TAB_SKILL = Regex("($SKILL_NAMES)((?:§.|\\s)+)(\\d{1,2})((?:§.)*\\s*:)")
    private val CHAT_LEVELUP =
        Regex("(SKILL LEVEL UP (?:§.)*($SKILL_NAMES)(?:§.)* )([IVXLC]+)((?:§.)*[➜»>\\-]+(?:§.)*)([IVXLC]+)")

    fun enabled(): Boolean = SpoofConfig.masterEnabled && SpoofConfig.spoofSkills

    fun rewriteTooltip(lines: MutableList<Component>) {
        if (!enabled() || lines.isEmpty()) return

        var nameIdx = -1
        var skill: Skills.Skill? = null
        for (i in lines.indices) {
            val m = NAME.find(stripFormatting(lines[i].string).trim()) ?: continue
            val s = Skills.byName(m.groupValues[1]) ?: continue
            nameIdx = i
            skill = s
            break
        }
        if (skill == null) {
            for (line in lines) {
                val d = DESC.find(stripFormatting(line.string)) ?: continue
                skill = Skills.byName(d.groupValues[1])
                if (skill != null) break
            }
        }
        val sk = skill ?: return
        val target = SpoofConfig.skillLevel(sk) ?: return

        if (nameIdx >= 0) {
            val m = NAME.find(stripFormatting(lines[nameIdx].string).trim())!!
            lines[nameIdx] = relevelName(lines[nameIdx], m.groupValues[2], Skills.toRoman(target))
        }

        val maxed = target >= sk.cap
        val needed = sk.stepFrom(target)
        val nextRoman = Skills.toRoman(target + 1)
        val rewardRoman = if (maxed) Skills.toRoman(sk.cap) else nextRoman
        var pct = 0.0
        var seenProgress = false
        var rescaledXp = false
        var inRewards = false

        for (i in (nameIdx + 1).coerceAtLeast(0) until lines.size) {
            val legacy = Spoofer.toLegacy(lines[i])

            if (inRewards) {
                val trailing = PERK_TRAILING.find(stripFormatting(legacy))
                if (trailing != null) lines[i] = relevelName(lines[i], trailing.groupValues[1], rewardRoman)
                continue
            }

            val progress = PROGRESS.find(legacy)
            if (progress != null) {
                if (maxed) {
                    lines[i] = Spoofer.fromLegacy("§7Max Skill level reached!")
                } else {
                    pct = progress.groupValues[4].toDoubleOrNull() ?: 0.0
                    val replaced = legacy.substring(0, progress.range.first) +
                        progress.groupValues[1] + nextRoman + progress.groupValues[3] +
                        progress.groupValues[4] + progress.groupValues[5] +
                        legacy.substring(progress.range.last + 1)
                    lines[i] = Spoofer.fromLegacy(replaced)
                }
                seenProgress = true
                continue
            }

            val fraction = if (seenProgress && !rescaledXp) XP_FRACTION.find(legacy) else null
            if (fraction != null) {
                if (maxed) {
                    lines[i] = Component.empty()
                } else {
                    val current = needed * pct / 100.0
                    val replaced = legacy.substring(0, fraction.range.first) +
                        commaDecimal(current) + fraction.groupValues[2] + shortNum(needed) +
                        legacy.substring(fraction.range.last + 1)
                    lines[i] = Spoofer.fromLegacy(replaced)
                }
                rescaledXp = true
                continue
            }

            val rewards = REWARDS.find(legacy)
            if (rewards != null) {
                val replaced = legacy.substring(0, rewards.range.first) +
                    rewards.groupValues[1] + rewardRoman + rewards.groupValues[3] +
                    legacy.substring(rewards.range.last + 1)
                lines[i] = Spoofer.fromLegacy(replaced)
                inRewards = true
            }
        }
    }

    fun spoofText(legacy: String): String {
        if (!enabled()) return legacy
        var out = TAB_SKILL.replace(legacy) { m ->
            val target = Skills.byName(m.groupValues[1])?.let { SpoofConfig.skillLevel(it) }
            if (target == null) m.value
            else m.groupValues[1] + m.groupValues[2] + target + m.groupValues[4]
        }
        out = CHAT_LEVELUP.replace(out) { m ->
            val target = Skills.byName(m.groupValues[2])?.let { SpoofConfig.skillLevel(it) }
            if (target == null) m.value
            else m.groupValues[1] + Skills.toRoman((target - 1).coerceAtLeast(1)) +
                m.groupValues[4] + Skills.toRoman(target)
        }
        return out
    }

    private fun relevelName(line: Component, origRoman: String, newRoman: String): Component {
        val legacy = Spoofer.toLegacy(line)
        val idx = legacy.lastIndexOf(origRoman)
        if (idx < 0) return line
        return Spoofer.fromLegacy(legacy.substring(0, idx) + newRoman + legacy.substring(idx + origRoman.length))
    }

    private fun stripFormatting(s: String): String = s.replace(Regex("§."), "")

    private fun commaDecimal(v: Double): String {
        val rounded = Math.round(v * 10.0) / 10.0
        val whole = rounded.toLong()
        val frac = Math.round((rounded - whole) * 10.0).toInt()
        val ws = "%,d".format(whole)
        return if (frac != 0) "$ws.$frac" else ws
    }

    private fun shortNum(v: Long): String = when {
        v >= 1_000_000 -> trimDec(v / 1_000_000.0) + "M"
        v >= 1_000 -> trimDec(v / 1_000.0) + "k"
        else -> v.toString()
    }

    private fun trimDec(d: Double): String =
        if (d == Math.floor(d)) d.toLong().toString() else "%.1f".format(d)
}
