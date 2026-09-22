package com.minelittlepony.hdskins.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.gson.*;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.profile.ProfileUtils;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.ResourceUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;

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

public class SkinServerList implements ResourceManagerReloadListener {

    public static final Identifier SKIN_SERVERS = HDSkinsServer.id("skins/servers.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SkinServer.class, SkinServerSerializer.INSTANCE)
            .create();

    private List<Gateway> skinServers = new LinkedList<>();

    private long timestamp = System.currentTimeMillis();

    @Override
    public void onResourceManagerReload(ResourceManager mgr) {
        LOGGER.info("Loading skin servers");
        skinServers = ImmutableList.copyOf(ResourceUtil.streamAllResources(mgr, PackType.SERVER_DATA, SKIN_SERVERS).map(res -> {
            LOGGER.info("Found {} in {}", SKIN_SERVERS, res.sourcePackId());
            try (var reader = res.openAsReader()) {
                return GSON.fromJson(reader, SkinServerJson.class);
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("Unable to load resource '{}' from '{}'", SKIN_SERVERS, res.sourcePackId(), e);
            }
            return null;
        }).filter(Objects::nonNull).reduce(new LinkedList<Gateway>(), (gateways, res) -> {
            res.apply(gateways);
            return gateways;
        }, (_, b) -> b));
    }

    public Map<GameProfile, Map<SkinType, MinecraftProfileTexture>> fillProfiles(Collection<GameProfile> profiles) {
        Map<GameProfile, PartialTextures> result = new HashMap<>();
        List<GameProfile> profileList = profiles.stream().filter(profile -> {
            return getEmbeddedTextures(profile).findFirst().filter(textures -> {
                result.put(profile, new PartialTextures(Set.of(), textures));
                return true;
            }).isEmpty() && profile.id() != null;
        }).collect(Collectors.toList());
        Map<UUID, GameProfile> profileLookup = profiles.stream().collect(Collectors.toUnmodifiableMap(GameProfile::id, Function.identity()));
        Set<SkinType> requestedSkinTypes = SkinType.REGISTRY.stream().filter(SkinType::isKnown).collect(Collectors.toSet());

        for (Gateway gateway : skinServers) {
            try {
                if (!gateway.getServer().getFeatures().contains(Feature.SYNTHETIC)) {
                    loadSkins(gateway, profileList).forEach(textures -> {
                        GameProfile profile = profileLookup.get(textures.profileId());
                        if (profile == null) {
                            warnServerReturnedBadProfile(gateway, textures);
                        } else {
                            if (result.computeIfAbsent(profile,
                                    _ -> new PartialTextures(new HashSet<>(requestedSkinTypes), new HashMap<>()))
                                    .appendTextures(textures.textures())) {
                                profileList.remove(profile);
                            }
                        }
                    });

                    if (profileList.isEmpty()) {
                        break;
                    }
                }
            } catch (IOException | AuthenticationException e) {
                LOGGER.trace(e);
            }
        }

        return result.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue().textures())));

    }

    /**
     * Fills a profile and returns a new instance for use on the server.
     */
    public GameProfile fillProfileServerSide(GameProfile profile) {
        var requestedSkinTypes = SkinType.REGISTRY.stream().filter(SkinType::isKnown).collect(Collectors.toUnmodifiableSet());
        var data = new PartialTextures(new HashSet<>(requestedSkinTypes), new HashMap<>());

        for (Gateway gateway : skinServers) {
            if (gateway.getServer().getFeatures().contains(Feature.SYNTHETIC)) {
                continue;
            }
            try {
                var textures = gateway.getServer().loadSkins(profile);
                if (!textures.profileId().equals(profile.id())) {
                    warnServerReturnedBadProfile(gateway, textures);
                    continue;
                }
                if (data.appendTextures(textures.textures())) {
                    break;
                }
            } catch (IOException | AuthenticationException e) {
                LOGGER.trace(e);
            }
        }
        return writeEmbeddedTextures(profile, data.textures());
    }

    private static void warnServerReturnedBadProfile(Gateway gateway, TexturePayload payload) {
        LOGGER.warn("Server {} sent textures for unrequested profile {}. Ignoring.", gateway, payload.profileId());
    }

    private List<TexturePayload> loadSkins(Gateway gateway, List<GameProfile> profiles) throws IOException, AuthenticationException {
        return switch (profiles.size()) {
            case 0 -> List.of();
            case 1 -> List.of(gateway.getServer().loadSkins(profiles.get(0)));
            default -> gateway.getServer().loadSkins(profiles);
        };
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

    @Contract(pure=true)
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
