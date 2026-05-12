package kim.biryeong.pathetic.pathfinding;

import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public record FabricEnvironmentContext(
		PathNavigationRegion region,
		Mob mob,
		int horizontalRadiusBlocks,
		int heightBlocks
) implements EnvironmentContext {
	public FabricEnvironmentContext(PathNavigationRegion region, Mob mob) {
		this(region, mob, horizontalRadiusBlocks(mob), heightBlocks(mob));
	}

	private static int horizontalRadiusBlocks(Mob mob) {
		return Math.max(0, (int) Math.ceil((mob.getBbWidth() - 1.0F) * 0.5F));
	}

	private static int heightBlocks(Mob mob) {
		return Math.max(1, (int) Math.ceil(mob.getBbHeight()));
	}
}
