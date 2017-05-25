package com.tterrag.blur.config;

import javax.annotation.Nonnull;

import com.tterrag.blur.Blur;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;

public class BlurConfigGui extends GuiConfig {

    public BlurConfigGui(GuiScreen parentScreen) {
        super(parentScreen, new ConfigElement(Blur.instance.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), Blur.MODID, false, false, Blur.MODID + ".config.title");
    }
    
    @Override
    public void initGui() {
        super.initGui();
        this.entryList = new GuiConfigEntries(this, mc) {
            @Override
            protected void drawContainerBackground(@Nonnull Tessellator tessellator) {
                if (mc.theWorld == null) {
                    super.drawContainerBackground(tessellator);
                }
            }
        };
    }
    
    @Override
    public void drawDefaultBackground() {
        drawWorldBackground(0);
    }
}
