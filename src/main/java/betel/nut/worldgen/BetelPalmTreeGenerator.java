package betel.nut.worldgen;

import java.util.HashSet;
import java.util.Set;

import betel.nut.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BetelPalmTreeGenerator {
	public static final int DEFAULT_SAPLING_MIN_HEIGHT = 5;
	public static final int DEFAULT_SAPLING_MAX_HEIGHT = 7;
	public static final int DEFAULT_WORLDGEN_MIN_HEIGHT = 5;
	public static final int DEFAULT_WORLDGEN_MAX_HEIGHT = 8;

	public static boolean generate(LevelAccessor level, BlockPos basePos, RandomSource random, int minHeight,
			int maxHeight) {
		int height = randomHeight(random, minHeight, maxHeight);
		Set<BlockPos> leafPositions = createLeafPositions(basePos, height);

		if (!canGenerate(level, basePos, height, leafPositions)) {
			return false;
		}

		BlockState logState = ModBlocks.BETEL_PALM_LOG.defaultBlockState();
		BlockState leafState = ModBlocks.BETEL_PALM_LEAVES.defaultBlockState()
				.setValue(LeavesBlock.PERSISTENT, false);

		for (int y = 0; y < height; y++) {
			level.setBlock(basePos.above(y), logState, Block.UPDATE_ALL);
		}

		for (BlockPos leafPos : leafPositions) {
			level.setBlock(leafPos, leafState, Block.UPDATE_ALL);
		}

		return true;
	}

	private static int randomHeight(RandomSource random, int minHeight, int maxHeight) {
		int min = Math.max(1, minHeight);
		int max = Math.max(min, maxHeight);
		return min + random.nextInt(max - min + 1);
	}

	private static Set<BlockPos> createLeafPositions(BlockPos basePos, int height) {
		Set<BlockPos> leaves = new HashSet<>();
		BlockPos center = basePos.above(height);

		leaves.add(center);
		leaves.add(center.above());

		for (Direction direction : Direction.Plane.HORIZONTAL) {
			leaves.add(center.relative(direction));
			leaves.add(center.relative(direction, 2));
			leaves.add(center.above().relative(direction));
			leaves.add(center.below().relative(direction));

			if (height >= 7) {
				leaves.add(center.relative(direction, 3));
			}
		}

		leaves.add(center.offset(1, 0, 1));
		leaves.add(center.offset(1, 0, -1));
		leaves.add(center.offset(-1, 0, 1));
		leaves.add(center.offset(-1, 0, -1));

		return leaves;
	}

	private static boolean canGenerate(LevelAccessor level, BlockPos basePos, int height, Set<BlockPos> leaves) {
		if (!canWriteAt(level, basePos.below()) || !canWriteAt(level, basePos.above(height + 1))) {
			return false;
		}

		if (!isValidGround(level.getBlockState(basePos.below()))) {
			return false;
		}

		for (int y = 0; y < height; y++) {
			BlockPos trunkPos = basePos.above(y);
			if (!canWriteAt(level, trunkPos) || !canReplaceTreeBlock(level.getBlockState(trunkPos))) {
				return false;
			}
		}

		for (BlockPos leafPos : leaves) {
			if (!canWriteAt(level, leafPos) || !canReplaceTreeBlock(level.getBlockState(leafPos))) {
				return false;
			}
		}

		return true;
	}

	private static boolean canWriteAt(LevelAccessor level, BlockPos pos) {
		if (level.isOutsideBuildHeight(pos)) {
			return false;
		}

		if (level instanceof WorldGenLevel worldGenLevel) {
			return worldGenLevel.getLevel().getWorldBorder().isWithinBounds(pos);
		}

		if (level instanceof Level worldLevel) {
			return worldLevel.getWorldBorder().isWithinBounds(pos);
		}

		return true;
	}

	private static boolean isValidGround(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND);
	}

	private static boolean canReplaceTreeBlock(BlockState state) {
		return state.isAir()
				|| (state.getFluidState().isEmpty() && state.canBeReplaced())
				|| state.is(ModBlocks.BETEL_PALM_SAPLING)
				|| state.is(BlockTags.LEAVES)
				|| state.is(Blocks.VINE);
	}

	private BetelPalmTreeGenerator() {
	}
}
