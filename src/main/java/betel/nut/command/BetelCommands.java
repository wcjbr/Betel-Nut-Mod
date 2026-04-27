package betel.nut.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.event.WithdrawalEatingRestrictions;
import betel.nut.event.WithdrawalEatingRestrictions.RestrictionLevel;
import betel.nut.item.EnderBetelTeleportHandler;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class BetelCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("betel")
						.requires(source -> source.hasPermission(2))
						.then(literal("addiction")
								.then(literal("get").executes(BetelCommands::getAddiction))
								.then(literal("set")
										.then(argument("value", IntegerArgumentType.integer(0))
												.executes(BetelCommands::setAddiction)
												.then(literal("reset_timer")
														.executes(BetelCommands::setAddictionAndResetTimer))))
								.then(literal("clear").executes(BetelCommands::clearAddiction))
								.then(literal("add")
										.then(argument("value", IntegerArgumentType.integer(0))
												.executes(BetelCommands::addAddiction))))
						.then(literal("withdrawal")
								.then(literal("set")
										.then(argument("value", IntegerArgumentType.integer(0))
												.executes(BetelCommands::setWithdrawal)))
								.then(literal("trigger").executes(BetelCommands::triggerWithdrawal)))
						.then(literal("ender")
								.then(literal("teleport").executes(BetelCommands::teleportEnderBetel)))
						.then(literal("tree")
								.then(literal("generate").executes(BetelCommands::generateTree)))
						.then(literal("trades")
								.then(literal("info").executes(BetelCommands::showTradeInfo)))
						.then(literal("eatingtest").executes(BetelCommands::eatingTest))
						.then(literal("reload").executes(BetelCommands::reloadConfig))));

		BetelNutMod.LOGGER.info("Betel nut debug commands registered successfully");
	}

	private static int getAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		BetelNutConfig config = BetelNutConfig.get();
		long gameTime = player.level().getGameTime();
		long lastEatTime = addiction.getLastEatTime();
		long timeSinceLastEat = lastEatTime <= 0 ? 0 : Math.max(0, gameTime - lastEatTime);
		String lastEatText = lastEatTime <= 0
				? "\u65e0"
				: lastEatTime + "\uff08\u8ddd\u4eca " + timeSinceLastEat + " tick\uff09";
		long cleanRemainingTicks = Math.max(0, addiction.getCleanTime() - gameTime);
		String withdrawalStartRemaining = withdrawalStartRemainingText(addiction, config, timeSinceLastEat,
				cleanRemainingTicks);
		double maxHealthPenalty = addiction.getCurrentMaxHealthPenalty(player);
		boolean hasMaxHealthPenalty = addiction.hasWithdrawalMaxHealthPenalty(player);
		RestrictionLevel eatingRestrictionLevel = WithdrawalEatingRestrictions.getRestrictionLevel(player, addiction);
		boolean eatingRestrictionEnabled = WithdrawalEatingRestrictions.isFeatureEnabled(config);

		context.getSource().sendSuccess(() -> Component.literal(
				"\u69df\u6994\u6210\u763e\u72b6\u6001\uff1a\u6210\u763e\u503c " + addiction.getAddictionValue()
						+ "\uff0c\u6212\u65ad\u503c " + addiction.getWithdrawalValue()
						+ "\uff0c\u5f53\u524d\u6212\u65ad\u9636\u6bb5 " + addiction.getWithdrawalStage()
						+ "\uff0c\u5df2\u63d0\u793a\u6212\u65ad\u9636\u6bb5 " + addiction.getNotifiedWithdrawalStage()
						+ "\uff0c\u751f\u547d\u4e0a\u9650\u60e9\u7f5a " + maxHealthPenalty
						+ "\uff0c\u6b63\u5728\u53d7\u751f\u547d\u4e0a\u9650\u60e9\u7f5a " + hasMaxHealthPenalty
						+ "\uff0c\u4e0a\u6b21\u98df\u7528\u65f6\u95f4 " + lastEatText
						+ "\uff0ctimeSinceLastEat " + timeSinceLastEat + " tick"
						+ "\uff0cwithdrawalStartRemaining " + withdrawalStartRemaining
						+ "\uff0c\u6e05\u9192\u4fdd\u62a4\u5269\u4f59 " + cleanRemainingTicks + " tick"
						+ "\uff0c\u8fdb\u98df\u9650\u5236\u542f\u7528 " + eatingRestrictionEnabled
						+ "\uff0c\u5f53\u524d\u8fdb\u98df\u9650\u5236\u7b49\u7ea7 "
						+ eatingRestrictionLevel.label() + "\u3002"),
				false);
		return Command.SINGLE_SUCCESS;
	}

	private static int setAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.setAddictionValue(value);
		if (addiction.getAddictionValue() <= 0) {
			addiction.clearActiveWithdrawalPenalties(player);
		}

		context.getSource().sendSuccess(() -> Component.literal(
				"\u5df2\u5c06\u4f60\u7684\u69df\u6994\u6210\u763e\u503c\u8bbe\u7f6e\u4e3a "
						+ addiction.getAddictionValue() + "\u3002"),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int setAddictionAndResetTimer(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.setAddictionValueAndResetLastEatTime(player, value);

		context.getSource().sendSuccess(() -> Component.literal(
				"Set betel addiction to " + addiction.getAddictionValue()
						+ " and reset lastEatTime to the current game time."),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int addAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.addAddictionValue(value);

		context.getSource().sendSuccess(() -> Component.literal(
				"\u5df2\u589e\u52a0\u69df\u6994\u6210\u763e\u503c " + value
						+ "\uff0c\u5f53\u524d\u6210\u763e\u503c\u4e3a " + addiction.getAddictionValue()
						+ "\u3002"),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int clearAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutEntityComponents.ADDICTION.get(player).clearAddiction(player);

		context.getSource().sendSuccess(() -> Component.literal(
				"\u5df2\u6e05\u7a7a\u4f60\u7684\u69df\u6994\u6210\u763e\u503c\u548c\u6212\u65ad\u503c\u3002"),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int setWithdrawal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.setWithdrawalValue(player, value);

		context.getSource().sendSuccess(() -> Component.literal(
				"\u5df2\u5c06\u4f60\u7684\u69df\u6994\u6212\u65ad\u503c\u8bbe\u7f6e\u4e3a "
						+ addiction.getWithdrawalValue() + "\u3002"),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int triggerWithdrawal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.triggerWithdrawalTest(player);

		context.getSource().sendSuccess(() -> Component.literal(
				"Triggered betel withdrawal test. addiction=" + addiction.getAddictionValue()
						+ ", withdrawal=" + addiction.getWithdrawalValue() + "."),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int teleportEnderBetel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		boolean teleported = EnderBetelTeleportHandler.tryTeleport(player, true);

		if (teleported) {
			context.getSource().sendSuccess(() -> Component.literal(
					"\u5df2\u6d4b\u8bd5\u672b\u5f71\u69df\u6994\u968f\u673a\u77ac\u79fb\u3002"), true);
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendFailure(Component.literal(
				"\u672b\u5f71\u69df\u6994\u77ac\u79fb\u6d4b\u8bd5\u5931\u8d25\uff1a\u6ca1\u6709\u627e\u5230\u5b89\u5168\u843d\u70b9\u3002"));
		return 0;
	}

	private static String withdrawalStartRemainingText(BetelNutAddictionComponent addiction, BetelNutConfig config,
			long timeSinceLastEat, long cleanRemainingTicks) {
		if (!config.enableAddictionSystem) {
			return "disabled";
		}
		if (addiction.getAddictionValue() <= 0) {
			return "no addiction";
		}
		if (addiction.getAddictionValue() < config.minimumAddictionForWithdrawal) {
			return "not eligible, needs addiction >= " + config.minimumAddictionForWithdrawal;
		}
		if (addiction.getLastEatTime() <= 0) {
			return "timer not started";
		}

		long withdrawalDelayRemaining = Math.max(0, config.timeBeforeWithdrawalTicks - timeSinceLastEat);
		long remaining = Math.max(withdrawalDelayRemaining, cleanRemainingTicks);
		if (remaining <= 0) {
			return "0 tick, detection active";
		}
		return remaining + " tick";
	}

	private static int reloadConfig(CommandContext<CommandSourceStack> context) {
		boolean loaded = BetelNutConfig.reload();

		if (loaded) {
			context.getSource().sendSuccess(() -> Component.literal(
					"\u69df\u6994\u914d\u7f6e\u5df2\u91cd\u65b0\u52a0\u8f7d\u3002"), true);
		} else {
			context.getSource().sendFailure(Component.literal(
					"\u69df\u6994\u914d\u7f6e\u8bfb\u53d6\u5931\u8d25\uff0c\u5df2\u56de\u9000\u5230\u9ed8\u8ba4\u914d\u7f6e\u3002\u8bf7\u67e5\u770b\u65e5\u5fd7\u3002"));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int showTradeInfo(CommandContext<CommandSourceStack> context) {
		BetelNutConfig config = BetelNutConfig.get();
		String status = config.enableFarmerTrades ? "enabled" : "disabled";

		context.getSource().sendSuccess(() -> Component.literal(
				"Farmer betel trades are " + status + ". "
						+ "Novice: 1 emerald -> " + config.farmerTradeRawBetelBuyCount
						+ " raw betel nut; " + config.farmerTradeRawBetelSellCount + " raw betel nut -> 1 emerald. "
						+ "Apprentice: 1 emerald -> " + config.farmerTradeLeafBuyCount + " betel leaf. "
						+ "Journeyman: 2 emeralds -> " + config.farmerTradeRoastedBetelBuyCount
						+ " roasted betel nut. "
						+ "Expert: " + config.farmerTradeFlavorEmeraldCost
						+ " emeralds -> 1 flavored betel nut. "
						+ "Master: " + config.farmerTradeSyntheticWorldEmeraldCost
						+ " emeralds + 1 roasted betel nut -> 1 synthetic world betel."),
				false);
		return Command.SINGLE_SUCCESS;
	}

	private static int eatingTest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		ItemStack stack = player.getMainHandItem();
		RestrictionLevel level = WithdrawalEatingRestrictions.getRestrictionLevel(player, addiction);
		boolean checkedItem = WithdrawalEatingRestrictions.isCheckedItem(stack);
		boolean canUse = WithdrawalEatingRestrictions.canUseAtCurrentRestriction(player, addiction, stack);
		String itemName = stack.isEmpty() ? "empty" : stack.getHoverName().getString();
		String result = canUse ? "\u53ef\u4ee5" : "\u4e0d\u80fd";
		String scope = checkedItem
				? "\u4f1a\u8fdb\u5165\u8fdb\u98df\u9650\u5236\u68c0\u67e5"
				: "\u4e0d\u662f\u98df\u7269\u6216\u6cbb\u7597\u996e\u54c1\uff0c\u4e0d\u4f1a\u88ab\u8fdb\u98df\u9650\u5236\u62e6\u622a";

		context.getSource().sendSuccess(() -> Component.literal(
				"\u8fdb\u98df\u6d4b\u8bd5\uff1a\u5f53\u524d\u624b\u6301\u7269\u54c1 " + itemName
						+ "\uff0c\u6212\u65ad\u9636\u6bb5 " + addiction.getWithdrawalStage()
						+ "\uff0c\u8fdb\u98df\u9650\u5236\u7b49\u7ea7 " + level.label()
						+ "\uff0c" + scope
						+ "\uff0c\u5f53\u524d\u9636\u6bb5" + result + "\u4f7f\u7528\u3002"),
				false);
		return Command.SINGLE_SUCCESS;
	}

	private static int generateTree(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutConfig config = BetelNutConfig.get();
		boolean generated = tryGenerateTree(player.serverLevel(), player.blockPosition(), player.getRandom(),
				config.betelPalmMinHeight, config.betelPalmMaxHeight);

		if (generated) {
			context.getSource().sendSuccess(() -> Component.literal(
					"\u5df2\u751f\u6210\u4e00\u68f5\u69df\u6994\u6811\u3002"), true);
		} else {
			context.getSource().sendFailure(Component.literal(
					"\u5f53\u524d\u4f4d\u7f6e\u4e0d\u9002\u5408\u751f\u6210\u69df\u6994\u6811\u3002"));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static boolean tryGenerateTree(ServerLevel level, BlockPos origin, RandomSource random, int minHeight,
			int maxHeight) {
		for (int radius = 0; radius <= 3; radius++) {
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					if (Math.abs(x) != radius && Math.abs(z) != radius) {
						continue;
					}

					if (BetelPalmTreeGenerator.generate(level, origin.offset(x, 0, z), random, minHeight, maxHeight)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private BetelCommands() {
	}
}
