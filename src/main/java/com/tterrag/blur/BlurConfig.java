package com.tterrag.blur;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber(value = Dist.CLIENT, modid = Blur.MODID, bus = Bus.MOD)
public class BlurConfig {

	public static class Client {
		
		public final ConfigValue<List<? extends String>> guiExclusions;
		
		public final IntValue fadeTime;
		public final IntValue radius;
		ConfigValue<String> colorFirstRaw, colorSecondRaw;
		
		Client(ForgeConfigSpec.Builder builder) {
			guiExclusions = builder.comment("A list of classes to be excluded from the blur shader.")
					.defineList("guiExclusions", Lists.newArrayList(ChatScreen.class.getName()), o -> {
						try {
							return o instanceof String && Class.forName((String) o) != null;
						} catch (ClassNotFoundException e) {
							return false;
						}
					});
			
			fadeTime = builder.comment("The time it takes for the blur to fade in, in ms.")
					.defineInRange("fadeTime", 200, 0, 10 * 1000);
			
			radius = builder.comment("The radius of the blur effect. This controls how \"strong\" the blur is.")
					.defineInRange("radius", 12, 1, 100);
			
			Predicate<Object> hexValidator = o -> {
				if (o instanceof String) {
					try {
						Integer.parseInt((String) o, 16);
						return true;
					} catch (NumberFormatException e) {}
				}
				return false;
			};
			
			colorFirstRaw = builder.comment("The start color of the background gradient. Given in ARGB hex.")
					.define("gradientStartColor", "75000000", hexValidator);
			
			colorSecondRaw = builder.comment("The end color of the background gradient. Given in ARGB hex.")
					.define("gradientEndColor", "75000000", hexValidator);
		}
	}
	
	static final ForgeConfigSpec clientSpec;
	public static final Client CLIENT;
    static {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        clientSpec = specPair.getRight();
        CLIENT = specPair.getLeft();
    }
    
    public static int colorFirst, colorSecond;
    
    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {
        updateColors();
    }
    
    @SubscribeEvent
    public static void onReload(final ModConfig.Reloading configEvent) {
        updateColors();
    }
    
    private static void updateColors() {
        colorFirst = Integer.parseUnsignedInt(CLIENT.colorFirstRaw.get(), 16);
        colorSecond = Integer.parseUnsignedInt(CLIENT.colorSecondRaw.get(), 16);
        Minecraft.getInstance().execute(() -> Blur.instance.updateUniform("Radius", CLIENT.radius.get()));
    }
}
