package betel.nut.worldgen;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class ModWorldGeneration {
	public static void register() {
		try {
			ModConfiguredFeatures.register();

			if (!BetelNutConfig.get().enableBetelPalmWorldGeneration) {
				BetelNutMod.LOGGER.info("Betel palm world generation is disabled by config; skipping biome injection");
				return;
			}

			BiomeModifications.create(BetelNutMod.id("betel_palm_tree"))
					.add(ModificationPhase.ADDITIONS, ModWorldGeneration::shouldAddBetelPalm,
							ModWorldGeneration::addBetelPalmFeature);
			BetelNutMod.LOGGER.info("Betel palm world generation registered successfully");
		} catch (RuntimeException exception) {
			BetelNutMod.LOGGER.error("Failed to register betel palm world generation.", exception);
		}
	}

	private static void addBetelPalmFeature(BiomeModificationContext context) {
		context.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION,
				ModPlacedFeatures.BETEL_PALM_TREE);
	}

	private static boolean shouldAddBetelPalm(BiomeSelectionContext context) {
		if (!BiomeSelectors.foundInOverworld().test(context)) {
			return false;
		}

		BetelNutConfig config = BetelNutConfig.get();
		ResourceKey<Biome> biomeKey = context.getBiomeKey();

		if (config.betelPalmGenerateInJungle && isJungleBiome(biomeKey)) {
			return true;
		}

		if (config.betelPalmGenerateInBeach && biomeKey.equals(Biomes.BEACH)) {
			return true;
		}

		return config.betelPalmGenerateInMangroveSwamp && biomeKey.equals(Biomes.MANGROVE_SWAMP);
	}

	private static boolean isJungleBiome(ResourceKey<Biome> biomeKey) {
		return biomeKey.equals(Biomes.JUNGLE)
				|| biomeKey.equals(Biomes.SPARSE_JUNGLE)
				|| biomeKey.equals(Biomes.BAMBOO_JUNGLE);
	}

	private ModWorldGeneration() {
	}
}
