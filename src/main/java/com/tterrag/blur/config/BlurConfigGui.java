package com.tterrag.blur.config;

import javax.annotation.Nonnull;

import com.tterrag.blur.Blur;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;

public class BlurConfigGui extends GuiConfig {

    public BlurConfigGui(GuiScreen parentScreen) {
        super(parentScreen, new ConfigElement(Blur.instance.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), Blur.MODID, false, false, I18n.format(Blur.MODID + ".config.title"));
    }
    
    @Override
    public void initGui() {
        if (this.entryList == null || this.needsRefresh)
        {
            this.entryList = new GuiConfigEntries(this, mc) {
                @SuppressWarnings({ "unused", "null" })
                @Override
                protected void drawContainerBackground(@Nonnull Tessellator tessellator) {
                    if (mc.world == null) {
                        super.drawContainerBackground(tessellator);
                    }
                }
            };
            this.needsRefresh = false;
        }
        super.initGui();
    }
    
    @Override
    public void drawDefaultBackground() {
        drawWorldBackground(0);
    }
}
