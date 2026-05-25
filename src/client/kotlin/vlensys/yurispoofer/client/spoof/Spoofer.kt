package vlensys.yurispoofer.client.spoof

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import java.util.Optional

object Spoofer {
    private val LEVEL = Regex("(?:§.)*\\[(?:§.|\\d)(?:§.|[^\\]§])*]")

    private val LOBBY_DATE = Regex("(\\d{1,2}/\\d{1,2}/\\d{2,4}\\s+)((?:§.)*)([0-9A-Za-z](?:§.|[0-9A-Za-z])*)")
    private val LOBBY_SENDING = Regex("(Sending (?:you )?to (?:server )?)((?:§.)*)([0-9A-Za-z](?:§.|[0-9A-Za-z])*)")

    @Volatile var detectedLobbyId: String? = null

    @JvmStatic
    fun spoof(component: Component): Component = spoof(component, true)

    @JvmStatic
    fun spoofTab(component: Component): Component {
        val showRank = SpoofConfig.spoofRank && Ranks.showInTab(SpoofConfig.rankPreset)
        return spoof(component, showRank)
    }

    @JvmStatic
    fun spoof(component: Component, showRank: Boolean): Component {
        if (!SpoofConfig.masterEnabled) return component

        val nameSpoof = SpoofConfig.spoofName && SpoofConfig.fakeName.isNotEmpty()
        val identityOn = nameSpoof || SpoofConfig.spoofRank || SpoofConfig.spoofLevel
        val lobbyOn = SpoofConfig.spoofLobby && SpoofConfig.lobbyText.isNotEmpty()
        val skillsOn = SkillSpoofer.enabled()
        val currencyOn = CurrencySpoofer.enabled()
        if (!identityOn && !lobbyOn && !skillsOn && !currencyOn) return component

        val legacy = toLegacy(component)
        var out = legacy

        if (lobbyOn) out = applyLobby(out)
        if (skillsOn) out = SkillSpoofer.spoofText(out)
        if (currencyOn) out = CurrencySpoofer.spoofText(out)

        val real = realName()
        if (identityOn && real != null && component.string.contains(real)) {
            if (SpoofConfig.spoofLevel) {
                out = replaceFirst(out, LEVEL) { formatLevel(SpoofConfig.levelText) }
            }
            if (SpoofConfig.spoofRank) {
                val prefix = Ranks.prefixFor(SpoofConfig.rankPreset, SpoofConfig.rankText)
                val nameColor = Ranks.nameColorFor(SpoofConfig.rankPreset, SpoofConfig.rankText)
                out = applyRank(out, real, prefix, nameColor, showRank)
            }
            if (nameSpoof) {
                out = out.replace(real, amp(SpoofConfig.fakeName) + "§r")
            }
        }

        val realId = detectedLobbyId
        if (lobbyOn && realId != null && out.contains(realId)) {
            out = out.replace(realId, amp(SpoofConfig.lobbyText) + "§r")
        }

        if (out == legacy) return component
        return fromLegacy(out)
    }

    private fun applyLobby(s: String): String {
        val lobbyPlain = stripFormatting(amp(SpoofConfig.lobbyText))
        var out = replaceLobbyMatch(s, LOBBY_DATE, lobbyPlain)
        out = replaceLobbyMatch(out, LOBBY_SENDING, lobbyPlain)
        return out
    }

    private fun replaceLobbyMatch(s: String, r: Regex, lobbyPlain: String): String =
        replaceFirst(s, r) { m ->
            val afterPrefix = m.range.first + m.groupValues[1].length
            val remainderPlain = stripFormatting(s.substring(afterPrefix))
            if (lobbyPlain.isNotEmpty() && remainderPlain.startsWith(lobbyPlain)) {
                m.value
            } else {
                val rawId = stripFormatting(m.groupValues[3])
                if (rawId.isNotEmpty()) detectedLobbyId = rawId
                m.groupValues[1] + coloredLobbyText(m.groupValues[2]) + "§r"
            }
        }

