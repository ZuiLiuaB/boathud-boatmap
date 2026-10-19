package hibi.boathud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

/**
 * Main mod class that manages the initialization, ticking, and cleanup of the BoatHud mod.
 * Reference: Xaero Minimap's modular and lifecycle-aware design
 */
public class Common implements ClientModInitializer {

	// Instance-based fields to avoid static memory leaks
	private Minecraft client;
	private HudRenderer hudRenderer;
	private HudData hudData;
	private boolean ridingBoat;

	@Override
	public void onInitializeClient() {
		// Initialize client reference
		this.client = Minecraft.getInstance();
		
		// Initialize renderer and load config
		this.hudRenderer = new HudRenderer(client);
		Config.load();
		
		// Register tick event for data updates
		ClientTickEvents.END_LEVEL_TICK.register(this::onWorldTick);

		// Register client tick event for the look-ahead camera
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		
		// Register client stop event for cleanup
		ClientLifecycleEvents.CLIENT_STOPPING.register(this::onClientStopping);
	}

	/**
	 * Handles client tick events for the speed-based look-ahead camera (Boat Cam style).
	 */
	private void onClientTick(Minecraft client) {
		if(client.player == null) return;
		if(!Config.cameraControl) return;
		if(client.player.getVehicle() instanceof AbstractBoat boat && boat.getControllingPassenger() == client.player) {
			CameraHandler.tick(boat, (net.minecraft.client.player.LocalPlayer) client.player);
		}
	}
	
	/**
	 * Handles world tick events to update HUD data.
	 */
	private void onWorldTick(ClientLevel clientWorld) {
		if(this.client == null || this.client.player == null) return;
		
		// Initialize hudData if not already done
		if(this.hudData == null) {
			this.hudData = new HudData();
		}
		
		// Check if player is riding a boat
		if(this.client.player.getVehicle() instanceof AbstractBoat boat && boat.getFirstPassenger() == this.client.player) {
			this.hudData.update();
			this.ridingBoat = true;
		} else {
			this.ridingBoat = false;
		}
	}
	
	/**
	 * Handles client stopping event for proper cleanup.
	 */
	private void onClientStopping(Minecraft client) {
		// Clean up resources to avoid memory leaks
		this.hudData = null;
		this.hudRenderer = null;
	}
	
	/**
	 * Getters for accessing mod state from other classes
	 */
	public boolean isRidingBoat() {
		return this.ridingBoat;
	}
	
	public void setRidingBoat(boolean ridingBoat) {
		this.ridingBoat = ridingBoat;
	}
	
	public HudData getHudData() {
		return this.hudData;
	}
	
	public void setHudData(HudData hudData) {
		this.hudData = hudData;
	}
	
	public HudRenderer getHudRenderer() {
		return this.hudRenderer;
	}
	
	public Minecraft getClient() {
		return this.client;
	}
	
	// Singleton instance for global access (reference: Xaero Minimap's singleton pattern)
	private static Common instance;
	
	/**
	 * Gets the singleton instance of the Common class.
	 */
	public static Common getInstance() {
		return instance;
	}
	
	/**
	 * Sets the singleton instance during initialization.
	 */
	public Common() {
		instance = this;
	}
}
