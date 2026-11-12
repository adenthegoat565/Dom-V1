package com.domclient.client.mixin;

import com.domclient.client.module.ModuleManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "keyPress", at = @At("HEAD"))
	private void domclient$moduleBinds(long handle, int action, KeyEvent event, CallbackInfo ci) {
		if (action != GLFW.GLFW_PRESS || handle != minecraft.getWindow().handle()) {
			return;
		}
		if (minecraft.gui.screen() == null && minecraft.gui.overlay() == null && minecraft.player != null) {
			ModuleManager.onKeyPressed(event.key());
		}
	}
}
