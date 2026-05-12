package kim.biryeong.pathetic.pathfinding;

import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PathfindingDebugMetrics {
	private static final Logger LOGGER = LoggerFactory.getLogger("pathetic-mobs.pathfinding");
	private static final ThreadLocal<Sample> ACTIVE_SAMPLE = new ThreadLocal<>();

	private static volatile boolean enabled;

	private PathfindingDebugMetrics() {
	}

	public static void enable() {
		enabled = true;
	}

	public static void begin(Mob mob, Set<BlockPos> targets) {
		if (!shouldSample()) {
			return;
		}
		ACTIVE_SAMPLE.set(new Sample(System.nanoTime(), entityTypeName(mob), targets.size(), "unknown"));
	}

	public static void markVanilla(String reason) {
		Sample sample = ACTIVE_SAMPLE.get();
		if (sample != null) {
			sample.engine = "vanilla_" + reason;
		}
	}

	public static void markPathetic() {
		Sample sample = ACTIVE_SAMPLE.get();
		if (sample != null) {
			sample.engine = "pathetic";
		}
	}

	public static void finish(Path path) {
		Sample sample = ACTIVE_SAMPLE.get();
		if (sample == null) {
			return;
		}
		ACTIVE_SAMPLE.remove();

		long elapsedMicros = (System.nanoTime() - sample.startNanos) / 1_000L;
		int nodeCount = path == null ? 0 : path.getNodeCount();
		LOGGER.debug(
				"pathfinding_debug engine={} mob={} elapsed_us={} success={} nodes={} targets={}",
				sample.engine,
				sample.mobType,
				elapsedMicros,
				path != null,
				nodeCount,
				sample.targetCount
		);
	}

	public static boolean isEnabled() {
		return enabled && LOGGER.isDebugEnabled();
	}

	private static boolean shouldSample() {
		return isEnabled();
	}

	private static String entityTypeName(Mob mob) {
		return EntityType.getKey(mob.getType()).toString().toLowerCase(Locale.ROOT);
	}

	private static final class Sample {
		private final long startNanos;
		private final String mobType;
		private final int targetCount;
		private String engine;

		private Sample(long startNanos, String mobType, int targetCount, String engine) {
			this.startNanos = startNanos;
			this.mobType = mobType;
			this.targetCount = targetCount;
			this.engine = engine;
		}
	}
}
