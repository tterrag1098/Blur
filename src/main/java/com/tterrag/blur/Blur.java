package com.tterrag.blur;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Throwables;
import com.tterrag.blur.mixin.MixinWorldRenderer;
import com.tterrag.blur.util.ShaderResourcePack;

import net.fabricmc.api.ModInitializer;
import net.minecraft.class_279;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.Shader;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.resource.ClientResourcePackContainer;
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

    private String[] blurExclusions = new String[] {
    	"net.minecraft.client.gui.ingame.ChatGui"
    };

    private Field _listShaders;
    private long start;
    private int fadeTime = 250;
    
    public int radius = 10; // Store default so we don't trigger an extra reload
    public int colorFirst, colorSecond;
    
    @Nonnull
    private ShaderResourcePack dummyPack = new ShaderResourcePack();
    
    public static Blur instance;
    
    public Blur() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
    	Field _rps;
    	try {
    		_rps = MinecraftClient.class.getDeclaredField("field_1715");
    	} catch (NoSuchFieldException e) {
    		_rps = MinecraftClient.class.getDeclaredField("resourcePackContainerManager");
    	}
    	_rps.setAccessible(true);
    	ResourcePackContainerManager<ClientResourcePackContainer> rps = (ResourcePackContainerManager<ClientResourcePackContainer>)_rps.get(MinecraftClient.getInstance());
    	rps.addCreator(new ResourcePackCreator() {
    		
    		@Override
    		public <T extends ResourcePackContainer> void registerContainer(Map<String, T> var1, Factory<T> factory) {

    	      T var3 = (T) new ClientResourcePackContainer("blur", true, () -> dummyPack, new StringTextComponent(dummyPack.getName()), new StringTextComponent(dummyPack.getName()), ResourcePackCompatibility.COMPATIBLE, SortingDirection.BOTTOM, true, null);
    	      if (var3 != null) {
    	         var1.put("blur", var3);
    	      }
    		}
    	});
      
    	instance = this; 
    }

    @Override
    public void onInitialize() {
        // Add our dummy resourcepack
//        ((ReloadableResourceManager)MinecraftClient.getInstance().getResourceManager()).addListener(dummyPack);
        
//        config = new Configuration(new File(event.getModConfigurationDirectory(), "blur.cfg"));
//        saveConfig();
    }
    
/*    private void saveConfig() {
        
        blurExclusions = config.getStringList("guiExclusions", Configuration.CATEGORY_GENERAL, new String[] {
                GuiChat.class.getName(),
        }, "A list of classes to be excluded from the blur shader.");
        
        fadeTime = config.getInt("fadeTime", Configuration.CATEGORY_GENERAL, 200, 0, Integer.MAX_VALUE, "The time it takes for the blur to fade in, in ms.");
        
        int r = config.getInt("radius", Configuration.CATEGORY_GENERAL, 12, 1, 100, "The radius of the blur effect. This controls how \"strong\" the blur is.");
        if (r != radius) {
            radius = r;
            dummyPack.onResourceManagerReload(Minecraft.getMinecraft().getResourceManager());
            if (Minecraft.getMinecraft().world != null) {
                Minecraft.getMinecraft().entityRenderer.stopUseShader();
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
    }*/
    
/*    @SubscribeEvent
    public void onConfigChanged(OnConfigChangedEvent event) {
        if (event.getModID().equals(MODID)) {
            saveConfig();
        }
    }
    */
    public void onGuiChange(Gui newGui) {
        if (_listShaders == null) {
            try {
				_listShaders = class_279.class.getDeclaredField("field_1497");
				_listShaders.setAccessible(true);
			} catch (NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(e);
			}
        }
        if (MinecraftClient.getInstance().world != null) {
            WorldRenderer er = MinecraftClient.getInstance().worldRenderer;
            boolean excluded = newGui == null || ArrayUtils.contains(blurExclusions, newGui.getClass().getName());
            if (!er.method_3175() && !excluded) {
                ((MixinWorldRenderer)er).invokeLoadShader(new Identifier("shaders/post/fade_in_blur.json"));
                start = System.currentTimeMillis();
            } else if (er.method_3175() && excluded) {
                er.method_3207();
            }
        }
    }
    
    private float getProgress() {
        return Math.min((System.currentTimeMillis() - start) / (float) fadeTime, 1);
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
                Throwables.propagate(e);
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
