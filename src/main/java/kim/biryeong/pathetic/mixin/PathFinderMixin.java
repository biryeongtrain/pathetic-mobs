package kim.biryeong.pathetic.mixin;

import java.util.Set;
import kim.biryeong.pathetic.pathfinding.PatheticPathfinding;
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

	@Inject(method = "findPath", at = @At("HEAD"), cancellable = true)
	private void pathetic$findPath(
			PathNavigationRegion region,
			Mob mob,
			Set<BlockPos> targets,
			float maxRange,
			int accuracy,
			float searchDepthMultiplier,
			CallbackInfoReturnable<Path> cir
	) {
		if (!(nodeEvaluator instanceof WalkNodeEvaluator walkNodeEvaluator) || targets.size() != 1) {
			return;
		}

		BlockPos target = targets.iterator().next();
		Path path = PatheticPathfinding.findGroundPath(region, mob, walkNodeEvaluator, target, accuracy);
		if (path != null) {
			cir.setReturnValue(path);
		}
	}
}
