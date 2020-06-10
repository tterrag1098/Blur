package com.tterrag.blur;

import static com.tterrag.blur.Blur.MODID;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import com.tterrag.blur.util.ShaderResourcePack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderDefault;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.PackCompatibility;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackInfo.Priority;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;
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
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
        	FMLJavaModLoadingContext.get().getModEventBus().register(this);
            MinecraftForge.EVENT_BUS.register(this);
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BlurConfig.clientSpec);
            
    //        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.RESOURCEPACK, () -> (mc, pack) -> dummyPack);
            ResourcePackList<ClientResourcePackInfo> rps = ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getInstance(), "field_110448_aq");
            rps.addPackFinder(new IPackFinder() {
    
                @Override
                public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> nameToPackMap, ResourcePackInfo.IFactory<T> packInfoFactory) {
                    NativeImage img = null;
                    try {
                        img = NativeImage.read(dummyPack.getRootResourceStream("pack.png"));
                    } catch (IOException e) {
                        LogManager.getLogger().error("Could not load blur's pack.png", e);
                    }
                    @SuppressWarnings({ "unchecked", "deprecation" })
                    T var3 = (T) new ClientResourcePackInfo("blur", true, () -> dummyPack, new StringTextComponent(dummyPack.getName()), new StringTextComponent("Default shaders for Blur"),
                            PackCompatibility.COMPATIBLE, Priority.BOTTOM, true, img);
                    if (var3 != null) {
                    	nameToPackMap.put("blur", var3);
                    }
                }
            });
        });

        instance = this;
    }
    
    @SubscribeEvent
    public void preInit(FMLClientSetupEvent event) {
        // Add our dummy resourcepack
        ((SimpleReloadableResourceManager)Minecraft.getInstance().getResourceManager()).addReloadListener(dummyPack);   
    }
    
    @SubscribeEvent
    public void onGuiChange(GuiOpenEvent event) throws SecurityException {
        if (_listShaders == null) {
            _listShaders = ObfuscationReflectionHelper.findField(ShaderGroup.class, "field_148031_d");
        }
        if (Minecraft.getInstance().world != null) {
            GameRenderer er = Minecraft.getInstance().gameRenderer;
            boolean excluded = event.getGui() == null || BlurConfig.CLIENT.guiExclusions.get().contains(event.getGui().getClass().getName());
            if (er.getShaderGroup() == null && !excluded) {
                er.loadShader(new ResourceLocation("shaders/post/fade_in_blur.json"));
                updateUniform("Radius", BlurConfig.CLIENT.radius.get());
                start = System.currentTimeMillis();
            } else if (er.getShaderGroup() != null && excluded) {
                er.stopUseShader();
            }
        }
    }
    
    private float getProgress() {
        return Math.min((System.currentTimeMillis() - start) / (float) BlurConfig.CLIENT.fadeTime.get(), 1);
    }
    
    private float prevProgress = -1;
    
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().currentScreen != null && Minecraft.getInstance().gameRenderer.getShaderGroup() != null) {
            float progress = getProgress();
            if (progress != prevProgress) {
                prevProgress = progress;
                updateUniform("Progress", progress);
            }
        }
    }
    
    public void updateUniform(String name, float value) {
        if (_listShaders == null) return;
        ShaderGroup sg = Minecraft.getInstance().gameRenderer.getShaderGroup();
        try {
            @SuppressWarnings("unchecked")
            List<Shader> shaders = (List<Shader>) _listShaders.get(sg);
            for (Shader s : shaders) {
                ShaderDefault su = s.getShaderManager().getShaderUniform(name);
                if (su != null) {
                    su.set(value);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
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
