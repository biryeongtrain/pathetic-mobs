package kim.biryeong.pathetic.pathfinding;

import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;
import net.minecraft.world.level.pathfinder.PathType;

public final class FabricNavigationPoint implements NavigationPoint {
	private final PathType pathType;
	private final float malus;
	private final Cost cost;

	public FabricNavigationPoint(PathType pathType, float malus) {
		this.pathType = pathType;
		this.malus = malus;
		this.cost = malus <= 0.0F ? Cost.ZERO : Cost.of(malus);
	}

	@Override
	public boolean isTraversable() {
		return pathType != PathType.BLOCKED && malus >= 0.0F;
	}

	public PathType pathType() {
		return pathType;
	}

	public Cost cost() {
		return cost;
	}
}
