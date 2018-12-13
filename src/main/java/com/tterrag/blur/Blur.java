package com.tterrag.blur;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tterrag.blur.mixin.MixinWorldRenderer;
import com.tterrag.blur.util.ReflectionHelper;
import com.tterrag.blur.util.ShaderResourcePack;

import net.fabricmc.api.ModInitializer;
import net.minecraft.class_279;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.Shader;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ingame.ChatGui;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.resource.ClientResourcePackContainer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.resource.ResourcePackContainer;
import net.minecraft.resource.ResourcePackContainer.Factory;
import net.minecraft.resource.ResourcePackContainer.SortingDirection;
import net.minecraft.resource.ResourcePackContainerManager;
import net.minecraft.resource.ResourcePackCreator;
import net.minecraft.text.StringTextComponent;
import net.minecraft.util.Identifier;

public class Blur implements ModInitializer {
    
    public static final String MODID = "blur";
    public static final String MOD_NAME = "Blur";
    public static final String VERSION = "@VERSION@";
    
    static class ConfigJson {
        String[] blurExclusions = new String[] { ChatGui.class.getName() };
        int fadeTimeMillis = 200;
        int radius = 8;
        String gradientStartColor = "75000000";
        String gradientEndColor = "75000000";
    }

    private Field _listShaders;
    private long start;

    public ConfigJson configs = new ConfigJson();
    public int colorFirst, colorSecond;
    
    @Nonnull
    private ShaderResourcePack dummyPack = new ShaderResourcePack();
    
    public static Blur instance;

    public Blur() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ResourcePackContainerManager<ClientResourcePackContainer> rps = ReflectionHelper.getValue(MinecraftClient.class, MinecraftClient.getInstance(), "field_1715", "resourcePackContainerManager");
        rps.addCreator(new ResourcePackCreator() {

            @Override
            public <T extends ResourcePackContainer> void registerContainer(Map<String, T> var1, Factory<T> factory) {
                NativeImage img = null;
                try {
                    img = NativeImage.fromInputStream(dummyPack.openRoot("pack.png"));
                } catch (IOException e) {
                    LogManager.getLogger().error("Could not load blur's pack.png", e);
                }
                @SuppressWarnings("unchecked")
                T var3 = (T) new ClientResourcePackContainer("blur", true, () -> dummyPack, new StringTextComponent(dummyPack.getName()), new StringTextComponent("Default shaders for Blur"),
                        ResourcePackCompatibility.COMPATIBLE, SortingDirection.BOTTOM, true, img);
                if (var3 != null) {
                    var1.put("blur", var3);
                }
            }
        });

        instance = this;
    }
    
    @Override
    public void onInitialize() {
        Path config = MinecraftClient.getInstance().runDirectory.toPath().resolve(Paths.get("config", "blur.cfg"));
        File configFile = config.toFile();
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                Files.write(config, new GsonBuilder().setPrettyPrinting().create().toJson(configs).getBytes(), StandardOpenOption.CREATE_NEW);
            } else {
                configs = new Gson().fromJson(new FileReader(configFile), ConfigJson.class);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        colorFirst = Integer.parseUnsignedInt(configs.gradientStartColor, 16);
        colorSecond = Integer.parseUnsignedInt(configs.gradientEndColor, 16);
    }

    public void registerReloadListeners(ReloadableResourceManager manager) {
        manager.addListener(dummyPack);
    }

    public void onGuiChange(Gui newGui) {
        if (_listShaders == null) {
            _listShaders = ReflectionHelper.getField(class_279.class, "field_1497");
        }
        if (MinecraftClient.getInstance().world != null) {
            WorldRenderer er = MinecraftClient.getInstance().worldRenderer;
            boolean excluded = newGui == null || ArrayUtils.contains(configs.blurExclusions, newGui.getClass().getName());
            if (!er.method_3175() && !excluded) {
                ((MixinWorldRenderer)er).invokeLoadShader(new Identifier("shaders/post/fade_in_blur.json"));
                start = System.currentTimeMillis();
            } else if (er.method_3175() && excluded) {
                er.method_3207();
            }
        }
    }
    
    public int getRadius() {
        return configs.radius;
    }
    
    private float getProgress() {
        return Math.min((System.currentTimeMillis() - start) / (float) configs.fadeTimeMillis, 1);
    }
    
    public void onPostRenderTick() {
        if (MinecraftClient.getInstance().currentGui != null && MinecraftClient.getInstance().worldRenderer.method_3175()) {
            class_279 sg = MinecraftClient.getInstance().worldRenderer.method_3183();
            try {
                @SuppressWarnings("unchecked")
                List<Shader> shaders = (List<Shader>) _listShaders.get(sg);
                for (Shader s : shaders) {
                    GlUniform su = s.method_1295().getUniformByName("Progress");
                    if (su != null) {
                        su.method_1251(getProgress());
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public int getBackgroundColor(boolean second) {
        int color = second ? colorSecond : colorFirst;
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
