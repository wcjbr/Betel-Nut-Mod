package betel.nut.worldgen;

import com.mojang.serialization.Codec;

import betel.nut.BetelNutConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BetelPalmTreeFeature extends Feature<NoneFeatureConfiguration> {
	public BetelPalmTreeFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BetelNutConfig config = BetelNutConfig.get();

		if (!config.enableBetelPalmWorldGeneration
				|| !level.getLevel().dimension().equals(Level.OVERWORLD)
				|| config.betelPalmTreesPerChunk <= 0
				|| config.betelPalmSpawnChance <= 0.0D) {
			return false;
		}

		boolean generated = false;
		BlockPos origin = context.origin();
		int chunkMinX = origin.getX() & ~15;
		int chunkMinZ = origin.getZ() & ~15;

		for (int attempt = 0; attempt < config.betelPalmTreesPerChunk; attempt++) {
			if (context.random().nextDouble() > config.betelPalmSpawnChance) {
				continue;
			}

			BlockPos candidate = attempt == 0
					? origin
					: surfacePos(level, chunkMinX + context.random().nextInt(16),
							chunkMinZ + context.random().nextInt(16));
			generated |= BetelPalmTreeGenerator.generate(level, candidate, context.random(),
					config.betelPalmMinHeight, config.betelPalmMaxHeight);
		}

		return generated;
	}

	private static BlockPos surfacePos(WorldGenLevel level, int x, int z) {
		return level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, new BlockPos(x, 0, z));
	}
}
