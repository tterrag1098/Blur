package com.tterrag.blur.config;

import com.tterrag.blur.Blur;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class BlurConfigGui extends GuiConfig {

    public BlurConfigGui(GuiScreen parentScreen) {
        super(parentScreen, new ConfigElement(Blur.instance.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), Blur.MODID, false, false, Blur.MODID + ".config.title");
    }
}
