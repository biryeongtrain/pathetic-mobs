package kim.biryeong.pathetic;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import kim.biryeong.pathetic.pathfinding.FabricEnvironmentContext;
import kim.biryeong.pathetic.pathfinding.FabricNavigationPoint;
import kim.biryeong.pathetic.pathfinding.FabricNavigationPointProvider;
import kim.biryeong.pathetic.pathfinding.PathfindingBenchmarkControl;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathfindingGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PathfindingGameTest.class);
	private static final int WARMUP_ITERATIONS = 5;
	private static final int SAMPLE_ITERATIONS = 24;
	private static final List<String> SCENARIO_NAMES = List.of(
			"flat_short",
			"flat_wide",
			"wall_detour",
			"hazard_avoid",
			"narrow_corridor",
			"enclosed_goal_back_entrance",
			"two_gate_hazard_detour",
			"courtyard_backtrack"
	);
	private static final Map<String, BenchmarkResult> COMPLETED_RESULTS = new LinkedHashMap<>();

	@GameTest
	public void flatShortPathfindingBenchmark(GameTestHelper helper) {
		runScenarioBenchmark(helper, flatShort(helper));
	}

	@GameTest
	public void flatWidePathfindingBenchmark(GameTestHelper helper) {
		runScenarioBenchmark(helper, flatWide(helper));
	}

	@GameTest
	public void wallDetourPathfindingBenchmark(GameTestHelper helper) {
		runScenarioBenchmark(helper, wallDetour(helper));
	}

	@GameTest
	public void hazardAvoidPathfindingBenchmark(GameTestHelper helper) {
		runScenarioBenchmark(helper, hazardAvoid(helper));
	}

	@GameTest
	public void narrowCorridorPathfindingBenchmark(GameTestHelper helper) {
		runScenarioBenchmark(helper, narrowCorridor(helper));
	}

	@GameTest
	public void enclosedGoalBackEntrancePathfindingBenchmark(GameTestHelper helper) {
		runScenarioBenchmark(helper, enclosedGoalBackEntrance(helper));
	}

	@GameTest
	public void twoGateHazardDetourPathfindingBenchmark(GameTestHelper helper) {
		runScenarioBenchmark(helper, twoGateHazardDetour(helper));
	}

	@GameTest
	public void courtyardBacktrackPathfindingBenchmark(GameTestHelper helper) {
		runScenarioBenchmark(helper, courtyardBacktrack(helper));
	}

	@GameTest
	public void mobSpecificHazardPenaltyClassification(GameTestHelper helper) {
		helper.runAfterDelay(2, () -> {
			fillPlatform(helper, 0, 0, 3, 3);
			helper.setBlock(new BlockPos(1, 0, 1), Blocks.MAGMA_BLOCK);

			Mob zombie = helper.spawn(EntityType.ZOMBIE, 0, 1, 0, EntitySpawnReason.TRIGGERED);
			Mob warden = helper.spawn(EntityType.WARDEN, 3, 1, 3, EntitySpawnReason.TRIGGERED);
			helper.assertTrue(
					zombie.getPathfindingMalus(PathType.FIRE) > 0.0F,
					"Expected zombie to avoid direct fire damage path types."
			);
			helper.assertTrue(
					warden.getPathfindingMalus(PathType.FIRE) == 0.0F,
					"Expected warden to ignore direct fire damage path types."
			);

			PathNavigationRegion region = new PathNavigationRegion(
					helper.getLevel(),
					helper.absolutePos(new BlockPos(0, 0, 0)),
					helper.absolutePos(new BlockPos(3, 4, 3))
			);
			BlockPos sample = helper.absolutePos(new BlockPos(1, 1, 1));
			PathPosition samplePosition = PathPosition.of(sample.getX(), sample.getY(), sample.getZ());

			FabricNavigationPoint zombiePoint = new FabricNavigationPointProvider()
					.pointAt(samplePosition, new FabricEnvironmentContext(region, zombie));
			FabricNavigationPoint wardenPoint = new FabricNavigationPointProvider()
					.pointAt(samplePosition, new FabricEnvironmentContext(region, warden));

			helper.assertTrue(zombiePoint.isTraversable(), "Expected zombie magma floor to stay traversable with a penalty.");
			helper.assertTrue(zombiePoint.cost().value() > 0.0D, "Expected zombie magma floor to carry a penalty.");
			helper.assertTrue(wardenPoint.isTraversable(), "Expected warden magma floor to stay traversable.");
			helper.assertTrue(wardenPoint.cost().value() == 0.0D, "Expected warden magma floor penalty to be ignored.");
			helper.succeed();
		});
	}

	@GameTest
	public void babyMobDoesNotRequireHeadClearance(GameTestHelper helper) {
		helper.runAfterDelay(2, () -> {
			fillPlatform(helper, 0, 0, 3, 3);
			helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);

			Mob baby = helper.spawn(EntityType.ZOMBIE, 0, 1, 0, EntitySpawnReason.TRIGGERED);
			baby.setBaby(true);
			Mob adult = helper.spawn(EntityType.ZOMBIE, 3, 1, 3, EntitySpawnReason.TRIGGERED);

			PathNavigationRegion region = region(helper, 0, 0, 0, 3, 4, 3);
			PathPosition samplePosition = pathPosition(helper, 1, 1, 1);
			FabricNavigationPoint babyPoint = new FabricNavigationPointProvider()
					.pointAt(samplePosition, new FabricEnvironmentContext(region, baby));
			FabricNavigationPoint adultPoint = new FabricNavigationPointProvider()
					.pointAt(samplePosition, new FabricEnvironmentContext(region, adult));

			helper.assertTrue(baby.getBbHeight() < 1.0F, "Expected baby zombie to be shorter than one block.");
			helper.assertTrue(adult.getBbHeight() > 1.0F, "Expected adult zombie to require head clearance.");
			helper.assertTrue(babyPoint.isTraversable(), "Expected baby zombie to fit under one-block overhead clearance.");
			helper.assertTrue(!adultPoint.isTraversable(), "Expected adult zombie to be blocked by head-level stone.");
			helper.succeed();
		});
	}

	@GameTest
	public void largeMobRequiresFullFootprintClearance(GameTestHelper helper) {
		helper.runAfterDelay(2, () -> {
			fillPlatform(helper, 0, 0, 5, 5);
			helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);

			Mob golem = helper.spawn(EntityType.IRON_GOLEM, 0, 1, 0, EntitySpawnReason.TRIGGERED);
			Mob zombie = helper.spawn(EntityType.ZOMBIE, 5, 1, 5, EntitySpawnReason.TRIGGERED);

			PathNavigationRegion region = region(helper, 0, 0, 0, 5, 5, 5);
			PathPosition samplePosition = pathPosition(helper, 2, 1, 2);
			FabricNavigationPoint golemPoint = new FabricNavigationPointProvider()
					.pointAt(samplePosition, new FabricEnvironmentContext(region, golem));
			FabricNavigationPoint zombiePoint = new FabricNavigationPointProvider()
					.pointAt(samplePosition, new FabricEnvironmentContext(region, zombie));

			helper.assertTrue(golem.getBbWidth() > 1.0F, "Expected iron golem to require a wider footprint.");
			helper.assertTrue(golem.getBbHeight() > 2.0F, "Expected iron golem to require more than two vertical blocks.");
			helper.assertTrue(!golemPoint.isTraversable(), "Expected iron golem to be blocked by side-footprint stone.");
			helper.assertTrue(zombiePoint.isTraversable(), "Expected normal zombie to ignore side-footprint stone at this point.");
			helper.succeed();
		});
	}

	private static void runScenarioBenchmark(GameTestHelper helper, BenchmarkScenario scenario) {
		helper.runAfterDelay(2, () -> {
			recordResult(measureScenario(helper, scenario));
			helper.succeed();
		});
	}

	private static BenchmarkResult measureScenario(GameTestHelper helper, BenchmarkScenario scenario) {
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			measurePathCreation(helper, scenario.name(), scenario.mob(), scenario.target(), true);
			measurePathCreation(helper, scenario.name(), scenario.mob(), scenario.target(), false);
		}

		long[] vanillaNanos = new long[SAMPLE_ITERATIONS];
		long[] patheticNanos = new long[SAMPLE_ITERATIONS];
		int vanillaNodes = -1;
		int patheticNodes = -1;
		for (int i = 0; i < SAMPLE_ITERATIONS; i++) {
			if ((i & 1) == 0) {
				PathSample vanilla = measurePathCreation(helper, scenario.name(), scenario.mob(), scenario.target(), true);
				PathSample pathetic = measurePathCreation(helper, scenario.name(), scenario.mob(), scenario.target(), false);
				vanillaNanos[i] = vanilla.nanos();
				patheticNanos[i] = pathetic.nanos();
				vanillaNodes = vanilla.nodeCount();
				patheticNodes = pathetic.nodeCount();
			} else {
				PathSample pathetic = measurePathCreation(helper, scenario.name(), scenario.mob(), scenario.target(), false);
				PathSample vanilla = measurePathCreation(helper, scenario.name(), scenario.mob(), scenario.target(), true);
				patheticNanos[i] = pathetic.nanos();
				vanillaNanos[i] = vanilla.nanos();
				patheticNodes = pathetic.nodeCount();
				vanillaNodes = vanilla.nodeCount();
			}
		}

		long vanillaMedian = percentile(vanillaNanos, 0.50);
		long patheticMedian = percentile(patheticNanos, 0.50);
		long vanillaP95 = percentile(vanillaNanos, 0.95);
		long patheticP95 = percentile(patheticNanos, 0.95);
		double medianRatio = ratio(patheticMedian, vanillaMedian);
		double p95Ratio = ratio(patheticP95, vanillaP95);

		helper.assertTrue(Double.isFinite(medianRatio), "Expected a finite median ratio.");
		helper.assertTrue(vanillaNodes > 1, "Expected vanilla path to contain movement nodes.");
		helper.assertTrue(patheticNodes > 1, "Expected Pathetic path to contain movement nodes.");
		LOGGER.info(
				"pathfinding_benchmark scenario={} samples={} warmups={} vanillaMedianNs={} patheticMedianNs={} medianRatio={} vanillaP95Ns={} patheticP95Ns={} p95Ratio={} vanillaNodes={} patheticNodes={}",
				scenario.name(),
				SAMPLE_ITERATIONS,
				WARMUP_ITERATIONS,
				vanillaMedian,
				patheticMedian,
				formatRatio(medianRatio),
				vanillaP95,
				patheticP95,
				formatRatio(p95Ratio),
				vanillaNodes,
				patheticNodes
		);
		return new BenchmarkResult(scenario.name(), medianRatio, p95Ratio);
	}

	private static BenchmarkScenario flatShort(GameTestHelper helper) {
		fillPlatform(helper, 0, 0, 8, 2);
		return scenario(helper, "flat_short", 1, 1, 7, 1);
	}

	private static BenchmarkScenario flatWide(GameTestHelper helper) {
		fillPlatform(helper, 0, 0, 8, 4);
		return scenario(helper, "flat_wide", 1, 1, 7, 3);
	}

	private static BenchmarkScenario wallDetour(GameTestHelper helper) {
		fillPlatform(helper, 0, 0, 8, 4);
		for (int z = 0; z <= 3; z++) {
			helper.setBlock(new BlockPos(4, 1, z), Blocks.STONE);
			helper.setBlock(new BlockPos(4, 2, z), Blocks.STONE);
		}
		return scenario(helper, "wall_detour", 1, 2, 7, 2);
	}

	private static BenchmarkScenario hazardAvoid(GameTestHelper helper) {
		fillPlatform(helper, 0, 0, 8, 4);
		for (int x = 5; x <= 8; x++) {
			helper.setBlock(new BlockPos(x, 0, 2), Blocks.MAGMA_BLOCK);
		}
		return scenario(helper, "hazard_avoid", 1, 2, 7, 2);
	}

	private static BenchmarkScenario narrowCorridor(GameTestHelper helper) {
		fillPlatform(helper, 0, 0, 8, 2);
		for (int x = 0; x <= 8; x++) {
			helper.setBlock(new BlockPos(x, 1, 0), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 2, 0), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 2, 2), Blocks.STONE);
		}
		return scenario(helper, "narrow_corridor", 1, 1, 7, 1);
	}

	private static BenchmarkScenario enclosedGoalBackEntrance(GameTestHelper helper) {
		fillPlatform(helper, 0, 0, 10, 4);
		for (int x = 6; x <= 8; x++) {
			for (int z = 1; z <= 3; z++) {
				if (x == 7 && z == 2) {
					continue;
				}
				if (x == 8 && z == 2) {
					continue;
				}
				if (x == 6 || x == 8 || z == 1 || z == 3) {
					setTwoHighWall(helper, x, z);
				}
			}
		}
		return scenario(helper, "enclosed_goal_back_entrance", 1, 2, 7, 2);
	}

	private static BenchmarkScenario twoGateHazardDetour(GameTestHelper helper) {
		fillPlatform(helper, 0, 0, 12, 6);
		for (int z = 0; z <= 4; z++) {
			setTwoHighWall(helper, 4, z);
		}
		for (int z = 2; z <= 6; z++) {
			setTwoHighWall(helper, 8, z);
		}
		for (int x = 5; x <= 8; x++) {
			helper.setBlock(new BlockPos(x, 0, 3), Blocks.MAGMA_BLOCK);
		}
		return scenario(helper, "two_gate_hazard_detour", 1, 3, 11, 3);
	}

	private static BenchmarkScenario courtyardBacktrack(GameTestHelper helper) {
		fillPlatform(helper, 0, 0, 12, 8);
		for (int x = 5; x <= 11; x++) {
			setTwoHighWall(helper, x, 1);
			setTwoHighWall(helper, x, 7);
		}
		for (int z = 1; z <= 7; z++) {
			setTwoHighWall(helper, 5, z);
			if (z != 4) {
				setTwoHighWall(helper, 11, z);
			}
		}
		for (int z = 2; z <= 6; z++) {
			if (z != 6) {
				setTwoHighWall(helper, 8, z);
			}
		}
		for (int x = 6; x <= 8; x++) {
			helper.setBlock(new BlockPos(x, 0, 4), Blocks.MAGMA_BLOCK);
		}
		return scenario(helper, "courtyard_backtrack", 1, 4, 7, 4);
	}

	private static void fillPlatform(GameTestHelper helper, int xStart, int zStart, int xEnd, int zEnd) {
		for (int x = xStart; x <= xEnd; x++) {
			for (int z = zStart; z <= zEnd; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
			}
		}
	}

	private static PathNavigationRegion region(
			GameTestHelper helper,
			int startX,
			int startY,
			int startZ,
			int endX,
			int endY,
			int endZ
	) {
		return new PathNavigationRegion(
				helper.getLevel(),
				helper.absolutePos(new BlockPos(startX, startY, startZ)),
				helper.absolutePos(new BlockPos(endX, endY, endZ))
		);
	}

	private static PathPosition pathPosition(GameTestHelper helper, int x, int y, int z) {
		BlockPos sample = helper.absolutePos(new BlockPos(x, y, z));
		return PathPosition.of(sample.getX(), sample.getY(), sample.getZ());
	}

	private static void setTwoHighWall(GameTestHelper helper, int x, int z) {
		helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
		helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
	}

	private static BenchmarkScenario scenario(
			GameTestHelper helper,
			String name,
			int mobX,
			int mobZ,
			int targetX,
			int targetZ
	) {
		Mob mob = helper.spawn(EntityType.ZOMBIE, mobX, 1, mobZ, EntitySpawnReason.TRIGGERED);
		BlockPos target = helper.absolutePos(new BlockPos(targetX, 1, targetZ));
		return new BenchmarkScenario(name, mob, target);
	}

	private static PathSample measurePathCreation(
			GameTestHelper helper,
			String scenarioName,
			Mob mob,
			BlockPos target,
			boolean vanilla
	) {
		mob.getNavigation().stop();
		long startNanos = System.nanoTime();
		Path path = PathfindingBenchmarkControl.withPatheticDisabled(
				vanilla,
				() -> mob.getNavigation().createPath(target, 1)
		);
		long elapsedNanos = System.nanoTime() - startNanos;

		String engine = vanilla ? "vanilla" : "pathetic";
		helper.assertTrue(path != null, "Expected a path to be created for " + scenarioName + " using " + engine + ".");
		helper.assertTrue(path.getNodeCount() > 1, "Expected " + scenarioName + " path to contain movement nodes.");
		return new PathSample(elapsedNanos, path.getNodeCount());
	}

	private static long percentile(long[] values, double percentile) {
		long[] sorted = Arrays.copyOf(values, values.length);
		Arrays.sort(sorted);
		int index = Math.min(sorted.length - 1, Math.max(0, (int) Math.ceil(sorted.length * percentile) - 1));
		return sorted[index];
	}

	private static double ratio(long numerator, long denominator) {
		return denominator == 0 ? Double.NaN : (double) numerator / (double) denominator;
	}

	private static String formatRatio(double ratio) {
		return String.format(Locale.ROOT, "%.3f", ratio);
	}

	private static synchronized void recordResult(BenchmarkResult result) {
		COMPLETED_RESULTS.put(result.name(), result);
		if (!COMPLETED_RESULTS.keySet().containsAll(SCENARIO_NAMES)) {
			return;
		}

		double worstMedianRatio = 0.0D;
		double worstP95Ratio = 0.0D;
		String worstMedianScenario = "";
		String worstP95Scenario = "";
		for (BenchmarkResult completed : COMPLETED_RESULTS.values()) {
			if (completed.medianRatio() > worstMedianRatio) {
				worstMedianRatio = completed.medianRatio();
				worstMedianScenario = completed.name();
			}
			if (completed.p95Ratio() > worstP95Ratio) {
				worstP95Ratio = completed.p95Ratio();
				worstP95Scenario = completed.name();
			}
		}

		LOGGER.info(
				"pathfinding_benchmark_aggregate scenarios={} samples={} warmups={} worstMedianScenario={} worstMedianRatio={} worstP95Scenario={} worstP95Ratio={}",
				COMPLETED_RESULTS.size(),
				SAMPLE_ITERATIONS,
				WARMUP_ITERATIONS,
				worstMedianScenario,
				formatRatio(worstMedianRatio),
				worstP95Scenario,
				formatRatio(worstP95Ratio)
		);
		COMPLETED_RESULTS.clear();
	}

	private record PathSample(long nanos, int nodeCount) {
	}

	private record BenchmarkScenario(String name, Mob mob, BlockPos target) {
	}

	private record BenchmarkResult(String name, double medianRatio, double p95Ratio) {
	}
}
