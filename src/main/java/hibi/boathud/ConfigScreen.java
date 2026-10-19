package hibi.boathud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Wrapper screen shown from ModMenu: one-click config export/import via the system clipboard,
 * plus a button into the full cloth-config settings screen.
 */
public class ConfigScreen extends Screen {
	private final Screen parent;

	public ConfigScreen(Screen parent) {
		super(Text.translatable("boathud.option.config_title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int bw = 160;
		int bh = 20;
		int cx = this.width / 2 - bw / 2;
		int y = this.height / 3;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("boathud.option.export_config"), b -> export())
			.dimensions(cx, y, bw, bh)
			.build());
		y += bh + 4;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("boathud.option.import_config"), b -> importConfig())
			.dimensions(cx, y, bw, bh)
			.build());
		y += bh + 4;

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("boathud.option.open_settings"), b -> this.client.setScreen(MenuInteg.buildClothScreen(this.parent)))
			.dimensions(cx, y, bw, bh)
			.build());
	}

	private void export() {
		notify(Config.exportToClipboard() ? "boathud.config.exported" : "boathud.config.export_failed");
	}

	private void importConfig() {
		notify(Config.importFromClipboard() ? "boathud.config.imported" : "boathud.config.import_failed");
	}

	private void notify(String key) {
		if(this.client != null && this.client.player != null) {
			this.client.player.sendMessage(Text.translatable(key), true);
		}
	}

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, 0xC0101010);
		int titleY = this.height / 3 - 30;
		graphics.drawTextWithShadow(this.textRenderer, this.title, this.width / 2 - this.textRenderer.getWidth(this.title) / 2, titleY, 0xFFFFFFFF);
		super.render(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		this.client.setScreen(this.parent);
	}
}
