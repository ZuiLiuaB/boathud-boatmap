package hibi.boathud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import hibi.boathud.Common;
import hibi.boathud.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;

@Mixin(Hud.class)
public class InGameHudMixin {
	@Inject(
		method = "extractHotbarAndDecorations",
		at = @At("TAIL")
	)
	public void render(GuiGraphicsExtractor graphics, DeltaTracker counter, CallbackInfo info) {
		// Get Common instance
		Common common = Common.getInstance();
		if(common == null) return;

		// Render main HUD if enabled and riding boat
		// (stays visible while the chat screen is open, same as the vanilla HUD)
		if(Config.enabled && common.isRidingBoat()) {
			common.getHudRenderer().render(graphics, counter);
		} else if(Config.minimapEnabled) {
			// Render only minimap if enabled
			common.getHudRenderer().renderMinimap(graphics);
		}
	}
}
