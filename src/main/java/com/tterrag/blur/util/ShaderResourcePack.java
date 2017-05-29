package com.tterrag.blur.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Set;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import com.google.common.collect.ImmutableSet;
import com.tterrag.blur.Blur;

public class ShaderResourcePack implements IResourcePack {
	
	protected boolean validPath(ResourceLocation location) {
		return location.getResourceDomain().equals("minecraft") && location.getResourcePath().startsWith("shaders/");
	}

	@Override
	public InputStream getInputStream(ResourceLocation location) throws IOException {
		if (validPath(location)) {
			InputStream in = Blur.class.getResourceAsStream("/" + location.getResourcePath());
			StringBuilder data = new StringBuilder();
			Scanner scan = new Scanner(in);
			try {
				while (scan.hasNextLine()) {
					data.append(scan.nextLine().replaceAll("@radius@", Integer.toString(Blur.instance.radius))).append('\n');
				}
			} finally {
				scan.close();
			}
			
			return new ByteArrayInputStream(data.toString().getBytes());
		}
		return null;
	}

	@Override
	public boolean resourceExists(ResourceLocation location) {
		return validPath(location) && Blur.class.getResource("/" + location.getResourcePath()) != null;
	}

	@Override
	public Set<String> getResourceDomains() {
		return ImmutableSet.of("minecraft");
	}

	@Override
	public <T extends IMetadataSection> T getPackMetadata(
			MetadataSerializer metadataSerializer, String metadataSectionName)
			throws IOException {
		return null;
	}

	@Override
	public BufferedImage getPackImage() throws IOException {
		return null;
	}

	@Override
	public String getPackName() {
		return "Blur dummy resource pack";
	}

}
