package vlensys.yurispoofer.client.test

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import vlensys.yurispoofer.client.spoof.SkillSpoofer
import vlensys.yurispoofer.client.spoof.Skills
import vlensys.yurispoofer.client.spoof.SlayerSpoofer
import vlensys.yurispoofer.client.spoof.SpoofConfig
import vlensys.yurispoofer.client.spoof.Spoofer

class SpooferGuiGameTest : FabricClientGameTest {
    override fun runTest(context: ClientGameTestContext) {
        context.waitForScreen(TitleScreen::class.java)

        // engine check
        context.runOnClient<RuntimeException> { mc ->
            val real = mc.user.name
            SpoofConfig.masterEnabled = true
            SpoofConfig.spoofName = true
            SpoofConfig.fakeName = "&dHACKERMAN"
            SpoofConfig.spoofLevel = true
            SpoofConfig.levelText = "&4[500]"
            SpoofConfig.spoofRank = true
            SpoofConfig.rankPreset = "Custom"
            SpoofConfig.rankText = "&c[&6OWNER&c]"

            val input = Component.literal("§b[27] §a[VIP] $real")
            val result = Spoofer.spoof(input).string
            if (result.contains(real)) throw AssertionError("name not spoofed: '$result'")
            if (!result.contains("[500]")) throw AssertionError("level not spoofed: '$result'")
            if (!result.contains("OWNER")) throw AssertionError("rank not spoofed: '$result'")
            if (!result.contains("HACKERMAN")) throw AssertionError("fake name missing: '$result'")

            // rank insert
            SpoofConfig.spoofName = false
            SpoofConfig.spoofLevel = false
            SpoofConfig.rankPreset = "MVP+"
            val inserted = Spoofer.spoof(Component.literal("§7$real")).string
            if (!inserted.contains("[MVP+]")) throw AssertionError("rank not inserted: '$inserted'")

            // rank remove
            SpoofConfig.rankPreset = "None"
            val removed = Spoofer.spoof(Component.literal("§b[MVP+] §f$real")).string
            if (removed.contains("[")) throw AssertionError("rank not removed: '$removed'")

            // lobby id
            SpoofConfig.spoofRank = false
            SpoofConfig.spoofLobby = true
            SpoofConfig.lobbyText = "&8YURITEST"
            val lobby = Spoofer.spoof(Component.literal("§a05/23/26 §8mB3§8DA")).string
            if (!lobby.contains("YURITEST")) throw AssertionError("lobby not spoofed: '$lobby'")
            if (lobby.contains("mB3") || lobby.contains("DA")) throw AssertionError("lobby id leftover: '$lobby'")
            if (!lobby.contains("05/23/26")) throw AssertionError("date lost: '$lobby'")

            // join message
            val sending = Spoofer.spoof(Component.literal("Sending you to server mega77G...")).string
            if (sending.contains("mega77G")) throw AssertionError("server id not spoofed: '$sending'")
            if (!sending.contains("Sending you to")) throw AssertionError("join text lost: '$sending'")

            // tab rank
            SpoofConfig.spoofLobby = false
            SpoofConfig.spoofRank = true
            SpoofConfig.rankPreset = "MVP+"
            val tab = Spoofer.spoof(Component.literal("§7[§e50§7] §7$real"), false).string
            if (tab.contains("[MVP+]")) throw AssertionError("rank bracket leaked into tab: '$tab'")

            // skill math
            val mining = Skills.byName("Mining") ?: throw AssertionError("Mining skill missing")
            if (mining.cap != 60) throw AssertionError("Mining cap wrong: ${mining.cap}")
            if (mining.stepFrom(19) != 200000L) throw AssertionError("19->20 step wrong: ${mining.stepFrom(19)}")
            if (Skills.toRoman(25) != "XXV") throw AssertionError("toRoman(25) wrong: ${Skills.toRoman(25)}")
            if (Skills.parseRoman("XIX") != 19) throw AssertionError("parseRoman(XIX) wrong")

            // skill tooltip
            SpoofConfig.spoofRank = false
            SpoofConfig.spoofName = false
            SpoofConfig.spoofLevel = false
            SpoofConfig.spoofSkills = true
            SpoofConfig.skillLevels = mutableMapOf("Mining" to 25)
            val lines = mutableListOf<Component>(
                Component.literal("§aMining XIX"),
                Component.literal("§7Progress to Level XX: §e76.9%"),
                Component.literal("§e153,840.6§6/§e200k"),
                Component.literal("§7Level XX Rewards:"),
                Component.literal("§6Spelunker XX"),
            )
            SkillSpoofer.rewriteTooltip(lines)
            if (lines[0].string != "Mining XXV") throw AssertionError("name not releveled: '${lines[0].string}'")
            if (!lines[1].string.contains("Progress to Level XXVI")) throw AssertionError("progress level wrong: '${lines[1].string}'")
            // xp rescale
            if (!lines[2].string.contains("615,200/800k")) throw AssertionError("xp not rescaled: '${lines[2].string}'")
            if (!lines[3].string.contains("Level XXVI Rewards")) throw AssertionError("rewards level wrong: '${lines[3].string}'")
            if (lines[4].string != "Spelunker XXVI") throw AssertionError("perk not releveled: '${lines[4].string}'")

            // tab skill
            val tab2 = SkillSpoofer.spoofText("§7Mining 19§7: §a76.9%")
            if (!tab2.contains("Mining 25")) throw AssertionError("tab skill not releveled: '$tab2'")
            SpoofConfig.spoofSkills = false
            SpoofConfig.skillLevels = mutableMapOf()

            // slayer tooltip
            SpoofConfig.spoofSlayers = true
            SpoofConfig.slayerLevels = mutableMapOf("Zombie" to 5)
            // boss item
            val bossLines = mutableListOf<Component>(
                Component.literal("§c☠ Revenant Horror"),
                Component.literal("§7Zombie Slayer: §eLVL 1"),
            )
            SlayerSpoofer.rewriteTooltip(bossLines)
            if (!bossLines[1].string.contains("LVL 5")) throw AssertionError("boss level wrong: '${bossLines[1].string}'")
            // rewards item
            val rewardLines = mutableListOf<Component>(
                Component.literal("§dBoss Leveling Rewards"),
                Component.literal("§7Current LVL: §e1"),
                Component.literal("§7Zombie Slayer XP to LVL 2:"),
                Component.literal("§d5§7/§d15"),
            )
            SlayerSpoofer.rewriteTooltip(rewardLines)
            if (!rewardLines[1].string.contains("Current LVL: 5")) throw AssertionError("current lvl wrong: '${rewardLines[1].string}'")
            if (!rewardLines[2].string.contains("XP to LVL 6")) throw AssertionError("xp header wrong: '${rewardLines[2].string}'")
            if (!rewardLines[3].string.contains("/20k")) throw AssertionError("slayer xp not rescaled: '${rewardLines[3].string}'")
            SpoofConfig.spoofSlayers = false
            SpoofConfig.slayerLevels = mutableMapOf()
        }
    }
}
