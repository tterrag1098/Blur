package com.tterrag.blur.util;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import com.tterrag.blur.BlurConfig;

import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.resources.data.PackMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;

public class ShaderResourcePack implements IResourcePack, ISelectiveResourceReloadListener {

	private final ModFile blurModFile = FMLLoader.getLoadingModList().getModFileById("blur").getFile();
	
	protected boolean validPath(ResourceLocation location) {
		return location.getNamespace().equals("minecraft") && location.getPath().startsWith("shaders/");
	}
	
	private final Map<ResourceLocation, String> loadedData = new HashMap<>();

	@Override
	public InputStream getResourceStream(ResourcePackType type, ResourceLocation location) throws IOException {
        if (type == ResourcePackType.CLIENT_RESOURCES && validPath(location)) {
            String s = loadedData.computeIfAbsent(location, loc -> {
				InputStream in;
				try {
					in = Files.newInputStream(blurModFile.findResource(location.getPath()));
				} catch (IOException e) {
					throw new RuntimeException("Could not read " + location.getPath());
				}
				StringBuilder data = new StringBuilder();
                Scanner scan = new Scanner(in);
                try {
                    while (scan.hasNextLine()) {
                        data.append(scan.nextLine().replaceAll("@radius@", Integer.toString(BlurConfig.CLIENT.radius.get()))).append('\n');
                    }
                } finally {
                    scan.close();
                }
                return data.toString();
            });

            return new ByteArrayInputStream(s.getBytes());
        }
        throw new FileNotFoundException(location.toString());
	}

	@Override
	public boolean resourceExists(ResourcePackType type, ResourceLocation location) {
		return type == ResourcePackType.CLIENT_RESOURCES && validPath(location) && Files.exists(blurModFile.findResource(location.getPath()));
	}

	@Override
	public Set<String> getResourceNamespaces(ResourcePackType type) {
		return type == ResourcePackType.CLIENT_RESOURCES ? ImmutableSet.of("minecraft") : Collections.emptySet();
	}

	@SuppressWarnings({ "unchecked", "null" })
    @Override
	public <T> T getMetadata(IMetadataSectionSerializer<T> arg0) throws IOException {
	    if ("pack".equals(arg0.getSectionName())) {
	        return (T) new PackMetadataSection(new StringTextComponent("Blur's default shaders"), 3);
	    }
	    return null;
    }

	@Override
	public String getName() {
		return "Blur dummy resource pack";
	}
	
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
	    loadedData.clear();
	}

	@Override
	public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType arg0, String arg1, int arg2, Predicate<String> arg3) {
		return Collections.emptyList();
	}
	
	@Override
	public InputStream getRootResourceStream(String arg0) throws IOException {
        return Files.newInputStream(blurModFile.findResource("assets/blur/" + arg0));
	}
	
	@Override
	public void close() throws IOException {}
}
