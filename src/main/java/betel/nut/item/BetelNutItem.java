package betel.nut.item;

import java.util.List;

import betel.nut.BetelNutConfig;
import betel.nut.component.BetelNutEntityComponents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BetelNutItem extends Item {
	private final int addictionIncrease;
	private final List<EffectSpec> effects;

	public BetelNutItem(Properties properties, int addictionIncrease, List<EffectSpec> effects) {
		super(properties);
		this.addictionIncrease = addictionIncrease;
		this.effects = effects;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		boolean isEnderBetelNut = stack.is(ModItems.ENDER_BETEL_NUT);
		ItemStack result = super.finishUsingItem(stack, level, entity);

		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			for (EffectSpec effect : this.effects) {
				player.addEffect(new MobEffectInstance(effect.effect(), effect.durationTicks(), effect.amplifier()));
			}

			if (BetelNutConfig.get().enableAddictionSystem) {
				BetelNutEntityComponents.ADDICTION.get(player).eatBetelNut(player, this.addictionIncrease,
						level.getGameTime());
			}

			if (isEnderBetelNut) {
				EnderBetelTeleportHandler.tryTeleport(player);
			}
		}

		return result;
	}

	public record EffectSpec(Holder<MobEffect> effect, int durationTicks, int amplifier) {
	}
}
