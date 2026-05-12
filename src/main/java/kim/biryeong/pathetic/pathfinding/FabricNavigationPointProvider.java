package kim.biryeong.pathetic.pathfinding;

import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FabricNavigationPointProvider implements NavigationPointProvider {
	private static final FabricNavigationPoint BLOCKED = new FabricNavigationPoint(false, Cost.ZERO);
	private static final FabricNavigationPoint OPEN = new FabricNavigationPoint(true, Cost.ZERO);
	private static final Cost AVOID_COST = Cost.of(8.0D);

	private final Long2ObjectOpenHashMap<FabricNavigationPoint> cache = new Long2ObjectOpenHashMap<>(256);
	private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

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

		FabricNavigationPoint point = evaluate(context, x, y, z);
		cache.put(key, point);
		return point;
	}

	private FabricNavigationPoint evaluate(FabricEnvironmentContext context, int x, int y, int z) {
		if (!isEmptyCollision(context, x, y, z) || !isEmptyCollision(context, x, y + 1, z)) {
			return BLOCKED;
		}

		BlockState floor = blockState(context, x, y - 1, z);
		VoxelShape floorCollision = floor.getCollisionShape(context.region(), mutablePos);
		if (floorCollision.isEmpty()) {
			return BLOCKED;
		}

		if (isHazard(blockState(context, x, y, z)) || isHazard(floor)) {
			return new FabricNavigationPoint(true, AVOID_COST);
		}
		return OPEN;
	}

	private boolean isEmptyCollision(FabricEnvironmentContext context, int x, int y, int z) {
		BlockState state = blockState(context, x, y, z);
		return state.isAir() || state.getCollisionShape(context.region(), mutablePos).isEmpty();
	}

	private BlockState blockState(FabricEnvironmentContext context, int x, int y, int z) {
		mutablePos.set(x, y, z);
		return context.region().getBlockState(mutablePos);
	}

	private static boolean isHazard(BlockState state) {
		return state.is(Blocks.LAVA)
				|| state.is(Blocks.FIRE)
				|| state.is(Blocks.CAMPFIRE)
				|| state.is(Blocks.SOUL_CAMPFIRE)
				|| state.is(Blocks.CACTUS)
				|| state.is(Blocks.MAGMA_BLOCK)
				|| state.is(Blocks.SWEET_BERRY_BUSH);
	}
}
