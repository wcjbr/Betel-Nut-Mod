package betel.nut.event;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BetelNutEvents {
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			BetelNutConfig config = BetelNutConfig.get();
			if (!config.enableAddictionSystem) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					if (player.isAlive()) {
						BetelNutEntityComponents.ADDICTION.get(player).clearActiveWithdrawalPenalties(player);
					}
				}
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isAlive()) {
					BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
					if (!config.enableWithdrawalMaxHealthPenalty) {
						addiction.clearWithdrawalMaxHealthPenalty(player);
					}
					addiction.handleRespawnWithdrawalCheck(player);
				}
			}

			if (server.getTickCount() % config.withdrawalCheckIntervalTicks != 0) {
				return;
			}

			BetelNutMod.LOGGER.debug("AddictionTickHandler tick: serverTick={}, interval={}, players={}",
					server.getTickCount(), config.withdrawalCheckIntervalTicks,
					server.getPlayerList().getPlayerCount());

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isAlive()) {
					BetelNutEntityComponents.ADDICTION.get(player).serverTick(player);
				}
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				BetelNutEntityComponents.ADDICTION.get(newPlayer).scheduleRespawnWithdrawalCheck(newPlayer);
			}
		});

		BetelNutMod.LOGGER.info("Betel nut addiction system initialized successfully");
	}

	public static void handleFinishedUsingItem(ServerPlayer player, ItemStack stack, long gameTime) {
		if (!BetelNutConfig.get().enableAddictionSystem) {
			return;
		}

		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);

		if (stack.is(Items.MILK_BUCKET)) {
			addiction.applyMilkRelief(player, gameTime);
		} else if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
			addiction.applyEnchantedGoldenAppleRecovery(player, gameTime);
		} else if (stack.is(Items.GOLDEN_APPLE)) {
			addiction.applyGoldenAppleRecovery(player, gameTime);
		}
	}

	private BetelNutEvents() {
	}
}
