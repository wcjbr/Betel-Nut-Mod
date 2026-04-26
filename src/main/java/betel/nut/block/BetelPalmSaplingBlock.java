package betel.nut.block;

import java.util.Optional;

import betel.nut.worldgen.BetelPalmTreeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BetelPalmSaplingBlock extends SaplingBlock {
	private static final TreeGrower BETEL_PALM_GROWER = new TreeGrower("betel_palm", Optional.empty(),
			Optional.empty(), Optional.empty());

	public BetelPalmSaplingBlock(BlockBehaviour.Properties properties) {
		super(BETEL_PALM_GROWER, properties);
	}

	@Override
	public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		BetelPalmTreeGenerator.generate(level, pos, random, BetelPalmTreeGenerator.DEFAULT_SAPLING_MIN_HEIGHT,
				BetelPalmTreeGenerator.DEFAULT_SAPLING_MAX_HEIGHT);
	}
}
