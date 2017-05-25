package com.tterrag.blurbg;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Throwables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@Mod(modid = "blurbg", name = "BlurBG", version = "@VERSION@", acceptedMinecraftVersions = "[1.9, 1.12)")
public class BlurBG {
    
    private String[] blurExclusions;

    private Field _listShaders;
    private long start;
    private int fadeTime;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        
        Configuration config = new Configuration(new File(event.getModConfigurationDirectory(), "blurbg.cfg"));
        
        blurExclusions = config.getStringList("guiExclusions", Configuration.CATEGORY_GENERAL, new String[] {
                GuiChat.class.getName(),
        }, "A list of classes to be excluded from the blur shader.");
        
        fadeTime = config.getInt("fadeTime", Configuration.CATEGORY_GENERAL, 300, 0, Integer.MAX_VALUE, "The time it takes for the blur to fade in, in ms.");
        
        config.save();
    }
    
    @SuppressWarnings("null")
    @SubscribeEvent
    public void onGuiChange(GuiOpenEvent event) {
        if (_listShaders == null) {
            _listShaders = ReflectionHelper.findField(ShaderGroup.class, "field_148031_d", "listShaders");
        }
        if (Minecraft.getMinecraft().world != null) {
            EntityRenderer er = Minecraft.getMinecraft().entityRenderer;
            if (!er.isShaderActive() && event.getGui() != null && !ArrayUtils.contains(blurExclusions, event.getGui().getClass().getName())) {
                er.loadShader(new ResourceLocation("shaders/post/fade_in_blur.json"));
                start = System.currentTimeMillis();
            } else if (er.isShaderActive() && event.getGui() == null) {
                er.stopUseShader();
            }
        }
    }
    
    @SuppressWarnings("null")
    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END && Minecraft.getMinecraft().currentScreen != null && Minecraft.getMinecraft().entityRenderer.isShaderActive()) {
            ShaderGroup sg = Minecraft.getMinecraft().entityRenderer.getShaderGroup();
            try {
                @SuppressWarnings("unchecked")
                List<Shader> shaders = (List<Shader>) _listShaders.get(sg);
                for (Shader s : shaders) {
                    ShaderUniform su = s.getShaderManager().getShaderUniform("Progress");
                    if (su != null) {
                        su.set(Math.min((System.currentTimeMillis() - start) / (float) fadeTime, 1));
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                Throwables.propagate(e);
            }
        }
    }

}
