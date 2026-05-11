package kim.biryeong.pathetic.pathfinding;

import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public record FabricEnvironmentContext(
		PathNavigationRegion region,
		Mob mob,
		WalkNodeEvaluator evaluator,
		PathfindingContext pathfindingContext
) implements EnvironmentContext {
	public FabricEnvironmentContext(PathNavigationRegion region, Mob mob, WalkNodeEvaluator evaluator) {
		this(region, mob, evaluator, new PathfindingContext(region, mob));
	}
}
