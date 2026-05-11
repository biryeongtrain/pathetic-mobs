package kim.biryeong.pathetic.pathfinding;

import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathType;

public final class FabricNavigationPointProvider implements NavigationPointProvider {
	private final Map<Long, FabricNavigationPoint> cache = new HashMap<>(256);

	@Override
	public NavigationPoint getNavigationPoint(PathPosition pathPosition, EnvironmentContext environmentContext) {
		return pointAt(pathPosition, (FabricEnvironmentContext) environmentContext);
	}

	public FabricNavigationPoint pointAt(PathPosition pathPosition, FabricEnvironmentContext context) {
		int x = pathPosition.getFlooredX();
		int y = pathPosition.getFlooredY();
		int z = pathPosition.getFlooredZ();
		long key = BlockPos.asLong(x, y, z);
		FabricNavigationPoint cached = cache.get(key);
		if (cached != null) {
			return cached;
		}

		PathType pathType = context.evaluator().getPathTypeOfMob(context.pathfindingContext(), x, y, z, context.mob());
		float malus = context.mob().getPathfindingMalus(pathType);
		FabricNavigationPoint point = new FabricNavigationPoint(pathType, malus);
		cache.put(key, point);
		return point;
	}
}
