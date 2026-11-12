package com.domclient.client.mixin;

import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.render.FullbrightModule;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract", at = @At("TAIL"))
	private void domclient$fullbright(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
		FullbrightModule fullbright = ModuleManager.get(FullbrightModule.class);
		if (fullbright != null && fullbright.isEnabled() && renderState.needsUpdate) {
			renderState.nightVisionEffectIntensity = 1.0F;
			renderState.brightness = Math.max(renderState.brightness, 1.0F);
		}
	}
}
