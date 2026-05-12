package kim.biryeong.pathetic;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import kim.biryeong.pathetic.pathfinding.PathfindingBenchmarkControl;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class PatheticDebugCommands {
	private static final SimpleCommandExceptionType ENTITY_NOT_MOB =
			new SimpleCommandExceptionType(Component.literal("entity_type must be a Mob for pathfinding comparison."));
	private static final SimpleCommandExceptionType SPAWN_FAILED =
			new SimpleCommandExceptionType(Component.literal("Failed to spawn pathfinding comparison entities."));

	private PatheticDebugCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register(PatheticDebugCommands::registerCommands);
	}

	private static void registerCommands(
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext registryAccess,
			net.minecraft.commands.Commands.CommandSelection environment
	) {
		dispatcher.register(literal("pathetic")
				.then(literal("pathdebug")
						.then(literal("spawn")
								.then(argument("entity_type", ResourceArgument.resource(registryAccess, Registries.ENTITY_TYPE))
										.then(argument("pos", Vec3Argument.vec3())
												.executes(PatheticDebugCommands::spawnComparisonEntities))))));
	}

	private static int spawnComparisonEntities(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Holder.Reference<EntityType<?>> holder = ResourceArgument.getSummonableEntityType(context, "entity_type");
		EntityType<?> entityType = holder.value();
		Vec3 pos = Vec3Argument.getVec3(context, "pos");
		ServerLevel level = context.getSource().getLevel();
		BlockPos spawnBlock = BlockPos.containing(pos);

		Mob vanilla = spawnMob(entityType, level, spawnBlock, pos.add(-0.6D, 0.0D, 0.0D), "Vanilla");
		Mob patched = spawnMob(entityType, level, spawnBlock, pos.add(0.6D, 0.0D, 0.0D), "Pathetic");
		PathfindingBenchmarkControl.useVanillaPathfinding(vanilla);

		String typeName = EntityType.getKey(entityType).toString();
		context.getSource().sendSuccess(
				() -> Component.literal("Spawned pathfinding comparison for " + typeName + " at " + formatPos(pos)),
				true
		);
		return 2;
	}

	private static Mob spawnMob(
			EntityType<?> entityType,
			ServerLevel level,
			BlockPos spawnBlock,
			Vec3 pos,
			String pathfinderName
	) throws CommandSyntaxException {
		Entity entity = entityType.spawn(level, spawnBlock, EntitySpawnReason.COMMAND);
		if (entity == null) {
			throw SPAWN_FAILED.create();
		}
		if (!(entity instanceof Mob mob)) {
			entity.discard();
			throw ENTITY_NOT_MOB.create();
		}

		String typeName = EntityType.getKey(entityType).toString();
		mob.setPos(pos);
		mob.setCustomName(Component.literal("[" + pathfinderName + "] " + typeName));
		mob.setCustomNameVisible(true);
		mob.setPersistenceRequired();
		return mob;
	}

	private static String formatPos(Vec3 pos) {
		return String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", pos.x, pos.y, pos.z);
	}
}
