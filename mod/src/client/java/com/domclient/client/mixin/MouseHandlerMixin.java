package com.domclient.client.mixin;

import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.render.ZoomModule;
import com.domclient.client.util.CpsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private double accumulatedDX;

	@Shadow
	private double accumulatedDY;

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void domclient$zoomScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		if (handle != minecraft.getWindow().handle() || minecraft.gui.screen() != null) {
			return;
		}
		ZoomModule zoom = ModuleManager.get(ZoomModule.class);
		if (zoom != null && zoom.onScroll(yoffset)) {
			ci.cancel();
		}
	}

	@Inject(method = "onButton", at = @At("HEAD"))
	private void domclient$countClicks(long handle, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
		if (action == 1 && handle == minecraft.getWindow().handle() && minecraft.gui.screen() == null) {
			CpsTracker.onClick(buttonInfo.button());
		}
	}

	@Inject(method = "turnPlayer", at = @At("HEAD"))
	private void domclient$zoomSensitivity(double mousea, CallbackInfo ci) {
		ZoomModule zoom = ModuleManager.get(ZoomModule.class);
		if (zoom != null) {
			double divisor = zoom.sensitivityDivisor();
			if (divisor > 1.0) {
				accumulatedDX /= divisor;
				accumulatedDY /= divisor;
			}
		}
	}
}
