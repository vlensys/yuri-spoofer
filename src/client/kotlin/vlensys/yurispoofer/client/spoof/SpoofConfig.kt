package vlensys.yurispoofer.client.spoof

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

object SpoofConfig {
    var masterEnabled: Boolean = false
    var bgImage: Boolean = false

    var spoofName: Boolean = false
    var fakeName: String = ""

    var spoofRank: Boolean = false
    var rankPreset: String = "MVP+"
    var rankText: String = "&c[&6OWNER&c]"

    var spoofLevel: Boolean = false
    var levelText: String = "500"

    var spoofLobby: Boolean = false
    var lobbyText: String = "YURI01"

    var spoofSkills: Boolean = false
    var skillLevels: MutableMap<String, Int> = mutableMapOf()

    var spoofSlayers: Boolean = false
    var slayerLevels: MutableMap<String, Int> = mutableMapOf()

    var spoofCurrency: Boolean = false
    var currencyValues: MutableMap<String, String> = mutableMapOf()

    // cape
    var spoofCape: Boolean = false
    var capeId: String = "migrator"            // cape id
    var customCapes: MutableList<CustomCape> = mutableListOf()  // custom capes

    // cape directory
    fun capesDir(): java.nio.file.Path = FabricLoader.getInstance().configDir.resolve("yuri-spoofer/capes")

    // armor
    var spoofArmor: Boolean = false
    var armorPieces: MutableMap<String, ArmorPiece> = mutableMapOf()

    fun armorPiece(slot: String): ArmorPiece = armorPieces.getOrPut(slot) { ArmorPiece() }

    // skull
    var spoofSkull: Boolean = false
    var skullId: String = ""               // skull id
    var skullCustomTexture: String = ""    // skull texture

    fun skillLevel(skill: Skills.Skill): Int? {
        val raw = skillLevels[skill.name] ?: return null
        return raw.coerceIn(1, skill.cap)
    }

    fun slayerLevel(slayer: Slayers.Slayer): Int? {
        val raw = slayerLevels[slayer.name] ?: return null
        return raw.coerceIn(1, slayer.cap)
    }

    fun currencyValue(currency: Currencies.Currency): String? {
        val raw = currencyValues[currency.name]?.trim() ?: return null
        if (raw.isEmpty()) return null
        return formatCurrency(raw.replace('&', '§'))
    }

    private val PLAIN_NUMBER = Regex("^[0-9,]+(\\.[0-9]+)?$")

    private fun formatCurrency(s: String): String {
        if (!PLAIN_NUMBER.matches(s)) return s
        val dot = s.indexOf('.')
        val intPart = (if (dot >= 0) s.substring(0, dot) else s).replace(",", "")
        if (intPart.isEmpty()) return s
        val frac = if (dot >= 0) s.substring(dot) else ""
        return "%,d".format(java.util.Locale.ROOT, intPart.toBigInteger()) + frac
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val path = FabricLoader.getInstance().configDir.resolve("yuri-spoofer.json")

    private data class Data(
        var masterEnabled: Boolean = false,
        var bgImage: Boolean = false,
        var spoofName: Boolean = false,
        var fakeName: String = "",
        var spoofRank: Boolean = false,
        var rankPreset: String = "MVP+",
        var rankText: String = "&c[&6OWNER&c]",
        var spoofLevel: Boolean = false,
        var levelText: String = "500",
        var spoofLobby: Boolean = false,
        var lobbyText: String = "YURI01",
        var spoofSkills: Boolean = false,
        var skillLevels: MutableMap<String, Int> = mutableMapOf(),
        var spoofSlayers: Boolean = false,
        var slayerLevels: MutableMap<String, Int> = mutableMapOf(),
        var spoofCurrency: Boolean = false,
        var currencyValues: MutableMap<String, String> = mutableMapOf(),
        var spoofCape: Boolean = false,
        var capeId: String = "migrator",
        var customCapes: MutableList<CustomCape> = mutableListOf(),
        var spoofArmor: Boolean = false,
        var armorPieces: MutableMap<String, ArmorPiece> = mutableMapOf(),
        var spoofSkull: Boolean = false,
        var skullId: String = "",
        var skullCustomTexture: String = "",
    )

    fun load() {
        try {
            if (Files.exists(path)) {
                val d = gson.fromJson(Files.readString(path), Data::class.java) ?: return
                masterEnabled = d.masterEnabled
                bgImage = d.bgImage
                spoofName = d.spoofName
                fakeName = d.fakeName
                spoofRank = d.spoofRank
                rankPreset = d.rankPreset
                rankText = d.rankText
                spoofLevel = d.spoofLevel
                levelText = d.levelText
                spoofLobby = d.spoofLobby
                lobbyText = d.lobbyText
                spoofSkills = d.spoofSkills
                skillLevels = d.skillLevels
                spoofSlayers = d.spoofSlayers
                slayerLevels = d.slayerLevels
                spoofCurrency = d.spoofCurrency
                currencyValues = d.currencyValues
                spoofCape = d.spoofCape
                capeId = d.capeId
                customCapes = d.customCapes
                spoofArmor = d.spoofArmor
                armorPieces = d.armorPieces
                spoofSkull = d.spoofSkull
                skullId = d.skullId
                skullCustomTexture = d.skullCustomTexture
            }
        } catch (_: Exception) {
        }
    }

    fun save() {
        try {
            val d = Data(
                masterEnabled, bgImage, spoofName, fakeName, spoofRank, rankPreset, rankText,
                spoofLevel, levelText, spoofLobby, lobbyText, spoofSkills, skillLevels,
                spoofSlayers, slayerLevels, spoofCurrency, currencyValues,
                spoofCape, capeId, customCapes,
                spoofArmor, armorPieces,
                spoofSkull, skullId, skullCustomTexture,
            )
            Files.writeString(path, gson.toJson(d))
        } catch (_: Exception) {
        }
    }
}

// custom cape
data class CustomCape(
    var id: String = "",     // id
    var name: String = "",   // name
    var path: String = "",   // path
)
