package com.tterrag.blur.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.tterrag.blur.Blur;

import net.minecraft.client.gui.Gui;

@Mixin(Gui.class)
public class MixinGui {

    @ModifyConstant(
            method = "drawBackground(I)V",
            constant = @Constant(intValue = -1072689136))
    public int getFirstBackgroundColor(int color) {
        return Blur.instance.getBackgroundColor(false);
    }
    
    @ModifyConstant(
            method = "drawBackground(I)V",
            constant = @Constant(intValue = -804253680))
    public int getSecondBackgroundColor(int color) {
        return Blur.instance.getBackgroundColor(true);
    }
}
