package com.minelittlepony.hdskins.client;

import com.google.common.cache.LoadingCache;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.client.forward.ForwardingTextureManager;
import com.minelittlepony.hdskins.client.gui.ButtonIconWidget;
import com.minelittlepony.hdskins.client.resources.HDPlayerSkinTexture;
import com.minelittlepony.hdskins.core.EventHookedPlayerMap;
import com.minelittlepony.hdskins.core.EventHookedSkinCache;
import com.minelittlepony.hdskins.core.PrivateFields;
import com.minelittlepony.hdskins.skins.SkinCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class HDSkinsEvents {
    private static final Logger logger = LogManager.getLogger();

    private final HDSkins skins;
    private final PrivateFields fields;

    private final List<PendingSkin> pendingSkins = new LinkedList<>();

    private boolean skinCacheLoaderReplaced;

    public HDSkinsEvents(HDSkins skins, PrivateFields fields) {
        this.skins = skins;
        this.fields = fields;
    }

    public void onClientLogin(ClientPlayNetworkHandler handler) {
        pendingSkins.clear();
        skins.resetCache(MinecraftClient.getInstance().getSessionService());

        replaceNetworkPlayerMap(handler);
    }

    public void onTick(MinecraftClient minecraft) {
        pendingSkins.removeIf(this::setPlayerSkin);

        if (!skinCacheLoaderReplaced) {
            skins.resetCache(MinecraftClient.getInstance().getSessionService());

            PlayerSkinProvider skins = minecraft.getSkinProvider();
            replaceSkinCacheLoader(skins);
            replaceSkinTextureManager(skins, this::mapPlayerSkinTextureToHD);
            skinCacheLoaderReplaced = true;
        }
    }

    private void addPendingSkin(PendingSkin pending) {
        this.pendingSkins.add(pending);
    }

    private AbstractTexture mapPlayerSkinTextureToHD(AbstractTexture texture) {
        if (texture instanceof PlayerSkinTexture && !(texture instanceof HDPlayerSkinTexture)) {
            texture = new HDPlayerSkinTexture((PlayerSkinTexture) texture);
        }
        return texture;
    }

    private void onPlayerAdd(PlayerListEntry player) {
        skins.getSkinCache().getPayload(player.getProfile())
                .thenAcceptAsync(payload -> loadSkins(player, payload.getTextures()), MinecraftClient.getInstance())
                .exceptionally(t -> {
                    logger.catching(t);
                    return null;
                });
    }

    private void loadSkins(PlayerListEntry player, Map<Type, MinecraftProfileTexture> textures) {
        logger.debug("Loaded skins for {}: {}", player.getProfile().getName(), textures);
        RenderSystem.recordRenderCall(() -> {
            PlayerSkinProvider.SkinTextureAvailableCallback callback = (type, location, texture) -> addPendingSkin(new PendingSkin(player, type, location, texture));
            for (Type textureType : Type.values()) {
                if (textures.containsKey(textureType)) {
                    loadSkin(textures.get(textureType), textureType, callback);
                }
            }
        });
    }

    public static void loadSkin(MinecraftProfileTexture profileTexture, Type textureType, @Nullable PlayerSkinProvider.SkinTextureAvailableCallback callback) {
        PlayerSkinProvider skins = MinecraftClient.getInstance().getSkinProvider();
        skins.loadSkin(profileTexture, textureType, (type, location, texture) -> {
            if (callback != null) {
                callback.onSkinTextureAvailable(textureType, location, profileTexture);
            }
        });
    }

    private boolean setPlayerSkin(PendingSkin skin) {
        if (skin.player.texturesLoaded) {
            skin.player.textures.put(skin.type, skin.identifier);
            if (skin.type == Type.SKIN) {
                skin.player.model = skin.texture.getMetadata("model");
                if (skin.player.model == null) {
                    skin.player.model = "default";
                }
            }
            return true;
        }
        return false;
    }

    public void onScreenInit(Screen screen, Consumer<AbstractButtonWidget> buttons) {
        /*
        if (screen instanceof TitleScreen) {
            MinecraftClient mc = MinecraftClient.getInstance();
            ButtonWidget button = new ButtonIconWidget(screen.width - 50, screen.height - 50,
                    String.format("minecraft:leather_leggings{display: {color: %d}}", 0x3c5dcb),
                    sender -> mc.openScreen(GuiSkins.create(screen, skins.getSkinCache().getServerList()))
            );

            button.y = screen.height - 50; // ModMenu;
            buttons.accept(button);
        }
        */
    }


    private void replaceNetworkPlayerMap(ClientPlayNetworkHandler handler) {
        // ClientPlayNetworkHandler;playerListEntries:Map<UUID, PlayerListEntry>
        Map<UUID, PlayerListEntry> entries = fields.playerListEntries.get(handler);
        entries = new EventHookedPlayerMap(entries, this::onPlayerAdd);
        fields.playerListEntries.set(handler, entries);
        logger.info("Replaced {} to detect when a player joins the server.", fields.playerListEntries);
    }

    private void replaceSkinCacheLoader(PlayerSkinProvider skins) {
        // replace the skin cache to make it return my skins instead
        LoadingCache<GameProfile, Map<Type, MinecraftProfileTexture>> vanillaCache = fields.skinCache.get(skins);
        vanillaCache = new EventHookedSkinCache(vanillaCache, this.skins::getSkinCache);
        fields.skinCache.set(skins, vanillaCache);
        logger.info("Replaced {} to handle non-player skins.", fields.skinCache);
    }

    private void replaceSkinTextureManager(PlayerSkinProvider skins, UnaryOperator<AbstractTexture> callback) {
        // Replacing the texture manager allows me to intercept loadTexture and
        // substitute the Texture object with one that supports HD Skins.
        TextureManager textures = fields.textureManager.get(skins);
        textures = new ForwardingTextureManager(textures) {
            @Override
            public void registerTexture(Identifier identifier, AbstractTexture abstractTexture) {
                super.registerTexture(identifier, callback.apply(abstractTexture));
            }
        };
        fields.textureManager.set(skins, textures);
        logger.info("Replaced {} to handle HD skins.", fields.textureManager);
    }

    protected static class PendingSkin {
        public final PlayerListEntry player;
        public final Type type;
        public final Identifier identifier;
        public final MinecraftProfileTexture texture;

        public PendingSkin(PlayerListEntry player, Type type, Identifier identifier, MinecraftProfileTexture texture) {
            this.player = player;
            this.type = type;
            this.identifier = identifier;
            this.texture = texture;
        }
    }
}
