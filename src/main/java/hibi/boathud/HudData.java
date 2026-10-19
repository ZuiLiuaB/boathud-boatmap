package hibi.boathud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;

public class HudData {
	/** The current speed in m/s. */
	public double speed;
	/** The current acceleration in g. */
	public double g;
	/** The current drift angle in degrees, the angle difference between the velocity and where the boat is facing. */
	public double driftAngle;

	/** The curerent ping of the player, just for bookkeeping. */
	public int ping;
	/** The name of the player. This is incompatible with mods that change which account you're logged in as. */
	public final String name;
	/** Controls whether or not the player's inputs are displayed on the HUD - if they are the ones driving it. */
	public boolean isDriver;

	private double oldSpeed;

	public HudData(){
		Minecraft client = Common.getInstance().getClient();
		this.name = client.player.getName().getString();
	}

	/** Updates the data. Assumes player is in a boat. Do not call unless you are absolutely sure the player is in a boat. */
	public void update() {
		Minecraft client = Common.getInstance().getClient();
		AbstractBoat boat = (AbstractBoat)client.player.getVehicle();
		// Ignore vertical speed
		Vec3 velocity = boat.getDeltaMovement().multiply(1, 0, 1);
		this.oldSpeed = this.speed;
		this.speed = velocity.length() * 20d; // Speed in Minecraft's engine is in meters/tick.

		// a̅•b̅ = |a̅||b̅|cos ϑ
		// ϑ = acos [(a̅•b̅) / (|a̅||b̅|)]
		this.driftAngle = Math.toDegrees(Math.acos(velocity.dot(boat.getLookAngle()) / velocity.length() * boat.getLookAngle().length()));
		if(Double.isNaN(this.driftAngle)) this.driftAngle = 0; // Div by 0

		// Trivial miscellanea
		this.g = (this.speed - this.oldSpeed) * 2.040816327d; // 20 tps / 9.8 m/s²
		// The PlayerInfo wrapper may exist but its internal list entry can still be
		// null during server-side client verification (e.g. Sonar) on multiplayer
		// servers. Calling getLatency() then throws NPE, so guard both cases.
		PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
		int ping = 0;
		if (info != null) {
			try {
				ping = info.getLatency();
			} catch (NullPointerException ignore) {
				ping = 0; // entry not populated yet, treat as unknown
			}
		}
		this.ping = ping;
		this.isDriver = boat.getControllingPassenger() == client.player;
	}
}
