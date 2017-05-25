package com.tterrag.blurbg.config;

import com.tterrag.blurbg.BlurBG;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class BlurConfigGui extends GuiConfig {

    public BlurConfigGui(GuiScreen parentScreen) {
        super(parentScreen, new ConfigElement(BlurBG.instance.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), "blurbg", false, false, "blurbg.config.title");
    }
}
