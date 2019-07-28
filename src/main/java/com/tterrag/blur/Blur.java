package com.tterrag.blur;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tterrag.blur.mixin.MixinGameRenderer;
import com.tterrag.blur.util.ReflectionHelper;
import com.tterrag.blur.util.ShaderResourcePack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostProcessShader;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.resource.ClientResourcePackContainer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.resource.ResourcePackContainer;
import net.minecraft.resource.ResourcePackContainer.Factory;
import net.minecraft.resource.ResourcePackContainerManager;
import net.minecraft.resource.ResourcePackCreator;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

public class Blur implements ClientModInitializer {

    public static final String MODID = "blur";
    public static final String MOD_NAME = "Blur";
    public static final String VERSION = "@VERSION@";

    static class ConfigJson {
        String[] blurExclusions = new String[]{ ChatScreen.class.getName() };
        int fadeTimeMillis = 200;
        int radius = 8;
        String gradientStartColor = "75000000";
        String gradientEndColor = "75000000";
    }

    private Field _listShaders;
    private long start;

    public ConfigJson configs = new ConfigJson();
    public int colorFirst, colorSecond;

    private ShaderResourcePack dummyPack = new ShaderResourcePack();

    public static Blur instance;

    public Blur() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ResourcePackContainerManager<ClientResourcePackContainer> rps = ReflectionHelper.getValue(MinecraftClient.class, MinecraftClient.getInstance(), "field_1715", "resourcePackContainerManager");
        rps.addCreator(new ResourcePackCreator() {

            @Override
            public <T extends ResourcePackContainer> void registerContainer(Map<String, T> var1, Factory<T> factory) {
                NativeImage img = null;
                try {
                    img = NativeImage.read(dummyPack.openRoot("icon.png"));
                } catch (IOException e) {
                    LogManager.getLogger().error("Could not load blur's icon.png", e);
                }
                @SuppressWarnings("unchecked")
                T var3 = (T) new ClientResourcePackContainer("blur", true, () -> dummyPack, new LiteralText(dummyPack.getName()), new LiteralText("Default shaders for Blur"),
                        ResourcePackCompatibility.COMPATIBLE, ResourcePackContainer.InsertionPosition.BOTTOM, true, img);
                if (var3 != null) {
                    var1.put("blur", var3);
                }
            }
        });

        instance = this;
    }

    @Override
    public void onInitializeClient() {
        File configFile = new File(FabricLoader.getInstance().getConfigDirectory(), Blur.MODID + ".json");
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                Files.write(configFile.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(configs).getBytes(), StandardOpenOption.CREATE_NEW);
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
        manager.addPack(dummyPack);
    }

    public void onScreenChange(Screen newGui) {
        if (_listShaders == null) {
            _listShaders = ReflectionHelper.getField(ShaderEffect.class, "field_1497", "passes");
        }
        if (MinecraftClient.getInstance().world != null) {
            GameRenderer er = MinecraftClient.getInstance().gameRenderer;
            boolean excluded = newGui == null || ArrayUtils.contains(configs.blurExclusions, newGui.getClass().getName());
            if (!er.isShaderEnabled() && !excluded) {
                ((MixinGameRenderer) er).invokeLoadShader(new Identifier("shaders/post/fade_in_blur.json"));
                start = System.currentTimeMillis();
            } else if (er.isShaderEnabled() && excluded) {
                er.disableShader();
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
        if (MinecraftClient.getInstance().currentScreen != null && MinecraftClient.getInstance().gameRenderer.isShaderEnabled()) {
            ShaderEffect sg = MinecraftClient.getInstance().gameRenderer.getShader();
            try {
                @SuppressWarnings("unchecked")
                List<PostProcessShader> shaders = (List<PostProcessShader>) _listShaders.get(sg);
                for (PostProcessShader s : shaders) {
                    GlUniform su = s.getProgram().getUniformByName("Progress");
                    if (su != null) {
                        su.set(getProgress());
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
