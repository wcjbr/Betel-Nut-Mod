package betel.nut.component;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.message.BetelMessages;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.CopyableComponent;

public class BetelNutAddictionComponent implements CopyableComponent<BetelNutAddictionComponent> {
	private static final String ADDICTION_VALUE_KEY = "addictionValue";
	private static final String WITHDRAWAL_VALUE_KEY = "withdrawalValue";
	private static final String LAST_EAT_TIME_KEY = "lastEatTime";
	private static final String CLEAN_TIME_KEY = "cleanTime";
	private static final String NOTIFIED_WITHDRAWAL_STAGE_KEY = "notifiedWithdrawalStage";
	private static final ResourceLocation WITHDRAWAL_MAX_HEALTH_PENALTY_ID = BetelNutMod
			.id("withdrawal_max_health_penalty");

	private int addictionValue;
	private int withdrawalValue;
	private long lastEatTime;
	private long cleanTime;
	private int notifiedWithdrawalStage;
	private long nextFeedbackTime;
	private long nextEatingRestrictionMessageTime;
	private long respawnWithdrawalCheckTime = -1;
	private boolean showRespawnWithdrawalMessage;

	public BetelNutAddictionComponent(Player player) {
	}

	public int getAddictionValue() {
		return this.addictionValue;
	}

	public int getWithdrawalValue() {
		return this.withdrawalValue;
	}

	public long getLastEatTime() {
		return this.lastEatTime;
	}

	public long getCleanTime() {
		return this.cleanTime;
	}

	public int getNotifiedWithdrawalStage() {
		return this.notifiedWithdrawalStage;
	}

	public int getWithdrawalStage() {
		return BetelMessages.withdrawalStage(this.withdrawalValue);
	}

	public double getCurrentMaxHealthPenalty(ServerPlayer player) {
		AttributeModifier modifier = getWithdrawalMaxHealthModifier(player);
		return modifier == null ? 0.0D : Math.max(0.0D, -modifier.amount());
	}

	public boolean hasWithdrawalMaxHealthPenalty(ServerPlayer player) {
		return getWithdrawalMaxHealthModifier(player) != null;
	}

