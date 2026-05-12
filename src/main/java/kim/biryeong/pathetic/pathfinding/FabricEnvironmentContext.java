package kim.biryeong.pathetic.pathfinding;

import de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext;
import net.minecraft.world.level.PathNavigationRegion;

public record FabricEnvironmentContext(PathNavigationRegion region) implements EnvironmentContext {
}
