package hibi.boathud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import hibi.boathud.Common;
import hibi.boathud.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
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
		if(Config.enabled && common.isRidingBoat() && !(common.getClient().currentScreen instanceof ChatScreen)) {
			common.getHudRenderer().render(graphics, counter);
		} else if(Config.minimapEnabled && !(common.getClient().currentScreen instanceof ChatScreen)) {
			// Render only minimap if enabled and not in chat screen
			common.getHudRenderer().renderMinimap(graphics);
		}
	}
}
