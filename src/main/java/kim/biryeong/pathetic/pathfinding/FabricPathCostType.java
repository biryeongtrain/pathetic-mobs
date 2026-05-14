package kim.biryeong.pathetic.pathfinding;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.PathType;

enum FabricPathCostType {
	OPEN(PathType.OPEN, false),
	WALKABLE(PathType.WALKABLE, false),
	FENCE(PathType.FENCE, true),
	LAVA(PathType.LAVA, true),
	WATER(PathType.WATER, false),
	RAIL(PathType.RAIL, false),
	FIRE(PathType.DAMAGE_FIRE, true),
	DAMAGING(PathType.DAMAGE_OTHER, true),
	STICKY_HONEY(PathType.STICKY_HONEY, true),
	DAMAGE_CAUTIOUS(PathType.DAMAGE_CAUTIOUS, true),
	POWDER_SNOW(PathType.POWDER_SNOW, true),
	DOOR_OPEN(PathType.DOOR_OPEN, false),
	DOOR_WOOD_CLOSED(PathType.DOOR_WOOD_CLOSED, true),
	DOOR_IRON_CLOSED(PathType.DOOR_IRON_CLOSED, true),
	TRAPDOOR(PathType.TRAPDOOR, false),
	LEAVES(PathType.LEAVES, true);

	private final PathType vanillaType;
	private final boolean collisionMayBePassable;

	FabricPathCostType(PathType vanillaType, boolean collisionMayBePassable) {
		this.vanillaType = vanillaType;
		this.collisionMayBePassable = collisionMayBePassable;
	}

	float malus(Mob mob) {
		return mob.getPathfindingMalus(vanillaType);
	}

	boolean collisionMayBePassable() {
		return collisionMayBePassable;
	}

	static FabricPathCostType body(BlockState state) {
		if (state.isAir()) {
			return OPEN;
		}
		if (state.getFluidState().is(FluidTags.LAVA)) {
			return LAVA;
		}
		if (state.getFluidState().is(FluidTags.WATER)) {
			return WATER;
		}
		if (state.is(Blocks.POWDER_SNOW)) {
			return POWDER_SNOW;
		}
		if (isFireDamage(state)) {
			return FIRE;
		}
		if (state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH)) {
			return DAMAGING;
		}
		if (state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POINTED_DRIPSTONE)) {
			return DAMAGE_CAUTIOUS;
		}
		if (state.is(Blocks.HONEY_BLOCK)) {
			return STICKY_HONEY;
		}
		if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS) || state.is(BlockTags.FENCE_GATES)) {
			return FENCE;
		}
		if (state.is(BlockTags.LEAVES)) {
			return LEAVES;
		}
		if (state.is(BlockTags.RAILS)) {
			return RAIL;
		}
		if (state.is(BlockTags.TRAPDOORS)) {
			return TRAPDOOR;
		}
		if (state.getBlock() instanceof DoorBlock doorBlock) {
			if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) {
				return DOOR_OPEN;
			}
			return doorBlock.type().canOpenByHand() ? DOOR_WOOD_CLOSED : DOOR_IRON_CLOSED;
		}
		return WALKABLE;
	}

	static FabricPathCostType floor(BlockState state) {
		if (state.getFluidState().is(FluidTags.LAVA)) {
			return LAVA;
		}
		if (state.getFluidState().is(FluidTags.WATER)) {
			return WATER;
		}
		if (isFireDamage(state)) {
			return FIRE;
		}
		if (state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH)) {
			return DAMAGING;
		}
		if (state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POINTED_DRIPSTONE)) {
			return DAMAGE_CAUTIOUS;
		}
		if (state.is(Blocks.HONEY_BLOCK)) {
			return STICKY_HONEY;
		}
		return WALKABLE;
	}

	private static boolean isFireDamage(BlockState state) {
		if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
			return true;
		}
		return (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
				&& state.hasProperty(BlockStateProperties.LIT)
				&& state.getValue(BlockStateProperties.LIT);
	}
}
