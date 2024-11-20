package com.minelittlepony.hdskins.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.gson.*;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.profile.ProfileUtils;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.ResourceUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkinServerList implements SynchronousResourceReloader, IdentifiableResourceReloadListener {

    private static final Identifier SKIN_SERVERS = HDSkinsServer.id("skins/servers.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SkinServer.class, SkinServerSerializer.INSTANCE)
            .create();

    private List<Gateway> skinServers = new LinkedList<>();

    private long timestamp = System.currentTimeMillis();

    @Override
    public Identifier getFabricId() {
        return SKIN_SERVERS;
    }

    @Override
    public void reload(ResourceManager mgr) {
        LOGGER.info("Loading skin servers");
        skinServers = ImmutableList.copyOf(ResourceUtil.streamAllResources(mgr, ResourceType.SERVER_DATA, SKIN_SERVERS).map(res -> {
            LOGGER.info("Found {} in {}", SKIN_SERVERS, res.getPackId());
            try (var reader = res.getReader()) {
                return GSON.fromJson(reader, SkinServerJson.class);
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("Unable to load resource '{}' from '{}'", SKIN_SERVERS, res.getPackId(), e);
            }
            return null;
        }).filter(Objects::nonNull).reduce(new LinkedList<Gateway>(), (gateways, res) -> {
            res.apply(gateways);
            return gateways;
        }, (a, b) -> b));
    }

    public Map<GameProfile, Map<SkinType, MinecraftProfileTexture>> fillProfiles(Collection<GameProfile> profiles) {
        Map<GameProfile, PartialTextures> result = new HashMap<>();
        List<GameProfile> profileList = profiles.stream().filter(profile -> {
            return getEmbeddedTextures(profile).findFirst().filter(textures -> {
                result.put(profile, new PartialTextures(Set.of(), textures));
                return true;
            }).isEmpty() && profile.getId() != null;
        }).collect(Collectors.toList());
        Map<UUID, GameProfile> profileLookup = profiles.stream().collect(Collectors.toUnmodifiableMap(GameProfile::getId, Function.identity()));
        Set<SkinType> requestedSkinTypes = SkinType.REGISTRY.stream().filter(SkinType::isKnown).collect(Collectors.toSet());

        for (Gateway gateway : skinServers) {
            try {
                if (!gateway.getServer().getFeatures().contains(Feature.SYNTHETIC)) {
                    gateway.getServer().loadSkins(profileList).forEach(textures -> {
                        GameProfile profile = profileLookup.get(textures.profileId());
                        if (profile == null) {
                            LOGGER.warn("Server {} sent textures for unrequested profile {}. Ignoring.", gateway.toString(), textures.profileId());
                        } else {
                            if (result.computeIfAbsent(profile,
                                    p -> new PartialTextures(new HashSet<>(requestedSkinTypes), new HashMap<>()))
                                    .appendTextures(textures.textures())) {
                                profileList.remove(profile);
                                writeEmbeddedTextures(profile, result.get(profile).textures());
                            }
                        }
                    });

                    if (profileList.isEmpty()) {
                        break;
                    }
                }
            } catch (IOException e) {
                LOGGER.trace(e);
            }
        }

        return result.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue().textures())));

    }

    public Map<SkinType, MinecraftProfileTexture> fillProfile(GameProfile profile) {
        return fillProfiles(Set.of(profile)).getOrDefault(profile, Map.of());
    }

    public Stream<Map<SkinType, MinecraftProfileTexture>> getEmbeddedTextures(GameProfile profile) {
        return ProfileUtils.readCustomBlob(profile, ProfileUtils.HD_TEXTURES_KEY, ProfileUtils.TextureData.class)
                .filter(i -> i.timestamp() == -1 || i.timestamp() >= timestamp)
                .map(ProfileUtils.TextureData::textures)
                .filter(this::isUrlPermitted);
    }

    public GameProfile writeEmbeddedTextures(GameProfile profile, Map<SkinType, MinecraftProfileTexture> textures) {
        return ProfileUtils.writeCustomBlob(profile, ProfileUtils.HD_TEXTURES_KEY, new ProfileUtils.TextureData(timestamp, textures));
    }

    public void invalidateProfiles() {
        timestamp = System.currentTimeMillis();
    }

    private boolean isUrlPermitted(Map<SkinType, MinecraftProfileTexture> blob) {
        return blob.values().stream().map(MinecraftProfileTexture::getUrl).allMatch(url -> {
            return skinServers.stream().anyMatch(s -> s.getServer().ownsUrl(url));
        });
    }

    public Iterator<Gateway> getCycler() {
        return Iterators.cycle(skinServers);
    }

    public Iterable<Gateway> getGateways() {
        return new ArrayList<>(skinServers);
    }

    private static <T> void addAllStart(List<T> list, List<T> toAdd) {
        list.addAll(0, toAdd);
    }

    record PartialTextures(Set<SkinType> requestedSkinTypes, Map<SkinType, MinecraftProfileTexture> textures) {
        boolean appendTextures(Map<SkinType, MinecraftProfileTexture> textures) {
            textures.forEach(this.textures::putIfAbsent);
            requestedSkinTypes.removeAll(textures.keySet());
            return requestedSkinTypes.isEmpty();
        }
    }

    private static class SkinServerJson {
        boolean overwrite = false;
        InsertType insert = InsertType.END;
        List<SkinServer> servers = Collections.emptyList();

        private void apply(List<Gateway> skinServers) {
            if (overwrite) {
                skinServers.clear();
            }
            LOGGER.info("Found {} servers", servers.size());
            insert.consumer.accept(skinServers, servers.stream().map(Gateway::new).toList());
        }
    }

    private enum InsertType {
        START(SkinServerList::addAllStart),
        END(List::addAll);

        final BiConsumer<List<Gateway>, List<Gateway>> consumer;

        InsertType(BiConsumer<List<Gateway>, List<Gateway>> consumer) {
            this.consumer = consumer;
        }
    }
}
