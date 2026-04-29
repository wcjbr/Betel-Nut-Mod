package betel.nut.client.addiction;

import betel.nut.BetelNutMod;
import betel.nut.addiction.AddictionStageUtil;
import betel.nut.client.BetelNutClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class AddictionHudRenderer {
	private static final ResourceLocation ICON_TEXTURE = BetelNutMod.id("textures/gui/betel_nut_addiction_icon.png");
	private static final int ICON_SIZE = 16;
	private static final int BAR_WIDTH = 72;
	private static final int BAR_HEIGHT = 7;
	private static final int PANEL_HEIGHT = 29;
	private static final int WARNING_SECONDS = 60;
	private static final int DANGER_SECONDS = 20;
	private static final int TICKS_PER_SECOND = 20;
	private static final int PREVIEW_STAGE = 2;
	private static final float PREVIEW_PROGRESS = 0.5F;
	private static final int PREVIEW_REMAINING_TICKS = 90 * TICKS_PER_SECOND;

	public static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		BetelNutClientConfig config = BetelNutClientConfig.get();
		if (client.player == null || client.level == null || client.options.hideGui
				|| !config.hudEnabled
				|| !ClientAddictionData.shouldRenderHud()) {
			return;
		}

		HudPosition position = getConfiguredPosition(client, true);
		renderHud(graphics, position.x(), position.y(), 0.0F);
	}

	public static int getHudWidth() {
		Minecraft client = Minecraft.getInstance();
		return getHudWidth(client.font, createDisplayData());
	}

	public static int getHudHeight() {
		return PANEL_HEIGHT;
	}

	public static int getScaledHudWidth() {
		return Math.max(1, (int) Math.ceil(getHudWidth() * BetelNutClientConfig.get().hudScale));
	}

	public static int getScaledHudHeight() {
		return Math.max(1, (int) Math.ceil(getHudHeight() * BetelNutClientConfig.get().hudScale));
	}

	public static HudPosition getConfiguredPosition(Minecraft client, boolean saveClamp) {
		BetelNutClientConfig config = BetelNutClientConfig.get();
		int hudWidth = getHudWidth();
		int hudHeight = getHudHeight();
		float scale = (float) config.hudScale;
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int scaledHudWidth = scaled(hudWidth, scale);
		int scaledHudHeight = scaled(hudHeight, scale);
		int x;
		int y;

		if (config.hudUseCustomPosition) {
			x = config.hudX;
			y = config.hudY;
		} else {
			x = switch (config.hudAnchor) {
				case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - scaledHudWidth - config.hudOffsetX;
				case CENTER_TOP, CENTER_BOTTOM -> screenWidth / 2 - scaledHudWidth / 2 + config.hudOffsetX;
				case TOP_LEFT, BOTTOM_LEFT -> config.hudOffsetX;
			};
			y = switch (config.hudAnchor) {
				case BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BOTTOM -> screenHeight - scaledHudHeight - config.hudOffsetY;
				case TOP_LEFT, TOP_RIGHT, CENTER_TOP -> config.hudOffsetY;
			};
		}

		HudPosition clamped = clampPosition(x, y, screenWidth, screenHeight, scaledHudWidth, scaledHudHeight);
		if (saveClamp && config.hudUseCustomPosition && (clamped.x() != config.hudX || clamped.y() != config.hudY)) {
			config.setHudPosition(clamped.x(), clamped.y());
			BetelNutClientConfig.save();
		}
		return clamped;
	}

	public static HudPosition clampPosition(int x, int y, int screenWidth, int screenHeight) {
		return clampPosition(x, y, screenWidth, screenHeight, getScaledHudWidth(), getScaledHudHeight());
	}

	public static void renderHud(GuiGraphics graphics, int x, int y, float tickDelta) {
		Minecraft client = Minecraft.getInstance();
		BetelNutClientConfig config = BetelNutClientConfig.get();
		Font font = client.font;
		HudDisplayData data = createDisplayData();
		Component label = Component.translatable("betel_nut_mod.hud.addiction");
		Component stageText = Component.translatable(AddictionStageUtil.getStageTranslationKey(data.stage()));
		int labelWidth = font.width(label);
		int totalWidth = getHudWidth(font, data);
		Component countdown = data.remainingTicks() >= 0 ? countdownText(data.remainingTicks()) : null;
		float scale = (float) config.hudScale;

		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		renderPanel(graphics, font, label, stageText, labelWidth, totalWidth, data, countdown, client);
		graphics.pose().popPose();
	}

	private static void renderPanel(GuiGraphics graphics, Font font, Component label, Component stageText,
			int labelWidth, int totalWidth, HudDisplayData data, Component countdown, Minecraft client) {
		int x = 0;
		int y = 0;

		graphics.fill(x, y, x + totalWidth, y + PANEL_HEIGHT, 0x66000000);
		graphics.renderOutline(x, y, totalWidth, PANEL_HEIGHT, 0xAA2A2A2A);
		graphics.blit(ICON_TEXTURE, x + 5, y + 6, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

		int textY = y + 5;
		int labelX = x + 5 + ICON_SIZE + 5;
		graphics.drawString(font, label, labelX, textY, 0xFFE6E6E6, false);

		int barX = labelX + labelWidth + 5;
		int barY = y + 8;
		graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF262626);
		graphics.fill(barX + 1, barY + 1, barX + BAR_WIDTH - 1, barY + BAR_HEIGHT - 1, 0xFF101010);

		boolean danger = isDangerCountdown(data.stage(), data.remainingTicks());
		boolean flash = danger && (client.gui.getGuiTicks() / 8) % 2 == 0;
		int barColor = flash ? 0xFFFF6058 : AddictionStageUtil.getStageColor(data.stage());
		int filledWidth = Math.max(0, Math.min(BAR_WIDTH - 2,
				(int) ((BAR_WIDTH - 2) * data.progress())));
		if (filledWidth > 0) {
			graphics.fill(barX + 1, barY + 1, barX + 1 + filledWidth, barY + BAR_HEIGHT - 1, barColor);
		}

		int stageX = barX + BAR_WIDTH + 8;
		graphics.drawString(font, stageText, stageX, textY, barColor, false);

		if (countdown != null) {
			int countdownColor = countdownColor(data.stage(), data.remainingTicks(), flash);
			graphics.drawString(font, countdown, labelX, y + 18, countdownColor, false);
		}
	}

	private static int getHudWidth(Font font, HudDisplayData data) {
		Component label = Component.translatable("betel_nut_mod.hud.addiction");
		Component stageText = Component.translatable(AddictionStageUtil.getStageTranslationKey(data.stage()));
		int labelWidth = font.width(label);
		int stageWidth = font.width(stageText);
		int totalWidth = 5 + ICON_SIZE + 5 + labelWidth + 5 + BAR_WIDTH + 8 + stageWidth + 5;
		if (data.remainingTicks() >= 0) {
			Component countdown = countdownText(data.remainingTicks());
			totalWidth = Math.max(totalWidth, 5 + ICON_SIZE + 5 + font.width(countdown) + 5);
		}
		return totalWidth;
	}

	private static HudDisplayData createDisplayData() {
		if (ClientAddictionData.shouldRenderHud()) {
			return new HudDisplayData(ClientAddictionData.getAddictionStage(), ClientAddictionData.getStageProgress(),
					ClientAddictionData.getRemainingWithdrawalTicks());
		}
		return new HudDisplayData(PREVIEW_STAGE, PREVIEW_PROGRESS, PREVIEW_REMAINING_TICKS);
	}

	private static HudPosition clampPosition(int x, int y, int screenWidth, int screenHeight, int scaledHudWidth,
			int scaledHudHeight) {
		int maxX = Math.max(0, screenWidth - scaledHudWidth);
		int maxY = Math.max(0, screenHeight - scaledHudHeight);
		return new HudPosition(clamp(x, 0, maxX), clamp(y, 0, maxY));
	}

	private static int scaled(int value, float scale) {
		return Math.max(1, (int) Math.ceil(value * scale));
	}

	private static Component countdownText(int remainingTicks) {
		boolean attackSoon = remainingTicks >= 0 && remainingTicks <= DANGER_SECONDS * TICKS_PER_SECOND;
		String key = attackSoon ? "betel_nut_mod.hud.attack" : "betel_nut_mod.hud.withdrawal";
		return Component.translatable(key).append(Component.literal(": "
				+ AddictionStageUtil.formatTicksAsTime(remainingTicks)));
	}

	private static int countdownColor(int stage, int remainingTicks, boolean flash) {
		if (isDangerCountdown(stage, remainingTicks)) {
			return flash ? 0xFFFFE0E0 : 0xFFFF4E45;
		}
		if (stage >= 2 && remainingTicks >= 0 && remainingTicks <= WARNING_SECONDS * TICKS_PER_SECOND) {
			return 0xFFFFB347;
		}
		return 0xFFCFCFCF;
	}

	private static boolean isDangerCountdown(int stage, int remainingTicks) {
		return stage >= 3
				&& remainingTicks >= 0
				&& remainingTicks <= DANGER_SECONDS * TICKS_PER_SECOND;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private record HudDisplayData(int stage, float progress, int remainingTicks) {
	}

	public record HudPosition(int x, int y) {
	}

	private AddictionHudRenderer() {
	}
}
