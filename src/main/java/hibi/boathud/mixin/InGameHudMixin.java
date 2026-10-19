package hibi.boathud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import hibi.boathud.Common;
import hibi.boathud.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(InGameHud.class)
public class InGameHudMixin {
	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/LayeredDrawer;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
			shift = At.Shift.AFTER
		)
	)
	public void render(DrawContext graphics, RenderTickCounter counter, CallbackInfo info) {
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
