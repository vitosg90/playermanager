package ru.vitosg90.playermanager.mixin;

import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vitosg90.playermanager.AstralManager;

@Mixin(net.minecraft.client.network.ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
	private void playermanager$blockMovePacketsInAstral(Packet<?> packet, CallbackInfo ci) {
		if (AstralManager.shouldCancelOutgoingPacket(packet)) {
			ci.cancel();
		}
	}
}

