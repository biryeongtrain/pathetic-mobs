package kim.biryeong.pathetic.pathfinding;

import java.util.function.Supplier;

public final class PathfindingBenchmarkControl {
	private static final ThreadLocal<Boolean> PATHETIC_DISABLED = ThreadLocal.withInitial(() -> false);

	private PathfindingBenchmarkControl() {
	}

	public static boolean isPatheticDisabled() {
		return PATHETIC_DISABLED.get();
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
