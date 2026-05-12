package kim.biryeong.pathetic;

import kim.biryeong.pathetic.pathfinding.PathfindingDebugMetrics;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatheticImplInitializer implements ModInitializer {
	public static final String MOD_ID = "pathetic-impl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			PatheticDebugCommands.register();
			PathfindingDebugMetrics.enable();
		}
		LOGGER.info("Pathetic Fabric pathfinding integration initialized.");
	}
}
