package com.tterrag.blur.mixin;

import com.tterrag.blur.Blur;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.resource.ReloadableResourceManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "openScreen",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;",
                    opcode = Opcodes.PUTFIELD))
    public void onScreenOpen(Screen newScreen, CallbackInfo info) {
        Blur.instance.onScreenChange(newScreen);
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "net/minecraft/client/toast/ToastManager.draw()V"),
            require = 1)
    public void onPostRenderTick(CallbackInfo info) {
        Blur.instance.onPostRenderTick();
    }

    @Inject(method = "init()V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/MinecraftClient;resourceManager:Lnet/minecraft/resource/ReloadableResourceManager;",
                    opcode = Opcodes.PUTFIELD,
                    shift = Shift.AFTER))
    public void onResourceManagerAssign(CallbackInfo info) {
        Blur.instance.registerReloadListeners((ReloadableResourceManager) MinecraftClient.getInstance().getResourceManager());
    }
}
