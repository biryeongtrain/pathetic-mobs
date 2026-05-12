package kim.biryeong.pathetic.pathfinding;

import de.bsommerfeld.pathetic.api.pathing.INeighborStrategy;
import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.api.wrapper.PathVector;
import de.bsommerfeld.pathetic.engine.factory.AStarPathfinderFactory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public final class PatheticPathfinding {
	private static final AStarPathfinderFactory FACTORY = new AStarPathfinderFactory();
	private static final List<PathVector> HORIZONTAL_CARDINAL_OFFSETS = List.of(
			PathVector.of(1.0D, 0.0D, 0.0D),
			PathVector.of(-1.0D, 0.0D, 0.0D),
			PathVector.of(0.0D, 0.0D, 1.0D),
			PathVector.of(0.0D, 0.0D, -1.0D)
	);
	private static final INeighborStrategy HORIZONTAL_CARDINAL = () -> HORIZONTAL_CARDINAL_OFFSETS;

	private PatheticPathfinding() {
	}

	public static Path findGroundPath(
			PathNavigationRegion region,
			Mob mob,
			BlockPos target,
			int maxPathLength
	) {
		FabricNavigationPointProvider provider = new FabricNavigationPointProvider();
		FabricEnvironmentContext context = new FabricEnvironmentContext(region);
		PathfinderConfiguration configuration = PathfinderConfiguration.builder()
				.provider(provider)
				.async(false)
				.fallback(false)
				.maxIterations(100_000)
				.maxLength(maxPathLength)
				.neighborStrategy(HORIZONTAL_CARDINAL)
				.nodeValidationProcessors(List.of(evaluation -> {
					FabricNavigationPoint point =
							provider.pointAt(evaluation.getCurrentPathPosition(), context);
					return point.isTraversable();
				}))
				.nodeCostProcessors(List.of(evaluation -> {
					FabricNavigationPoint point =
							provider.pointAt(evaluation.getCurrentPathPosition(), context);
					return point.cost();
				}))
				.build();

		Pathfinder pathfinder = FACTORY.createPathfinder(configuration);
		PathPosition start = PathPosition.of(mob.getX(), mob.getY(), mob.getZ());
		PathPosition end = PathPosition.of(target.getX(), target.getY(), target.getZ());
		PathfinderResult result = pathfinder.findPath(start, end, context).resultBlocking();
		if (result == null || !result.successful() || result.getPath() == null || result.getPath().length() <= 1) {
			return null;
		}

		return toMinecraftPath(result.getPath(), target);
	}

	private static Path toMinecraftPath(de.bsommerfeld.pathetic.api.pathing.result.Path patheticPath, BlockPos target) {
		List<Node> nodes = new ArrayList<>(patheticPath.length());
		Node previous = null;
		for (PathPosition position : patheticPath) {
			Node node = new Node(position.getFlooredX(), position.getFlooredY(), position.getFlooredZ());
			if (previous == null || previous.x != node.x || previous.y != node.y || previous.z != node.z) {
				nodes.add(node);
				previous = node;
			}
		}
		if (nodes.size() <= 1) {
			return null;
		}
		return new Path(nodes, target, true);
	}
}
