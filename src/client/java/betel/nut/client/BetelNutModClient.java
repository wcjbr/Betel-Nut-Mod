package betel.nut.client;

import betel.nut.block.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class BetelNutModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BETEL_PALM_LEAVES, RenderType.cutoutMipped());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BETEL_PALM_SAPLING, RenderType.cutout());
	}
}
