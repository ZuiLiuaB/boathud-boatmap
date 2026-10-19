package hibi.boathud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import hibi.boathud.Common;
import hibi.boathud.HudData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

	@Shadow
	private ClientLevel level;

	@Inject(
		method = "handleSetEntityPassengersPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;)V",
		at = @At(
			value = "INVOKE_ASSIGN",
			target = "Lnet/minecraft/world/entity/Entity;getYRot()F",
			shift = At.Shift.AFTER
		)
	)
	private void checkBoatEntry(ClientboundSetPassengersPacket packet, CallbackInfo info) {
		// Only react when the passenger-carrying entity is actually a boat.
		// AbstractBoat covers Boat, Raft, ChestBoat and ChestRaft - `instanceof Boat` would miss the other three.
		Entity entity = level.getEntity(packet.getVehicle());
		if(!(entity instanceof AbstractBoat)) return;
		// Get Common instance
		Common common = Common.getInstance();
		if(common == null) return;
		common.setRidingBoat(true);
		common.setHudData(new HudData());
	}
}
