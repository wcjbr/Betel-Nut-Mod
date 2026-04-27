package betel.nut.mixin;

import betel.nut.event.BetelNutEvents;
import betel.nut.event.WithdrawalEatingRestrictions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void betelNutMod$blockRestrictedUse(Level level, Player player, InteractionHand hand,
			CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			ItemStack stack = (ItemStack) (Object) this;
			if (WithdrawalEatingRestrictions.shouldBlockUse(serverPlayer, stack, true)) {
				cir.setReturnValue(InteractionResultHolder.fail(stack));
			}
		}
	}

	@Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
	private void betelNutMod$handleFinishedUsingItem(Level level, LivingEntity entity,
			CallbackInfoReturnable<ItemStack> cir) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			ItemStack stack = (ItemStack) (Object) this;
			if (WithdrawalEatingRestrictions.shouldBlockUse(player, stack, true)) {
				cir.setReturnValue(stack);
				return;
			}

			BetelNutEvents.handleFinishedUsingItem(player, stack, level.getGameTime());
		}
	}
}
