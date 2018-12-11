package com.tterrag.blur.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.Identifier;

@Mixin(WorldRenderer.class)
public interface MixinWorldRenderer {
	
	@Invoker
	void invokeLoadShader(Identifier loc);

}
