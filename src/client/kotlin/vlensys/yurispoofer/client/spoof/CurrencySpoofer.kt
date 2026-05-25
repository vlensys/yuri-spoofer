package vlensys.yurispoofer.client.spoof

import net.minecraft.network.chat.Component

object CurrencySpoofer {
    fun enabled(): Boolean = SpoofConfig.masterEnabled && SpoofConfig.spoofCurrency

    fun spoofText(legacy: String): String {
        if (!enabled()) return legacy
        var out = legacy
        for (c in Currencies.ALL) {
            val value = SpoofConfig.currencyValue(c) ?: continue
            out = c.regex.replace(out) { m -> m.groupValues[1] + value }
        }
        return out
    }

    fun rewriteTooltip(lines: MutableList<Component>) {
        if (!enabled() || lines.isEmpty()) return
        for (i in lines.indices) {
            val legacy = Spoofer.toLegacy(lines[i])
            val out = spoofText(legacy)
            if (out != legacy) lines[i] = Spoofer.fromLegacy(out)
        }
    }
}
