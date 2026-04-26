package betel.nut.worldgen;

import betel.nut.BetelNutMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModPlacedFeatures {
	public static final ResourceKey<PlacedFeature> BETEL_PALM_TREE = ResourceKey.create(Registries.PLACED_FEATURE,
			BetelNutMod.id("betel_palm_tree"));

	private ModPlacedFeatures() {
	}
}
