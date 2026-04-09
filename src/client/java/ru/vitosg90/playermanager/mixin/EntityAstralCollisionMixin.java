package ru.vitosg90.playermanager.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vitosg90.playermanager.AstralManager;

@Mixin(Entity.class)
public abstract class EntityAstralCollisionMixin {
	@Inject(
		method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void playermanager$allowDoorPhasing(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
		Entity self = (Entity) (Object) this;
		if (AstralManager.shouldIgnoreDoorLikeCollisions(self, movement)) {
			cir.setReturnValue(movement);
		}
	}
}

