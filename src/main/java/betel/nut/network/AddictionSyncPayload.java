package betel.nut.network;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record AddictionSyncPayload(
		int addictionValue,
		int addictionStage,
		int withdrawalSeverity,
		int nextWithdrawalTicks,
		int maxAddictionValue) implements CustomPacketPayload {
	public static final Type<AddictionSyncPayload> TYPE = new Type<>(BetelNutMod.id("addiction_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AddictionSyncPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, AddictionSyncPayload::addictionValue,
			ByteBufCodecs.VAR_INT, AddictionSyncPayload::addictionStage,
			ByteBufCodecs.VAR_INT, AddictionSyncPayload::withdrawalSeverity,
			ByteBufCodecs.VAR_INT, AddictionSyncPayload::nextWithdrawalTicks,
			ByteBufCodecs.VAR_INT, AddictionSyncPayload::maxAddictionValue,
			AddictionSyncPayload::new);

	public static void register() {
		PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
	}

	public static void send(ServerPlayer player) {
		if (player.connection == null || !ServerPlayNetworking.canSend(player, TYPE)) {
			return;
		}

		AddictionSyncPayload payload = fromPlayer(player);
		BetelNutMod.LOGGER.debug(
				"[BetelNut Debug] Sync to client: addictionValue={}, stage={}, withdrawalSeverity={}, nextWithdrawalTicks={}",
				payload.addictionValue(), payload.addictionStage(), payload.withdrawalSeverity(),
				payload.nextWithdrawalTicks());
		ServerPlayNetworking.send(player, payload);
	}

	public static AddictionSyncPayload fromPlayer(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem) {
			return new AddictionSyncPayload(0, 0, 0, -1, Math.max(1, config.maxAddictionValue));
		}

		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		int maxAddictionValue = Math.max(1, config.maxAddictionValue);
		int addictionValue = addiction.getAddictionValue();
		int addictionStage = addiction.getAddictionStage();
		int withdrawalSeverity = addiction.getWithdrawalSeverity();
		int nextWithdrawalTicks = addiction.getNextWithdrawalTicks(player);
		return new AddictionSyncPayload(addictionValue, addictionStage, withdrawalSeverity, nextWithdrawalTicks,
				maxAddictionValue);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
