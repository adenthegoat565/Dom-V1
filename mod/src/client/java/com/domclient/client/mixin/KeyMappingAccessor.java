package com.domclient.client.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
	@Accessor("clickCount")
	int domclient$getClickCount();

	@Accessor("clickCount")
	void domclient$setClickCount(int count);
}
