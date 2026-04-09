package hibi.boathud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.client.gui.screen.Screen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

/**
 * Main mod class that manages the initialization, ticking, and cleanup of the BoatHud mod.
 * Reference: Xaero Minimap's modular and lifecycle-aware design
 */
public class Common implements ClientModInitializer {

	// Instance-based fields to avoid static memory leaks
	private MinecraftClient client;
	private HudRenderer hudRenderer;
	private HudData hudData;
	private boolean ridingBoat;

	@Override
	public void onInitializeClient() {
		// Initialize client reference
		this.client = MinecraftClient.getInstance();
		
		// Initialize renderer and load config
		this.hudRenderer = new HudRenderer(client);
		Config.load();
		
		// Register tick event for data updates
		ClientTickEvents.END_WORLD_TICK.register(this::onWorldTick);
		
		// Register client stop event for cleanup
		ClientLifecycleEvents.CLIENT_STOPPING.register(this::onClientStopping);
		
		// Register command for opening settings
		ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);
	}
	
	/**
	 * Registers client commands for the mod.
	 */
	private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		// Register /boathudmap command to open settings
		dispatcher.register(ClientCommandManager.literal("boathudmap")
			.executes(context -> {
				// Open the config screen on the main thread
				MinecraftClient client = MinecraftClient.getInstance();
				client.execute(() -> {
					Screen configScreen = new MenuInteg().getModConfigScreenFactory().create(client.currentScreen);
					client.setScreen(configScreen);
				});
				return 1;
			})
		);
	}
	
	/**
	 * Handles world tick events to update HUD data.
	 */
	private void onWorldTick(ClientWorld clientWorld) {
		if(this.client == null || this.client.player == null) return;
		
		// Initialize hudData if not already done
		if(this.hudData == null) {
			this.hudData = new HudData();
		}
		
		// Check if player is riding a boat
		if(this.client.player.getVehicle() instanceof AbstractBoatEntity boat && boat.getFirstPassenger() == this.client.player) {
			this.hudData.update();
			this.ridingBoat = true;
		} else {
			this.ridingBoat = false;
		}
	}
	
	/**
	 * Handles client stopping event for proper cleanup.
	 */
	private void onClientStopping(MinecraftClient client) {
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
	
	public MinecraftClient getClient() {
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
