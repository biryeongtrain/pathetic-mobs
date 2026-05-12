package kim.biryeong.pathetic.mixin;

import java.util.Set;
import kim.biryeong.pathetic.pathfinding.PathfindingDebugMetrics;
import kim.biryeong.pathetic.pathfinding.PatheticPathfinding;
import kim.biryeong.pathetic.pathfinding.PathfindingBenchmarkControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathFinder.class)
public class PathFinderMixin {
	@Shadow
	@Final
	private NodeEvaluator nodeEvaluator;

	@Inject(method = "findPath(Lnet/minecraft/world/level/PathNavigationRegion;Lnet/minecraft/world/entity/Mob;Ljava/util/Set;FIF)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
	private void pathetic$findPath(
			PathNavigationRegion region,
			Mob mob,
			Set<BlockPos> targets,
			float maxRange,
			int accuracy,
			float searchDepthMultiplier,
			CallbackInfoReturnable<Path> cir
	) {
		PathfindingDebugMetrics.begin(mob, targets);
		if (PathfindingBenchmarkControl.isPatheticDisabled(mob)) {
			PathfindingDebugMetrics.markVanilla("disabled");
			return;
		}

		if (!(nodeEvaluator instanceof WalkNodeEvaluator) || targets.size() != 1) {
			PathfindingDebugMetrics.markVanilla("unsupported");
			return;
		}

		BlockPos target = targets.iterator().next();
		int maxPathLength = Math.max(1, (int) Math.ceil(maxRange));
		Path path = PatheticPathfinding.findGroundPath(region, mob, target, maxPathLength);
		if (path != null) {
			PathfindingDebugMetrics.markPathetic();
			cir.setReturnValue(path);
		} else {
			PathfindingDebugMetrics.markVanilla("fallback");
		}
	}

	@Inject(method = "findPath(Lnet/minecraft/world/level/PathNavigationRegion;Lnet/minecraft/world/entity/Mob;Ljava/util/Set;FIF)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("RETURN"))
	private void pathetic$logPathfinding(
			PathNavigationRegion region,
			Mob mob,
			Set<BlockPos> targets,
			float maxRange,
			int accuracy,
			float searchDepthMultiplier,
			CallbackInfoReturnable<Path> cir
	) {
		PathfindingDebugMetrics.finish(cir.getReturnValue());
	}
}
