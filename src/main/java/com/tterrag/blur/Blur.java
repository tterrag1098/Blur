package com.tterrag.blur;

import static com.tterrag.blur.Blur.MODID;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;

import com.tterrag.blur.util.ShaderResourcePack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.ResourcePackInfoClient;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.PackCompatibility;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackInfo.Priority;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MODID)
public class Blur {
    
    public static final String MODID = "blur";
    public static final String MOD_NAME = "Blur";
    public static final String VERSION = "@VERSION@";
    
    public static Blur instance;
        
    private Field _listShaders;
    private long start;
    
    @Nonnull
    private ShaderResourcePack dummyPack = new ShaderResourcePack();
    
    public Blur() {
    	FMLJavaModLoadingContext.get().getModEventBus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BlurConfig.clientSpec);
        
//        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.RESOURCEPACK, () -> (mc, pack) -> dummyPack);
        ResourcePackList<ResourcePackInfoClient> rps = ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getInstance(), "field_110448_aq");
        rps.addPackFinder(new IPackFinder() {

            @Override
            public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> nameToPackMap, ResourcePackInfo.IFactory<T> packInfoFactory) {
                NativeImage img = null;
                try {
                    img = NativeImage.read(dummyPack.getRootResourceStream("pack.png"));
                } catch (IOException e) {
                    LogManager.getLogger().error("Could not load blur's pack.png", e);
                }
                @SuppressWarnings("unchecked")
                T var3 = (T) new ResourcePackInfoClient("blur", true, () -> dummyPack, new TextComponentString(dummyPack.getName()), new TextComponentString("Default shaders for Blur"),
                        PackCompatibility.COMPATIBLE, Priority.BOTTOM, true, img);
                if (var3 != null) {
                	nameToPackMap.put("blur", var3);
                }
            }
        });

        instance = this;
    }
    
    @SubscribeEvent
    public void preInit(FMLClientSetupEvent event) {
        // Add our dummy resourcepack
        ((SimpleReloadableResourceManager)Minecraft.getInstance().getResourceManager()).addReloadListener(dummyPack);   
    }
    
    @SubscribeEvent
    public void onGuiChange(GuiOpenEvent event) throws NoSuchFieldException, SecurityException {
        if (_listShaders == null) {
            _listShaders = ShaderGroup.class.getDeclaredField("listShaders");
            _listShaders.setAccessible(true);
        }
        if (Minecraft.getInstance().world != null) {
            GameRenderer er = Minecraft.getInstance().entityRenderer;
            boolean excluded = event.getGui() == null || BlurConfig.CLIENT.guiExclusions.get().contains(event.getGui().getClass().getName());
            if (!er.isShaderActive() && !excluded) {
                er.loadShader(new ResourceLocation("shaders/post/fade_in_blur.json"));
                start = System.currentTimeMillis();
            } else if (er.isShaderActive() && excluded) {
                er.stopUseShader();
            }
        }
    }
    
    private float getProgress() {
        return Math.min((System.currentTimeMillis() - start) / (float) BlurConfig.CLIENT.fadeTime.get(), 1);
    }
    
    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (event.phase == Phase.END && Minecraft.getInstance().currentScreen != null && Minecraft.getInstance().entityRenderer.isShaderActive()) {
            ShaderGroup sg = Minecraft.getInstance().entityRenderer.getShaderGroup();
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
                throw new RuntimeException(e);
            }
        }
    }
    
    public static int getBackgroundColor(boolean second) {
        int color = second ? BlurConfig.colorFirst : BlurConfig.colorSecond;
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