	public void eatBetelNut(ServerPlayer player, int addictionIncrease, long gameTime) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem) {
			return;
		}

		int previousAddiction = this.addictionValue;
		int previousWithdrawal = this.withdrawalValue;
		this.addictionValue = clamp(this.addictionValue + addictionIncrease, 0, config.maxAddictionValue);
		this.withdrawalValue = 0;
		this.notifiedWithdrawalStage = 0;
		this.lastEatTime = gameTime;
		boolean removedMaxHealthPenalty = clearWithdrawalPenalties(player);

		if (previousWithdrawal >= 25) {
			sendFeedback(player, gameTime, BetelMessages.WITHDRAWAL_SUPPRESSED);
		} else {
			sendFeedback(player, gameTime,
					BetelMessages.betelNutEatenMessage(previousAddiction, this.addictionValue,
							config.maxAddictionValue));
		}
		sendRecoveryFeedbackIfNeeded(player, removedMaxHealthPenalty);
	}

	public void applyMilkRelief(ServerPlayer player, long gameTime) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem || !hasAddictionData()) {
			return;
		}

		this.cleanTime = Math.max(this.cleanTime, gameTime + config.milkReliefDurationTicks);
		boolean removedMaxHealthPenalty = clearWithdrawalPenalties(player);
		sendFeedback(player, gameTime, BetelMessages.MILK_RELIEF);
		sendRecoveryFeedbackIfNeeded(player, removedMaxHealthPenalty);
	}

	public void applyGoldenAppleRecovery(ServerPlayer player, long gameTime) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem || !hasAddictionData()) {
			return;
		}

		boolean hadMaxHealthPenalty = hasWithdrawalMaxHealthPenalty(player);
		this.addictionValue = clamp(this.addictionValue - config.goldenAppleAddictionReduction, 0,
				config.maxAddictionValue);
		this.withdrawalValue = clamp(this.withdrawalValue - config.goldenAppleWithdrawalReduction, 0,
				config.maxWithdrawalValue);
		syncNotifiedWithdrawalStageAfterDecrease();

		boolean removedMaxHealthPenalty = false;
		if (this.withdrawalValue < 25) {
			removedMaxHealthPenalty = clearWithdrawalPenalties(player);
		} else {
			applyWithdrawalEffects(player, config);
		}

		sendFeedback(player, gameTime, BetelMessages.GOLDEN_APPLE_RECOVERY);
		sendRecoveryFeedbackIfNeeded(player,
				removedMaxHealthPenalty || (hadMaxHealthPenalty && !hasWithdrawalMaxHealthPenalty(player)));
	}

	public void applyEnchantedGoldenAppleRecovery(ServerPlayer player, long gameTime) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem || !hasAddictionData()) {
			return;
		}

		this.addictionValue = clamp(this.addictionValue - config.enchantedGoldenAppleAddictionReduction, 0,
				config.maxAddictionValue);
		this.withdrawalValue = 0;
		this.notifiedWithdrawalStage = 0;
		this.cleanTime = Math.max(this.cleanTime, gameTime + config.enchantedGoldenAppleCleanTimeTicks);
		boolean removedMaxHealthPenalty = clearWithdrawalPenalties(player);
		sendFeedback(player, gameTime, BetelMessages.ENCHANTED_GOLDEN_APPLE_RECOVERY);
		sendRecoveryFeedbackIfNeeded(player, removedMaxHealthPenalty);
	}

	public void serverTick(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem) {
			clearWithdrawalPenalties(player);
			return;
		}

		long gameTime = player.level().getGameTime();

		if (this.addictionValue <= 0) {
			this.withdrawalValue = 0;
			this.notifiedWithdrawalStage = 0;
			clearWithdrawalPenalties(player);
			return;
		}

		if (this.lastEatTime <= 0 || gameTime < this.lastEatTime) {
			this.lastEatTime = gameTime;
		}

		if (this.cleanTime > gameTime) {
			clearWithdrawalPenalties(player);
			return;
		}

		int previousWithdrawal = this.withdrawalValue;
		updateWithdrawalValue(gameTime, config);
		if (this.withdrawalValue > previousWithdrawal) {
			BetelNutMod.LOGGER.debug(
					"AddictionTickHandler increased withdrawal for {}: addiction={}, withdrawal {} -> {}, timeSinceLastEat={} ticks",
					player.getScoreboardName(), this.addictionValue, previousWithdrawal, this.withdrawalValue,
					Math.max(0, gameTime - this.lastEatTime));
		}
		applyWithdrawalEffects(player, config);
	}

	public void setAddictionValue(int value) {
		BetelNutConfig config = BetelNutConfig.get();
		this.addictionValue = clamp(value, 0, config.maxAddictionValue);
		if (this.addictionValue <= 0) {
			this.withdrawalValue = 0;
			this.notifiedWithdrawalStage = 0;
		}
	}

	public void setAddictionValueAndResetLastEatTime(ServerPlayer player, int value) {
		setAddictionValue(value);
		resetLastEatTime(player);
	}

	public void addAddictionValue(int value) {
		setAddictionValue(this.addictionValue + value);
	}

	public void resetLastEatTime(ServerPlayer player) {
		this.lastEatTime = player.level().getGameTime();
		this.withdrawalValue = 0;
		this.notifiedWithdrawalStage = 0;
		clearWithdrawalPenalties(player);
	}

	public void setWithdrawalValue(ServerPlayer player, int value) {
		BetelNutConfig config = BetelNutConfig.get();
		this.withdrawalValue = clamp(value, 0, config.maxWithdrawalValue);
		syncNotifiedWithdrawalStageAfterDecrease();

		if (this.withdrawalValue < 25) {
			clearWithdrawalPenalties(player);
		} else {
			applyWithdrawalEffects(player, config);
		}
	}

	public void scheduleRespawnWithdrawalCheck(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		this.respawnWithdrawalCheckTime = -1;
		this.showRespawnWithdrawalMessage = false;

		if (!config.enableAddictionSystem || !config.reapplyWithdrawalAfterRespawn
				|| this.addictionValue <= 0 || this.withdrawalValue < 25) {
			return;
		}

		long gameTime = player.level().getGameTime();
		if (this.cleanTime > gameTime) {
			return;
		}

		this.respawnWithdrawalCheckTime = gameTime + config.respawnWithdrawalDelayTicks;
		this.showRespawnWithdrawalMessage = true;
	}

	public void handleRespawnWithdrawalCheck(ServerPlayer player) {
		if (this.respawnWithdrawalCheckTime < 0) {
			return;
		}

		long gameTime = player.level().getGameTime();
		if (gameTime < this.respawnWithdrawalCheckTime) {
			return;
		}

		boolean shouldShowMessage = this.showRespawnWithdrawalMessage;
		this.respawnWithdrawalCheckTime = -1;
		this.showRespawnWithdrawalMessage = false;

		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem || !config.reapplyWithdrawalAfterRespawn
				|| this.addictionValue <= 0 || this.withdrawalValue < 25 || this.cleanTime > gameTime) {
			return;
		}

		applyWithdrawalEffects(player, config);
		if (shouldShowMessage) {
			BetelMessages.send(player, BetelMessages.WITHDRAWAL_CONTINUES_AFTER_DEATH);
		}
	}

	public void triggerWithdrawalTest(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem) {
			return;
		}

		long gameTime = player.level().getGameTime();
		int minimumAddiction = Math.max(1, config.minimumAddictionForWithdrawal);
		if (this.addictionValue < minimumAddiction) {
			this.addictionValue = clamp(minimumAddiction, 1, config.maxAddictionValue);
		}

		this.cleanTime = 0;
		int gainPerInterval = Math.max(1, withdrawalGainPerInterval(config));
		int intervalsToFirstEffect = Math.max(1, (25 + gainPerInterval - 1) / gainPerInterval);
		long ticksBeforeFirstEffect = config.timeBeforeWithdrawalTicks
				+ (long) (intervalsToFirstEffect - 1) * config.withdrawalCheckIntervalTicks;
		this.lastEatTime = Math.max(1, gameTime - ticksBeforeFirstEffect);

		int previousWithdrawal = this.withdrawalValue;
		updateWithdrawalValue(gameTime, config);
		this.withdrawalValue = Math.max(this.withdrawalValue, 25);
		applyWithdrawalEffects(player, config);

		BetelNutMod.LOGGER.info(
				"Triggered betel withdrawal test for {}: addiction={}, withdrawal {} -> {}, lastEatTime={}, gameTime={}",
				player.getScoreboardName(), this.addictionValue, previousWithdrawal, this.withdrawalValue,
				this.lastEatTime, gameTime);
	}

	public void clearAddiction(ServerPlayer player) {
		this.addictionValue = 0;
		this.withdrawalValue = 0;
		this.lastEatTime = 0;
		this.cleanTime = 0;
		this.notifiedWithdrawalStage = 0;
		clearWithdrawalPenalties(player);
	}

	public void clearActiveWithdrawalPenalties(ServerPlayer player) {
		clearWithdrawalPenalties(player);
	}

	public void clearWithdrawalMaxHealthPenalty(ServerPlayer player) {
		removeWithdrawalMaxHealthPenalty(player);
	}

	public void sendEatingRestrictionMessage(ServerPlayer player, String message) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.showEatingRestrictionMessage) {
			return;
		}

		long gameTime = player.level().getGameTime();
		if (gameTime < this.nextEatingRestrictionMessageTime) {
			return;
		}

		if (BetelMessages.send(player, message)) {
			this.nextEatingRestrictionMessageTime = gameTime + config.eatingRestrictionMessageCooldownTicks;
		}
	}

	private void updateWithdrawalValue(long gameTime, BetelNutConfig config) {
		if (this.addictionValue < config.minimumAddictionForWithdrawal) {
			return;
		}

		long ticksWithoutBetel = gameTime - this.lastEatTime;
		if (ticksWithoutBetel < config.timeBeforeWithdrawalTicks) {
			return;
		}

		long withdrawalTicks = ticksWithoutBetel - config.timeBeforeWithdrawalTicks;
		int intervals = (int) (withdrawalTicks / config.withdrawalCheckIntervalTicks) + 1;
		int targetWithdrawal = clamp(intervals * withdrawalGainPerInterval(config), 0,
				config.maxWithdrawalValue);

		if (targetWithdrawal > this.withdrawalValue) {
			this.withdrawalValue = targetWithdrawal;
		}
	}

	private int withdrawalGainPerInterval(BetelNutConfig config) {
		if (this.addictionValue >= percentThreshold(config.maxAddictionValue, 75)) {
			return config.baseWithdrawalIncrease * 4;
		}
		if (this.addictionValue >= percentThreshold(config.maxAddictionValue, 50)) {
			return config.baseWithdrawalIncrease * 2;
		}
		return config.baseWithdrawalIncrease;
	}

	private void applyWithdrawalEffects(ServerPlayer player, BetelNutConfig config) {
		if (this.withdrawalValue < 25) {
			this.notifiedWithdrawalStage = 0;
			clearWithdrawalPenalties(player);
			return;
		}

		sendWithdrawalStageFeedback(player);

		if (this.withdrawalValue >= config.maxWithdrawalValue) {
			applyWithdrawalMaxHealthPenalty(player, config, config.stage4MaxHealthPenalty);
			player.addEffect(withdrawalEffect(config, MobEffects.MOVEMENT_SLOWDOWN, 2));
			player.addEffect(withdrawalEffect(config, MobEffects.DIG_SLOWDOWN, 2));
			player.addEffect(withdrawalEffect(config, MobEffects.WEAKNESS, 1));
			player.addEffect(withdrawalEffect(config, MobEffects.HUNGER, 2));
			player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, config.withdrawalNauseaDurationTicks, 0));
			if (config.enableStage4BlindnessOrDarkness) {
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, config.withdrawalNauseaDurationTicks, 0));
			}
		} else if (this.withdrawalValue >= 75) {
			applyWithdrawalMaxHealthPenalty(player, config, config.stage3MaxHealthPenalty);
			player.addEffect(withdrawalEffect(config, MobEffects.MOVEMENT_SLOWDOWN, 2));
			player.addEffect(withdrawalEffect(config, MobEffects.DIG_SLOWDOWN, 2));
			player.addEffect(withdrawalEffect(config, MobEffects.WEAKNESS, 1));
			player.addEffect(withdrawalEffect(config, MobEffects.HUNGER, 1));
			if (player.getRandom().nextFloat() < 0.35F) {
				player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, config.withdrawalNauseaDurationTicks, 0));
			}
		} else if (this.withdrawalValue >= 50) {
			applyWithdrawalMaxHealthPenalty(player, config, config.stage2MaxHealthPenalty);
			player.addEffect(withdrawalEffect(config, MobEffects.MOVEMENT_SLOWDOWN, 1));
			player.addEffect(withdrawalEffect(config, MobEffects.DIG_SLOWDOWN, 1));
			player.addEffect(withdrawalEffect(config, MobEffects.WEAKNESS, 0));
			player.addEffect(withdrawalEffect(config, MobEffects.HUNGER, 1));
		} else {
			removeWithdrawalMaxHealthPenalty(player);
			int stageOneAmplifier = config.stage1EffectAmplifierOffset;
			player.addEffect(withdrawalEffect(config, MobEffects.MOVEMENT_SLOWDOWN, stageOneAmplifier));
			player.addEffect(withdrawalEffect(config, MobEffects.DIG_SLOWDOWN, stageOneAmplifier));
			player.addEffect(withdrawalEffect(config, MobEffects.HUNGER, 0));
		}
	}

	private void sendWithdrawalStageFeedback(ServerPlayer player) {
		int currentStage = BetelMessages.withdrawalStage(this.withdrawalValue);
		if (currentStage > this.notifiedWithdrawalStage) {
			String message = BetelMessages.withdrawalStageMessage(currentStage);
			if (!message.isEmpty()) {
				BetelMessages.send(player, message);
			}
			this.notifiedWithdrawalStage = currentStage;
		}
	}

	private void sendFeedback(ServerPlayer player, long gameTime, String message) {
		if (gameTime < this.nextFeedbackTime) {
			return;
		}

		if (BetelMessages.send(player, message)) {
			this.nextFeedbackTime = gameTime + BetelNutConfig.get().feedbackCooldownTicks;
		}
	}

	private boolean hasAddictionData() {
		return this.addictionValue > 0 || this.withdrawalValue > 0;
	}

	private void syncNotifiedWithdrawalStageAfterDecrease() {
		int currentStage = BetelMessages.withdrawalStage(this.withdrawalValue);
		if (currentStage < this.notifiedWithdrawalStage) {
			this.notifiedWithdrawalStage = currentStage;
		}
	}

	private static MobEffectInstance withdrawalEffect(BetelNutConfig config,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
			int amplifier) {
		return new MobEffectInstance(effect, config.withdrawalEffectDurationTicks, amplifier, false, true, true);
	}

	private static boolean clearWithdrawalPenalties(ServerPlayer player) {
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		player.removeEffect(MobEffects.DIG_SLOWDOWN);
		player.removeEffect(MobEffects.WEAKNESS);
		player.removeEffect(MobEffects.HUNGER);
		player.removeEffect(MobEffects.CONFUSION);
		player.removeEffect(MobEffects.BLINDNESS);
		player.removeEffect(MobEffects.DARKNESS);
		return removeWithdrawalMaxHealthPenalty(player);
	}

	private static double applyWithdrawalMaxHealthPenalty(ServerPlayer player, BetelNutConfig config,
			double requestedPenalty) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return 0.0D;
		}

		maxHealth.removeModifier(WITHDRAWAL_MAX_HEALTH_PENALTY_ID);
		if (!config.enableWithdrawalMaxHealthPenalty || requestedPenalty <= 0.0D) {
			return 0.0D;
		}

		double maxHealthBeforePenalty = maxHealth.getValue();
		double allowedPenalty = Math.max(0.0D, maxHealthBeforePenalty - config.minimumMaxHealthAfterPenalty);
		double appliedPenalty = Math.min(requestedPenalty, allowedPenalty);
		if (appliedPenalty <= 0.0D) {
			return 0.0D;
		}

		maxHealth.addOrUpdateTransientModifier(new AttributeModifier(WITHDRAWAL_MAX_HEALTH_PENALTY_ID,
				-appliedPenalty, AttributeModifier.Operation.ADD_VALUE));
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		return appliedPenalty;
	}

	private static boolean removeWithdrawalMaxHealthPenalty(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		return maxHealth != null && maxHealth.removeModifier(WITHDRAWAL_MAX_HEALTH_PENALTY_ID);
	}

	private static AttributeModifier getWithdrawalMaxHealthModifier(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		return maxHealth == null ? null : maxHealth.getModifier(WITHDRAWAL_MAX_HEALTH_PENALTY_ID);
	}

	private static void sendRecoveryFeedbackIfNeeded(ServerPlayer player, boolean removedMaxHealthPenalty) {
		if (removedMaxHealthPenalty) {
			BetelMessages.send(player, BetelMessages.WITHDRAWAL_BODY_RECOVERING);
		}
	}

	@Override
	public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		BetelNutConfig config = BetelNutConfig.get();
		this.addictionValue = clamp(tag.getInt(ADDICTION_VALUE_KEY), 0, config.maxAddictionValue);
		this.withdrawalValue = clamp(tag.getInt(WITHDRAWAL_VALUE_KEY), 0, config.maxWithdrawalValue);
		this.lastEatTime = tag.getLong(LAST_EAT_TIME_KEY);
		this.cleanTime = tag.getLong(CLEAN_TIME_KEY);
		this.notifiedWithdrawalStage = clamp(tag.getInt(NOTIFIED_WITHDRAWAL_STAGE_KEY), 0, 4);
	}

	@Override
	public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		tag.putInt(ADDICTION_VALUE_KEY, this.addictionValue);
		tag.putInt(WITHDRAWAL_VALUE_KEY, this.withdrawalValue);
		tag.putLong(LAST_EAT_TIME_KEY, this.lastEatTime);
		tag.putLong(CLEAN_TIME_KEY, this.cleanTime);
		tag.putInt(NOTIFIED_WITHDRAWAL_STAGE_KEY, this.notifiedWithdrawalStage);
	}

	@Override
	public void copyFrom(BetelNutAddictionComponent other, HolderLookup.Provider registryLookup) {
		copyAllFrom(other);
	}

	public void copyForRespawn(BetelNutAddictionComponent other, boolean lossless) {
		if (lossless) {
			copyAllFrom(other);
			return;
		}

		BetelNutConfig config = BetelNutConfig.get();
		if (!config.keepAddictionAfterDeath) {
			clearStoredData();
			return;
		}

		this.addictionValue = other.addictionValue;
		this.cleanTime = other.cleanTime;

		if (config.keepWithdrawalAfterDeath) {
			this.withdrawalValue = other.withdrawalValue;
			this.lastEatTime = other.lastEatTime;
			this.notifiedWithdrawalStage = other.notifiedWithdrawalStage;
		} else {
			this.withdrawalValue = 0;
			this.lastEatTime = 0;
			this.notifiedWithdrawalStage = 0;
		}
	}

	private void copyAllFrom(BetelNutAddictionComponent other) {
		this.addictionValue = other.addictionValue;
		this.withdrawalValue = other.withdrawalValue;
		this.lastEatTime = other.lastEatTime;
		this.cleanTime = other.cleanTime;
		this.notifiedWithdrawalStage = other.notifiedWithdrawalStage;
	}

	private void clearStoredData() {
		this.addictionValue = 0;
		this.withdrawalValue = 0;
		this.lastEatTime = 0;
		this.cleanTime = 0;
		this.notifiedWithdrawalStage = 0;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int percentThreshold(int maxValue, int percent) {
		return Math.max(1, maxValue * percent / 100);
	}
}
