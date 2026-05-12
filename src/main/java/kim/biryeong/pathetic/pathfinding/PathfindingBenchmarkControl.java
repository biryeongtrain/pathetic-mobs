package kim.biryeong.pathetic.pathfinding;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.world.entity.Mob;

public final class PathfindingBenchmarkControl {
	private static final ThreadLocal<Boolean> PATHETIC_DISABLED = ThreadLocal.withInitial(() -> false);
	private static final Set<UUID> VANILLA_PATHFINDING_MOBS = ConcurrentHashMap.newKeySet();

	private PathfindingBenchmarkControl() {
	}

	public static boolean isPatheticDisabled() {
		return PATHETIC_DISABLED.get();
	}

	public static boolean isPatheticDisabled(Mob mob) {
		return isPatheticDisabled() || VANILLA_PATHFINDING_MOBS.contains(mob.getUUID());
	}

	public static void useVanillaPathfinding(Mob mob) {
		VANILLA_PATHFINDING_MOBS.add(mob.getUUID());
	}

	public static <T> T withPatheticDisabled(boolean disabled, Supplier<T> supplier) {
		boolean previous = PATHETIC_DISABLED.get();
		PATHETIC_DISABLED.set(disabled);
		try {
			return supplier.get();
		} finally {
			PATHETIC_DISABLED.set(previous);
		}
	}
}
