package com.domclient.client.mixin;

import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.render.ZoomModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void domclient$zoom(float partialTicks, CallbackInfoReturnable<Float> cir) {
		ZoomModule zoom = ModuleManager.get(ZoomModule.class);
		if (zoom == null) {
			return;
		}
		float divisor = zoom.fovDivisor();
		if (divisor > 1.0001f) {
			cir.setReturnValue(cir.getReturnValueF() / divisor);
		}
	}
}
