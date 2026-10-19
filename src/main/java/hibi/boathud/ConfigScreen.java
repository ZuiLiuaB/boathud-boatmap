package hibi.boathud;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Wrapper screen shown from ModMenu: one-click config export/import via the system clipboard,
 * plus a button into the full cloth-config settings screen.
 */
public class ConfigScreen extends Screen {
	private final Screen parent;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("boathud.option.config_title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int bw = 160;
		int bh = 20;
		int cx = this.width / 2 - bw / 2;
		int y = this.height / 3;

		this.addRenderableWidget(Button.builder(Component.translatable("boathud.option.export_config"), b -> export())
			.bounds(cx, y, bw, bh)
			.build());
		y += bh + 4;

		this.addRenderableWidget(Button.builder(Component.translatable("boathud.option.import_config"), b -> importConfig())
			.bounds(cx, y, bw, bh)
			.build());
		y += bh + 4;

		this.addRenderableWidget(Button.builder(Component.translatable("boathud.option.open_settings"), b -> this.minecraft.setScreenAndShow(MenuInteg.buildClothScreen(this.parent)))
			.bounds(cx, y, bw, bh)
			.build());
	}

	private void export() {
		notify(Config.exportToClipboard() ? "boathud.config.exported" : "boathud.config.export_failed");
	}

	private void importConfig() {
		notify(Config.importFromClipboard() ? "boathud.config.imported" : "boathud.config.import_failed");
	}

	private void notify(String key) {
		if(this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.sendOverlayMessage(Component.translatable(key));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// Do NOT call extractBackground() here: in 26.2 the final wrapper
		// Screen.extractRenderStateWithTooltipAndSubtitles already extracts the screen background
		// (blur + menu dim) BEFORE delegating to this override. A second extractBackground()
		// would request a second blur in the same frame and crash the client with
		// "IllegalStateException: Can only blur once per frame".
		int titleY = this.height / 3 - 30;
		graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, titleY, 0xFFFFFFFF);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}
}
