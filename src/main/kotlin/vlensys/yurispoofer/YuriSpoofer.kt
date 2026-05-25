package vlensys.yurispoofer

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object YuriSpoofer : ModInitializer {
    private val logger = LoggerFactory.getLogger("yuri-spoofer")

	override fun onInitialize() {
		logger.info("Hello Fabric world!")
	}
}
