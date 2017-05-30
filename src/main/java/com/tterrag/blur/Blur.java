package com.tterrag.blur;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Throwables;
import com.tterrag.blur.util.ShaderResourcePack;

import static com.tterrag.blur.Blur.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.client.util.JsonException;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

@Mod(modid = MODID, name = MOD_NAME, version = VERSION, acceptedMinecraftVersions = "[1.7, 1.8)", guiFactory = "com.tterrag.blur.config.BlurGuiFactory")
public class Blur {
    
    public static final String MODID = "blur";
    public static final String MOD_NAME = "Blur";
    public static final String VERSION = "@VERSION@";
    
    @Instance
    public static Blur instance;
    
    public Configuration config;
    
    private String[] blurExclusions;

    private Field _listShaders;
    private long start;
    private int fadeTime;
    
    public int radius; // Store default so we don't trigger an extra reload
    private int colorFirst, colorSecond;
    
    @Nonnull
    private ShaderResourcePack dummyPack = new ShaderResourcePack();
    
    @SuppressWarnings("unchecked")
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        
        // Add our dummy resourcepack
        ((List<IResourcePack>)ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "field_110449_ao", "defaultResourcePacks")).add(dummyPack);
        ((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(dummyPack);
        
        config = new Configuration(new File(event.getModConfigurationDirectory(), "blur.cfg"));
        saveConfig();
    }
    
    private void saveConfig() {
        
        blurExclusions = config.getStringList("guiExclusions", Configuration.CATEGORY_GENERAL, new String[] {
                GuiChat.class.getName(),
        }, "A list of classes to be excluded from the blur shader.");
        
        fadeTime = config.getInt("fadeTime", Configuration.CATEGORY_GENERAL, 200, 0, Integer.MAX_VALUE, "The time it takes for the blur to fade in, in ms.");
        
        int r = config.getInt("radius", Configuration.CATEGORY_GENERAL, 12, 1, 100, "The radius of the blur effect. This controls how \"strong\" the blur is.");
        if (r != radius) {
            radius = r;
            dummyPack.onResourceManagerReload(Minecraft.getMinecraft().getResourceManager());
            if (Minecraft.getMinecraft().theWorld != null) {
                Minecraft.getMinecraft().entityRenderer.deactivateShader();
            }
        }

        colorFirst = Integer.parseUnsignedInt(
                config.getString("gradientStartColor",  Configuration.CATEGORY_GENERAL, "75000000", "The start color of the background gradient. Given in ARGB hex."),
                16
        );
        
        colorSecond = Integer.parseUnsignedInt(
                config.getString("gradientEndColor",    Configuration.CATEGORY_GENERAL, "75000000", "The end color of the background gradient. Given in ARGB hex."),
                16
        );
        
        config.save();
    }
    
    @SubscribeEvent
    public void onConfigChanged(OnConfigChangedEvent event) {
        if (event.modID.equals(MODID)) {
            saveConfig();
        }
    }
    
    @SubscribeEvent
    public void onGuiChange(GuiOpenEvent event) throws JsonException {
        if (_listShaders == null) {
            _listShaders = ReflectionHelper.findField(ShaderGroup.class, "field_148031_d", "listShaders");
        }
        if (Minecraft.getMinecraft().theWorld != null && ShaderLinkHelper.getStaticShaderLinkHelper() != null) {
            EntityRenderer er = Minecraft.getMinecraft().entityRenderer;
            if (!er.isShaderActive() && event.gui != null && !ArrayUtils.contains(blurExclusions, event.gui.getClass().getName())) {
                Minecraft mc = Minecraft.getMinecraft();
                er.theShaderGroup = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), new ResourceLocation("shaders/post/fade_in_blur.json"));
                er.updateShaderGroupSize(mc.displayWidth, mc.displayHeight);
                start = System.currentTimeMillis();
            } else if (er.isShaderActive() && event.gui == null) {
                er.deactivateShader();
            }
        }
    }
    
    private float getProgress() {
        return Math.min((System.currentTimeMillis() - start) / (float) fadeTime, 1);
    }
    
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
                        su.set(getProgress());
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                Throwables.propagate(e);
            }
        }
    }
    
    public static int getBackgroundColor(boolean second) {
        int color = second ? instance.colorSecond : instance.colorFirst;
        int a = color >>> 24;
        int r = (color >> 16) & 0xFF;
        int b = (color >> 8) & 0xFF;
        int g = color & 0xFF;
        float prog = instance.getProgress();
        a *= prog;
        r *= prog;
        g *= prog;
        b *= prog;
        return a << 24 | r << 16 | b << 8 | g;
    }
}
