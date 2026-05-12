package kim.biryeong.pathetic;

import java.util.Arrays;
import java.util.Locale;
import kim.biryeong.pathetic.pathfinding.PathfindingBenchmarkControl;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathfindingGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PathfindingGameTest.class);
	private static final int WARMUP_ITERATIONS = 8;
	private static final int SAMPLE_ITERATIONS = 40;

	@GameTest
	public void groundMobComparesVanillaAndPatheticPathfinding(GameTestHelper helper) {
		for (int x = 0; x <= 8; x++) {
			for (int z = 0; z <= 2; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
			}
		}

		Mob mob = helper.spawn(EntityType.ZOMBIE, 1, 1, 1, EntitySpawnReason.TRIGGERED);
		BlockPos target = helper.absolutePos(new BlockPos(7, 1, 1));
		helper.runAfterDelay(2, () -> {
			for (int i = 0; i < WARMUP_ITERATIONS; i++) {
				measurePathCreation(helper, mob, target, true);
				measurePathCreation(helper, mob, target, false);
			}

			long[] vanillaNanos = new long[SAMPLE_ITERATIONS];
			long[] patheticNanos = new long[SAMPLE_ITERATIONS];
			int vanillaNodes = -1;
			int patheticNodes = -1;
			for (int i = 0; i < SAMPLE_ITERATIONS; i++) {
				if ((i & 1) == 0) {
					PathSample vanilla = measurePathCreation(helper, mob, target, true);
					PathSample pathetic = measurePathCreation(helper, mob, target, false);
					vanillaNanos[i] = vanilla.nanos();
					patheticNanos[i] = pathetic.nanos();
					vanillaNodes = vanilla.nodeCount();
					patheticNodes = pathetic.nodeCount();
				} else {
					PathSample pathetic = measurePathCreation(helper, mob, target, false);
					PathSample vanilla = measurePathCreation(helper, mob, target, true);
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
					"pathfinding_benchmark scenario=flat samples={} warmups={} vanillaMedianNs={} patheticMedianNs={} medianRatio={} vanillaP95Ns={} patheticP95Ns={} p95Ratio={} vanillaNodes={} patheticNodes={}",
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
			helper.succeed();
		});
	}

	private static PathSample measurePathCreation(GameTestHelper helper, Mob mob, BlockPos target, boolean vanilla) {
		mob.getNavigation().stop();
		long startNanos = System.nanoTime();
		Path path = PathfindingBenchmarkControl.withPatheticDisabled(
				vanilla,
				() -> mob.getNavigation().createPath(target, 1)
		);
		long elapsedNanos = System.nanoTime() - startNanos;

		helper.assertTrue(path != null, "Expected a path to be created.");
		helper.assertTrue(path.getNodeCount() > 1, "Expected the path to contain movement nodes.");
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

	private record PathSample(long nanos, int nodeCount) {
	}
}
