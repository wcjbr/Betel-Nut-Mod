package betel.nut.item;

import java.util.List;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	public static final Item RAW_BETEL_NUT = register("raw_betel_nut", new Item(new Item.Properties()));
	public static final Item BETEL_LEAF = register("betel_leaf", new Item(new Item.Properties()));

	public static final Item ROASTED_BETEL_NUT = register("roasted_betel_nut",
			new BetelNutItem(foodProperties(2, 0.3F), BetelNutConfig.ROASTED_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 200, 0))));

	public static final Item SPICY_BETEL_NUT = register("spicy_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.SPICY_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.FIRE_RESISTANCE, 2400, 0))));

	public static final Item SWEET_BETEL_NUT = register("sweet_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.SWEET_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 1800, 0))));

	public static final Item REFRESHING_BETEL_NUT = register("refreshing_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.REFRESHING_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.WATER_BREATHING, 2400, 0))));

	public static final Item NIGHT_BETEL_NUT = register("night_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.NIGHT_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.NIGHT_VISION, 2400, 0))));

	public static final Item ENERGIZING_BETEL_NUT = register("energizing_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.ENERGIZING_ADDICTION,
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 1200, 0),
							new BetelNutItem.EffectSpec(MobEffects.DIG_SPEED, 1200, 0))));

	public static final Item SYNTHETIC_WORLD_BETEL = register("synthetic_world_betel",
			new BetelNutItem(foodProperties(6, 0.8F).rarity(Rarity.EPIC).fireResistant(),
					BetelNutConfig.SYNTHETIC_WORLD_ADDICTION,
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 2400, 1),
							new BetelNutItem.EffectSpec(MobEffects.DIG_SPEED, 2400, 1),
							new BetelNutItem.EffectSpec(MobEffects.DAMAGE_RESISTANCE, 600, 0))));

	private static <T extends Item> T register(String path, T item) {
		return Registry.register(BuiltInRegistries.ITEM, BetelNutMod.id(path), item);
	}

	private static Item.Properties foodProperties(int nutrition, float saturationModifier) {
		FoodProperties food = new FoodProperties.Builder()
				.nutrition(nutrition)
				.saturationModifier(saturationModifier)
				.alwaysEdible()
				.build();
		return new Item.Properties().food(food);
	}

	public static void register() {
		BetelNutMod.LOGGER.info("Registering betel nut items");
	}

	private ModItems() {
	}
}
