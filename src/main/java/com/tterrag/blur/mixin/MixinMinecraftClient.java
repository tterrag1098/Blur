package com.tterrag.blur.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tterrag.blur.Blur;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.resource.ReloadableResourceManager;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
	
	@Inject(method = "openGui(Lnet/minecraft/client/gui/Gui;)V", 
			at = @At(value = "FIELD",
					 target = "net/minecraft/client/MinecraftClient.currentGui : Lnet/minecraft/client/gui/Gui;",
					 opcode = Opcodes.PUTFIELD))
	public void onGuiOpen(Gui newGui, CallbackInfo info) {
		Blur.instance.onGuiChange(newGui);
	}

	@Inject(method = "method_1523(Z)V",
			at = @At(value = "INVOKE", 
					 target = "net/minecraft/client/toast/ToastManager.draw()V"),
			require = 1)
	public void onPostRenderTick(CallbackInfo info) {
		Blur.instance.onPostRenderTick();
	}
	
	@Inject(method = "init()V",
			at = @At(value = "FIELD",
					 target = "net/minecraft/client/MinecraftClient.resourceManager : Lnet/minecraft/resource/ReloadableResourceManager;",
					 opcode = Opcodes.PUTFIELD,
					 shift = Shift.AFTER))
	public void onResourceManagerAssign(CallbackInfo info) {
		Blur.instance.registerReloadListeners((ReloadableResourceManager) MinecraftClient.getInstance().getResourceManager());
	}
}
