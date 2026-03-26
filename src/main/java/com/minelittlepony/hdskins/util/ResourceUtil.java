package com.minelittlepony.hdskins.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public interface ResourceUtil {
    static Stream<Resource> streamAllResources(ResourceManager manager, PackType type, Identifier path) {
        return manager.listPacks().flatMap(pack -> {
            List<Resource> resources = new ArrayList<>();
            pack.listResources(type, path.getNamespace(), path.getPath(), (_, stream) -> {
                resources.add(new Resource(pack, stream));
            });
            return resources.stream();
        });
    }
}
