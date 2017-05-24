package com.tterrag.blurbg;

import java.io.File;

import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "blurbg", name = "BlurBG", version = "@VERSION@", acceptedMinecraftVersions = "[1.9, 1.12)")
public class BlurBG {
    
    private String[] blurExclusions;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        
        Configuration config = new Configuration(new File(event.getModConfigurationDirectory(), "blurbg.cfg"));
        
        blurExclusions = config.getStringList("guiExclusions", Configuration.CATEGORY_GENERAL, new String[] {
                GuiChat.class.getName(),
        }, "A list of classes to be excluded from the blur shader.");
        
        config.save();
    }
    
    @SuppressWarnings("null")
    @SubscribeEvent
    public void onGuiChange(GuiOpenEvent event) {
        if (Minecraft.getMinecraft().world != null) {
            EntityRenderer er = Minecraft.getMinecraft().entityRenderer;
            if (!er.isShaderActive() && event.getGui() != null && !ArrayUtils.contains(blurExclusions, event.getGui().getClass().getName())) {
                er.loadShader(new ResourceLocation("blurbg", "shaders/post/blur.json"));
            } else if (er.isShaderActive() && event.getGui() == null) {
                er.stopUseShader();
            }
        }
    }

}
