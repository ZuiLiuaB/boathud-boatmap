package hibi.boathud;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Stores and updates HUD data for the boat HUD.
 * Reference: Xaero Minimap's data management design
 */
public class HudData {
	/** The current speed in m/s. */
	public double speed;
	/** The current acceleration in g. */
	public double g;
	/** The current drift angle in degrees, the angle difference between the velocity and where the boat is facing. */
	public double driftAngle;

	/** The current ping of the player, just for bookkeeping. */
	public int ping;
	/** The name of the player. This is incompatible with mods that change which account you're logged in as. */
	public final String name;
	/** Controls whether or not the player's inputs are displayed on the HUD - if they are the ones driving it. */
	public boolean isDriver;

	private double oldSpeed;
	private PlayerListEntry listEntry;

	/**
	 * Initializes HUD data.
	 */
	public HudData(){
		// Get client instance from Common singleton using getClient() method
		var client = Common.getInstance().getClient();
		if(client == null || client.player == null) {
			this.name = "Player";
			this.listEntry = null;
			return;
		}
		this.name = client.player.getName().getString();
		this.listEntry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
	}

	/** 
	 * Updates the data. 
	 * Assumes player is in a boat. Do not call unless you are absolutely sure the player is in a boat. 
	 */
	public void update() {
		// Get client instance from Common singleton using getClient() method
		var client = Common.getInstance().getClient();
		if(client == null || client.player == null) return;
		
		AbstractBoatEntity boat = (AbstractBoatEntity)client.player.getVehicle();
		// Ignore vertical speed
		Vec3d velocity = boat.getVelocity().multiply(1, 0, 1);
		this.oldSpeed = this.speed;
		this.speed = velocity.length() * 20d; // Speed in Minecraft's engine is in meters/tick.

		// a̅•b̅ = |a̅||b̅|cos ϑ
		// ϑ = acos [(a̅•b̅) / (|a̅||b̅|)]
		this.driftAngle = Math.toDegrees(Math.acos(velocity.dotProduct(boat.getRotationVector()) / velocity.length() * boat.getRotationVector().length()));
		if(Double.isNaN(this.driftAngle)) this.driftAngle = 0; // Div by 0

		// Trivial miscellanea
		this.g = (this.speed - this.oldSpeed) * 2.040816327d; // 20 tps / 9.8 m/s²
		if(this.listEntry != null) {
			this.ping = this.listEntry.getLatency();
		}
		this.isDriver = boat.getControllingPassenger() == client.player;
	}
}