    private fun coloredLobbyText(originalColor: String): String {
        val text = amp(SpoofConfig.lobbyText)
        return if (text.startsWith('§')) text else originalColor + text
    }

    private fun formatLevel(raw: String): String {
        val text = amp(raw).replace("[", "").replace("]", "").trim()
        val color = if (text.startsWith('§')) {
            text.substring(0, 2)
        } else {
            levelColor(text.takeWhile { it.isDigit() }.toIntOrNull() ?: 0)
        }
        val inner = if (text.startsWith('§')) text else "$color$text"
        return "§8[$inner§8]§r"
    }

    private fun levelColor(level: Int): String = when {
        level < 40  -> "§7"
        level < 80  -> "§f"
        level < 120 -> "§e"
        level < 160 -> "§a"
        level < 200 -> "§2"
        level < 240 -> "§b"
        level < 280 -> "§3"
        level < 320 -> "§9"
        level < 360 -> "§d"
        level < 400 -> "§5"
        level < 440 -> "§6"
        level < 480 -> "§c"
        else        -> "§4"
    }

    private fun stripFormatting(s: String): String = s.replace(Regex("§."), "")

    fun realName(): String? {
        val mc = Minecraft.getInstance()
        return mc.player?.gameProfile?.name ?: mc.user.name
    }

    private fun amp(s: String): String = s.replace('&', '§')

    private inline fun replaceFirst(s: String, r: Regex, replacement: (MatchResult) -> String): String {
        val m = r.find(s) ?: return s
        return s.substring(0, m.range.first) + replacement(m) + s.substring(m.range.last + 1)
    }

    private fun applyRank(s: String, real: String, prefix: String, nameColor: String, showPrefix: Boolean): String {
        val newPrefix = if (showPrefix && prefix.isNotEmpty()) "$prefix " else ""
        val anchor = Regex("(?:§.)*(?:\\[(?:§.|[A-Za-z+])+]\\s*)?(?:§.)*" + Regex.escape(real))
        val m = anchor.find(s) ?: return s
        return s.substring(0, m.range.first) + newPrefix + nameColor + real + s.substring(m.range.last + 1)
    }

    internal fun toLegacy(component: Component): String {
        val sb = StringBuilder()
        component.visit(FormattedText.StyledContentConsumer<Unit> { style, str ->
            sb.append(legacyPrefix(style)).append(str)
            Optional.empty()
        }, Style.EMPTY)
        return sb.toString()
    }

    private fun legacyPrefix(style: Style): String {
        val sb = StringBuilder("§r")
        style.color?.let { tc ->
            val cf = ChatFormatting.values().firstOrNull { it.isColor && it.color == tc.value }
            if (cf != null) sb.append('§').append(cf.char)
        }
        if (style.isBold) sb.append("§l")
        if (style.isItalic) sb.append("§o")
        if (style.isUnderlined) sb.append("§n")
        if (style.isStrikethrough) sb.append("§m")
        if (style.isObfuscated) sb.append("§k")
        return sb.toString()
    }

    internal fun fromLegacy(s: String): Component {
        val root: MutableComponent = Component.empty()
        var style = Style.EMPTY
        val buf = StringBuilder()

        fun flush() {
            if (buf.isNotEmpty()) {
                root.append(Component.literal(buf.toString()).setStyle(style))
                buf.clear()
            }
        }

        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '§' && i + 1 < s.length) {
                flush()
                style = applyCode(style, s[i + 1].lowercaseChar())
                i += 2
            } else {
                buf.append(c)
                i++
            }
        }
        flush()
        return root
    }

    private fun applyCode(style: Style, code: Char): Style {
        val cf = ChatFormatting.getByCode(code) ?: return style
        return when {
            cf == ChatFormatting.RESET -> Style.EMPTY
            cf.isColor -> Style.EMPTY.withColor(cf)
            else -> style.applyFormat(cf)
        }
    }
}
