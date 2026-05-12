package kim.biryeong.pathetic.pathfinding;

import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FabricNavigationPointProvider implements NavigationPointProvider {
	private static final FabricNavigationPoint BLOCKED = new FabricNavigationPoint(false, Cost.ZERO);
	private static final FabricNavigationPoint OPEN = new FabricNavigationPoint(true, Cost.ZERO);

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
		if (context.horizontalRadiusBlocks() > 0) {
			return evaluateFootprint(context, x, y, z);
		}

		BlockState body = blockState(context, x, y, z);
		BlockState floor = blockState(context, x, y - 1, z);
		if (context.heightBlocks() == 1 && body.isAir() && isCommonSafeFloor(floor)) {
			return OPEN;
		}
		BlockState head = context.heightBlocks() == 2 ? blockState(context, x, y + 1, z) : null;
		if (head != null && body.isAir() && head.isAir() && isCommonSafeFloor(floor)) {
			return OPEN;
		}

		float malus = 0.0F;
		for (int dy = 0; dy < context.heightBlocks(); dy++) {
			BlockState state = dy == 0 ? body : dy == 1 && head != null ? head : blockState(context, x, y + dy, z);
			float bodyMalus = bodyMalus(context, state, x, y + dy, z);
			if (bodyMalus < 0.0F) {
				return BLOCKED;
			}
			malus = Math.max(malus, bodyMalus);
		}

		float floorMalus = floorMalus(context, floor, x, y - 1, z);
		if (floorMalus < 0.0F) {
			return BLOCKED;
		}
		malus = Math.max(malus, floorMalus);
		return pointForMalus(malus);
	}

	private FabricNavigationPoint evaluateFootprint(FabricEnvironmentContext context, int x, int y, int z) {
		float malus = 0.0F;
		int radius = context.horizontalRadiusBlocks();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				int columnX = x + dx;
				int columnZ = z + dz;
				for (int dy = 0; dy < context.heightBlocks(); dy++) {
					int blockY = y + dy;
					float bodyMalus = bodyMalus(context, blockState(context, columnX, blockY, columnZ), columnX, blockY, columnZ);
					if (bodyMalus < 0.0F) {
						return BLOCKED;
					}
					malus = Math.max(malus, bodyMalus);
				}

				int floorY = y - 1;
				float floorMalus = floorMalus(context, blockState(context, columnX, floorY, columnZ), columnX, floorY, columnZ);
				if (floorMalus < 0.0F) {
					return BLOCKED;
				}
				malus = Math.max(malus, floorMalus);
			}
		}
		return pointForMalus(malus);
	}

	private float bodyMalus(FabricEnvironmentContext context, BlockState body, int x, int y, int z) {
		FabricPathCostType bodyType = FabricPathCostType.body(body);
		float bodyMalus = bodyType.malus(context.mob());
		if (bodyMalus < 0.0F || isBlockedByCollision(context, body, bodyType, x, y, z)) {
			return -1.0F;
		}
		return bodyMalus;
	}

	private float floorMalus(FabricEnvironmentContext context, BlockState floor, int x, int y, int z) {
		mutablePos.set(x, y, z);
		VoxelShape floorCollision = floor.getCollisionShape(context.region(), mutablePos);
		if (floorCollision.isEmpty()) {
			return -1.0F;
		}
		FabricPathCostType floorType = FabricPathCostType.floor(floor);
		float floorMalus = floorType.malus(context.mob());
		if (floorMalus < 0.0F) {
			return -1.0F;
		}
		return floorMalus;
	}

	private FabricNavigationPoint pointForMalus(float malus) {
		if (malus <= 0.0F) {
			return OPEN;
		}
		return new FabricNavigationPoint(true, Cost.of(malus));
	}

	private boolean isBlockedByCollision(
			FabricEnvironmentContext context,
			BlockState state,
			FabricPathCostType type,
			int x,
			int y,
			int z
	) {
		mutablePos.set(x, y, z);
		return !state.isAir() && !type.collisionMayBePassable() && !state.getCollisionShape(context.region(), mutablePos).isEmpty();
	}

	private BlockState blockState(FabricEnvironmentContext context, int x, int y, int z) {
		mutablePos.set(x, y, z);
		return context.region().getBlockState(mutablePos);
	}

	private static boolean isCommonSafeFloor(BlockState state) {
		Block block = state.getBlock();
		return block == Blocks.STONE
				|| block == Blocks.GRASS_BLOCK
				|| block == Blocks.DIRT
				|| block == Blocks.COBBLESTONE
				|| block == Blocks.DEEPSLATE
				|| block == Blocks.SAND
				|| block == Blocks.GRAVEL
				|| block == Blocks.NETHERRACK;
	}
}
