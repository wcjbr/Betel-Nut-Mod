package betel.nut.client.addiction;

import betel.nut.BetelNutMod;
import betel.nut.addiction.AddictionStageUtil;
import betel.nut.network.AddictionSyncPayload;
import net.minecraft.client.Minecraft;

public final class ClientAddictionData {
	private static boolean initialized;
	private static int addictionValue;
	private static int addictionStage;
	private static int withdrawalSeverity;
	private static int nextWithdrawalTicks = -1;
	private static int maxAddictionValue = 100;
	private static long receivedAtGameTime;

	public static boolean update(AddictionSyncPayload payload) {
		int previousStage = addictionStage;
		boolean hadPreviousSync = initialized;

		addictionValue = Math.max(0, payload.addictionValue());
		addictionStage = AddictionStageUtil.getWithdrawalSeverity(payload.addictionStage());
		withdrawalSeverity = AddictionStageUtil.getWithdrawalSeverity(payload.withdrawalSeverity());
		nextWithdrawalTicks = payload.nextWithdrawalTicks();
		maxAddictionValue = Math.max(1, payload.maxAddictionValue());
		receivedAtGameTime = currentGameTime();
		initialized = true;

		BetelNutMod.LOGGER.debug(
				"[BetelNut Debug] Client received: addictionValue={}, stage={}, withdrawalSeverity={}, nextWithdrawalTicks={}",
				addictionValue, addictionStage, withdrawalSeverity, nextWithdrawalTicks);

		return hadPreviousSync && addictionStage > previousStage && addictionStage > 0;
	}

	public static void reset() {
		initialized = false;
		addictionValue = 0;
		addictionStage = 0;
		withdrawalSeverity = 0;
		nextWithdrawalTicks = -1;
		maxAddictionValue = 100;
		receivedAtGameTime = 0L;
	}

	public static boolean hasSyncedData() {
		return initialized;
	}

	public static boolean shouldRenderHud() {
		return initialized && (addictionValue > 0 || addictionStage > 0);
	}

	public static int getAddictionValue() {
		return addictionValue;
	}

	public static int getAddictionStage() {
		return addictionStage;
	}

	public static int getWithdrawalSeverity() {
		return withdrawalSeverity;
	}

	public static int getMaxAddictionValue() {
		return maxAddictionValue;
	}

	public static float getStageProgress() {
		return AddictionStageUtil.getStageProgress(addictionValue, maxAddictionValue);
	}

	public static boolean wouldReachNextStage(int addictionIncrease) {
		return AddictionStageUtil.wouldReachNextStage(addictionValue, addictionIncrease, maxAddictionValue);
	}

	public static int getRemainingWithdrawalTicks() {
		if (nextWithdrawalTicks < 0) {
			return -1;
		}

		long elapsed = Math.max(0L, currentGameTime() - receivedAtGameTime);
		return (int) Math.max(0L, nextWithdrawalTicks - elapsed);
	}

	public static boolean isCloseToNextStage() {
		return addictionStage < 5 && getStageProgress() >= 0.85F;
	}

	private static long currentGameTime() {
		Minecraft client = Minecraft.getInstance();
		return client.level == null ? 0L : client.level.getGameTime();
	}

	private ClientAddictionData() {
	}
}
