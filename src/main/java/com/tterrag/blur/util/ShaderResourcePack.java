package com.tterrag.blur.util;


import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import com.tterrag.blur.Blur;

import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceReloadListener;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

public class ShaderResourcePack implements ResourcePack, ResourceReloadListener {

    protected boolean validPath(Identifier location) {
        return location.getNamespace().equals("minecraft") && location.getPath().startsWith("shaders/");
    }

    private final Map<Identifier, String> loadedData = new HashMap<>();

    @Override
    public InputStream open(ResourceType type, Identifier location) throws IOException {
        if (type == ResourceType.CLIENT_RESOURCES && validPath(location)) {
            String s = loadedData.computeIfAbsent(location, loc -> {
                InputStream in = Blur.class.getResourceAsStream("/" + location.getPath());
                StringBuilder data = new StringBuilder();
                Scanner scan = new Scanner(in);
                try {
                    while (scan.hasNextLine()) {
                        data.append(scan.nextLine().replaceAll("@radius@", Integer.toString(Blur.instance.getRadius()))).append('\n');
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
    public boolean contains(ResourceType type, Identifier location) {
        return type == ResourceType.CLIENT_RESOURCES && validPath(location) && Blur.class.getResource("/" + location.getPath()) != null;
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        return ImmutableSet.of("minecraft");
    }

    @SuppressWarnings({"unchecked", "null"})
    @Override
    public <T> T parseMetadata(ResourceMetadataReader<T> var1) throws IOException {
        if ("pack".equals(var1.getKey())) {
            return (T) new PackResourceMetadata(new LiteralText("Blur's default shaders"), 4);
        }
        return null;
    }

    @Override
    public String getName() {
        return "Blur Shaders";
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer var1, ResourceManager var2, Profiler var3, Profiler var4, Executor var5, Executor var6) {
        return new CompletableFuture<>().thenRun(loadedData::clear);
    }

    @Override
    public void close() {
    }

    @Override
    public InputStream openRoot(String var1) throws IOException {
        return Blur.class.getResourceAsStream("/assets/blur/" + var1);
    }

    @Override
    public Collection<Identifier> findResources(ResourceType var1, String var2, int var3, Predicate<String> var4) {
        return Collections.emptyList();
    }
}
