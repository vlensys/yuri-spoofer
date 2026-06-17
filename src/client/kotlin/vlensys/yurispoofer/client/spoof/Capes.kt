package vlensys.yurispoofer.client.spoof

// cape presets
object Capes {
    const val NONE = "none"
    const val ADD = "+add"               // import tile
    const val CUSTOM_PREFIX = "custom:"  // custom prefix

    fun isCustom(id: String): Boolean = id.startsWith(CUSTOM_PREFIX)

    data class Cape(val id: String, val display: String, val hash: String)

    val PRESETS: List<Cape> = listOf(
        Cape("migrator", "Migrator", "2340c0e03dd24a11b15a8b33c2a7e9e32abb2051b2481d0ba7defd635ca7a933"),
        Cape("minecon_2011", "MineCon 2011", "953cac8b779fe41383e675ee2b86071a71658f2180f56fbce8aa315ea70e2ed6"),
        Cape("minecon_2012", "MineCon 2012", "a2e8d97ec79100e90a75d369d1b3ba81273c4f82bc1b737e934eed4a854be1b6"),
        Cape("minecon_2013", "MineCon 2013", "153b1a0dfcbae953cdeb6f2c2bf6bf79943239b1372780da44bcbb29273131da"),
        Cape("minecon_2015", "MineCon 2015", "b0cc08840700447322d953a02b965f1d65a13a603bf64b17c803c21446fe1635"),
        Cape("minecon_2016", "MineCon 2016", "e7dfea16dc83c97df01a12fabbd1216359c0cd0ea42f9999b6e97c584963e980"),
        Cape("vanilla", "Vanilla", "f9a76537647989f9a0b6d001e320dac591c359e9e61a31f4ce11c88f207f0ad4"),
        Cape("cherry_blossom", "Cherry Blossom", "afd553b39358a24edfe3b8a9a939fa5fa4faa4d9a9c3d6af8eafb377fa05c2bb"),
        Cape("mojang", "Mojang", "5786fe99be377dfb6858859f926c4dbc995751e91cee373468c5fbf4865e7151"),
        Cape("mojang_studios", "Mojang Studios", "9e507afc56359978a3eb3e32367042b853cddd0995d17d0da995662913fb00f7"),
        Cape("mojang_office", "Mojang Office", "5c29410057e32abec02d870ecb52ec25fb45ea81e785a7854ae8429d7236ca26"),
        Cape("pan", "Pan", "28de4a81688ad18b49e735a273e086c18f1e3966956123ccb574034c06f5d336"),
        Cape("common", "Common", "5ec930cdd2629c8771655c60eebeb867b4b6559b0e6d3bc71c40c96347fa03f0"),
        Cape("copper", "Copper", "5e6f3193e74cd16cdd6637d9bae5484e3a37ff2a14c2d157c659a07810b1bdca"),
        Cape("mc_experience", "MC Experience", "7658c5025c77cfac7574aab3af94a46a8886e3b7722a895255fbf22ab8652434"),
        Cape("anniversary_15", "15th Anniversary", "cd9d82ab17fd92022dbd4a86cde4c382a7540e117fae7b9a2853658505a80625"),
        Cape("purple_heart", "Purple Heart", "cb40a92e32b57fd732a00fc325e7afb00a7ca74936ad50d8e860152e482cfbde"),
        Cape("founders", "Founder's", "99aba02ef05ec6aa4d42db8ee43796d6cd50e4b2954ab29f0caeb85f96bf52a1"),
        Cape("mcc_15th", "MCC 15th Year", "56c35628fe1c4d59dd52561a3d03bfa4e1a76d397c8b9c476c2f77cb6aebb1df"),
        Cape("yearn", "Yearn", "308b32a9e303155a0b4262f9e5483ad4a22e3412e84fe8385a0bdd73dc41fa89"),
        Cape("cobalt", "Cobalt", "ca29f5dd9e94fb1748203b92e36b66fda80750c87ebc18d6eafdb0e28cc1d05f"),
        Cape("home", "Home", "4b5de481e1b41f51c678155ff965c7e9526c7c641aaaf97a501916c7cb101c4b"),
        Cape("menace", "Menace", "b1fc59bc3de3cda3ed4d99c3d65b721c57cec900ece350cbda6f451099e078e7"),
        Cape("followers", "Follower's", "569b7f2a1d00d26f30efe3f9ab9ac817b1e6d35f4f3cfb0324ef2d328223d350"),
    )

    fun isPreset(id: String): Boolean = PRESETS.any { it.id == id }

    fun byId(id: String): Cape? = PRESETS.firstOrNull { it.id == id }

    fun displayFor(id: String): String = when {
        id == NONE -> "None"
        id == ADD -> "Add"
        isCustom(id) -> SpoofConfig.customCapes.firstOrNull { it.id == id }?.name ?: "Custom"
        else -> byId(id)?.display ?: id
    }
}
