package betel.nut.event;

import betel.nut.BetelNutConfig;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.item.ModItemTags;
import betel.nut.message.BetelMessages;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class WithdrawalEatingRestrictions {
	public enum RestrictionLevel {
		NONE("\u65e0\u9650\u5236", ""),
		SOFT_FOODS("\u4ec5\u8f6f\u98df", BetelMessages.EATING_RESTRICTION_STAGE2),
		ENCHANTED_GOLDEN_APPLE_ONLY("\u4ec5\u9644\u9b54\u91d1\u82f9\u679c",
				BetelMessages.EATING_RESTRICTION_STAGE3),
		BODY_REJECTS_FOOD("\u4ec5\u9644\u9b54\u91d1\u82f9\u679c", BetelMessages.EATING_RESTRICTION_STAGE4);

		private final String label;
		private final String blockMessage;

		RestrictionLevel(String label, String blockMessage) {
			this.label = label;
			this.blockMessage = blockMessage;
		}

		public String label() {
			return this.label;
		}

		private String blockMessage() {
			return this.blockMessage;
		}
	}

	public static void register() {
		UseItemCallback.EVENT.register(WithdrawalEatingRestrictions::onUseItem);
	}

	public static boolean isFeatureEnabled(BetelNutConfig config) {
		return config.enableAddictionSystem && config.enableWithdrawalEatingRestriction;
	}

	public static RestrictionLevel getRestrictionLevel(ServerPlayer player,
			BetelNutAddictionComponent addiction) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!isFeatureEnabled(config) || player.isCreative() || player.isSpectator()) {
			return RestrictionLevel.NONE;
		}

		long gameTime = player.level().getGameTime();
		if (addiction.getCleanTime() > gameTime) {
			return RestrictionLevel.NONE;
		}

		int withdrawalValue = addiction.getWithdrawalValue();
		if (withdrawalValue >= 100) {
			return RestrictionLevel.BODY_REJECTS_FOOD;
		}
		if (withdrawalValue >= config.stage3EatingRestrictionWithdrawalValue) {
			return RestrictionLevel.ENCHANTED_GOLDEN_APPLE_ONLY;
		}
		if (withdrawalValue >= config.stage2EatingRestrictionWithdrawalValue) {
			return RestrictionLevel.SOFT_FOODS;
		}
		return RestrictionLevel.NONE;
	}

	public static boolean isCheckedItem(ItemStack stack) {
		return !stack.isEmpty()
				&& (stack.has(DataComponents.FOOD)
						|| stack.is(Items.MILK_BUCKET)
						|| stack.is(ModItemTags.WITHDRAWAL_STAGE2_ALLOWED_FOODS)
						|| stack.is(ModItemTags.WITHDRAWAL_STAGE3_ALLOWED_FOODS));
	}

	public static boolean canUseAtCurrentRestriction(ServerPlayer player,
			BetelNutAddictionComponent addiction, ItemStack stack) {
		RestrictionLevel level = getRestrictionLevel(player, addiction);
		return level == RestrictionLevel.NONE
				|| !isCheckedItem(stack)
				|| isAllowedAtLevel(stack, level, BetelNutConfig.get());
	}

	private static InteractionResultHolder<ItemStack> onUseItem(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.pass(stack);
		}

		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(serverPlayer);
		RestrictionLevel restrictionLevel = getRestrictionLevel(serverPlayer, addiction);
		if (restrictionLevel == RestrictionLevel.NONE || !isCheckedItem(stack)) {
			return InteractionResultHolder.pass(stack);
		}

		if (!canCurrentlyConsume(serverPlayer, stack)) {
			return InteractionResultHolder.pass(stack);
		}

		if (isAllowedAtLevel(stack, restrictionLevel, BetelNutConfig.get())) {
			return InteractionResultHolder.pass(stack);
		}

		addiction.sendEatingRestrictionMessage(serverPlayer, restrictionLevel.blockMessage());
		return InteractionResultHolder.fail(stack);
	}

	private static boolean canCurrentlyConsume(ServerPlayer player, ItemStack stack) {
		if (stack.is(Items.MILK_BUCKET)
				|| stack.is(ModItemTags.WITHDRAWAL_STAGE2_ALLOWED_FOODS)
				|| stack.is(ModItemTags.WITHDRAWAL_STAGE3_ALLOWED_FOODS)) {
			return true;
		}

		FoodProperties food = stack.get(DataComponents.FOOD);
		return food != null && player.canEat(food.canAlwaysEat());
	}

	private static boolean isAllowedAtLevel(ItemStack stack, RestrictionLevel level, BetelNutConfig config) {
		return switch (level) {
			case NONE -> true;
			case SOFT_FOODS -> isAllowedAsSoftFood(stack, config);
			case ENCHANTED_GOLDEN_APPLE_ONLY -> isAllowedAsStrongRecoveryFood(stack);
			case BODY_REJECTS_FOOD -> stack.is(Items.ENCHANTED_GOLDEN_APPLE);
		};
	}

	private static boolean isAllowedAsSoftFood(ItemStack stack, BetelNutConfig config) {
		if (stack.is(Items.MILK_BUCKET)) {
			return config.allowMilkInStage2;
		}
		if (stack.is(Items.GOLDEN_APPLE) && !config.allowGoldenAppleInStage2) {
			return false;
		}
		return stack.is(ModItemTags.WITHDRAWAL_STAGE2_ALLOWED_FOODS);
	}

	private static boolean isAllowedAsStrongRecoveryFood(ItemStack stack) {
		if (stack.is(Items.GOLDEN_APPLE)) {
			return false;
		}
		return stack.is(Items.ENCHANTED_GOLDEN_APPLE)
				|| stack.is(ModItemTags.WITHDRAWAL_STAGE3_ALLOWED_FOODS);
	}

	private WithdrawalEatingRestrictions() {
	}
}
