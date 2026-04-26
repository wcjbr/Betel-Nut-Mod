package betel.nut.mixin;

import betel.nut.event.BetelNutEvents;
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
	@Inject(method = "finishUsingItem", at = @At("HEAD"))
	private void betelNutMod$handleFinishedUsingItem(Level level, LivingEntity entity,
			CallbackInfoReturnable<ItemStack> cir) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			BetelNutEvents.handleFinishedUsingItem(player, (ItemStack) (Object) this, level.getGameTime());
		}
	}
}
