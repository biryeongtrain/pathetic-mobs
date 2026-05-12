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
		Path directPath = findDirectGroundPath(provider, context, mob.blockPosition(), target);
		if (directPath != null) {
			return directPath;
		}

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

	private static Path findDirectGroundPath(
			FabricNavigationPointProvider provider,
			FabricEnvironmentContext context,
			BlockPos start,
			BlockPos target
	) {
		if (start.getY() != target.getY()) {
			return null;
		}

		int deltaX = target.getX() - start.getX();
		int deltaZ = target.getZ() - start.getZ();
		int steps = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
		if (steps == 0) {
			return null;
		}

		List<Node> nodes = new ArrayList<>(steps);
		Node previous = null;
		for (int i = 1; i <= steps; i++) {
			int x = start.getX() + Math.round((float) deltaX * (float) i / (float) steps);
			int z = start.getZ() + Math.round((float) deltaZ * (float) i / (float) steps);
			FabricNavigationPoint point = provider.pointAt(PathPosition.of(x, start.getY(), z), context);
			if (!point.isTraversable() || point.cost().value() > 0.0D) {
				return null;
			}

			Node node = new Node(x, start.getY(), z);
			if (previous == null || previous.x != node.x || previous.y != node.y || previous.z != node.z) {
				nodes.add(node);
				previous = node;
			}
		}

		if (nodes.isEmpty()) {
			return null;
		}
		return new Path(nodes, target, true);
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
