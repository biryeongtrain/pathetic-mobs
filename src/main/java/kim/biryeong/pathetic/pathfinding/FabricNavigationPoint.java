package kim.biryeong.pathetic.pathfinding;

import de.bsommerfeld.pathetic.api.provider.NavigationPoint;
import de.bsommerfeld.pathetic.api.pathing.processing.Cost;

public final class FabricNavigationPoint implements NavigationPoint {
	private final boolean traversable;
	private final Cost cost;

	public FabricNavigationPoint(boolean traversable, Cost cost) {
		this.traversable = traversable;
		this.cost = cost;
	}

	@Override
	public boolean isTraversable() {
		return traversable;
	}

	public Cost cost() {
		return cost;
	}
}
