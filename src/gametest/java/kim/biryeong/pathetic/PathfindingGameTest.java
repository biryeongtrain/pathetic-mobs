package kim.biryeong.pathetic;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathfindingGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PathfindingGameTest.class);

	@GameTest
	public void groundMobCreatesPatheticBackedPath(GameTestHelper helper) {
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
			long startNanos = System.nanoTime();
			Path path = mob.getNavigation().createPath(target, 1);
			long elapsedNanos = System.nanoTime() - startNanos;

			helper.assertTrue(path != null, "Expected a path to be created.");
			helper.assertTrue(path.getNodeCount() > 1, "Expected the path to contain movement nodes.");
			LOGGER.info("pathetic_pathfinding_gametest elapsedNanos={} nodeCount={}", elapsedNanos, path.getNodeCount());
			helper.succeed();
		});
	}
}
