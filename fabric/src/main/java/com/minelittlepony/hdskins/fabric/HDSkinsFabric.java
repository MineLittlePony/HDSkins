package com.minelittlepony.hdskins.fabric;

import com.minelittlepony.common.event.ScreenInitCallback;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.dummy.DummyPlayer;
import com.minelittlepony.hdskins.client.dummy.DummyPlayerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class HDSkinsFabric extends HDSkins implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        config.load();
        ResourceManagerHelper mgr = ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES);
        mgr.registerReloadListener(new FabricResourceListener(new Identifier(MOD_ID, "resources"), resources));
        mgr.registerReloadListener(new FabricResourceListener(new Identifier(MOD_ID, "skin_servers"), skinServerList));
        mgr.registerReloadListener(new FabricResourceListener(new Identifier(MOD_ID, "equipment_list"), equipmentList));
        EntityRendererRegistry.INSTANCE.register(DummyPlayer.TYPE, factory(DummyPlayerRenderer::new));
        ScreenInitCallback.EVENT.register((screen, buttonList) -> onScreenInit(screen, buttonList::add));

        FabricLoader.getInstance().getEntrypoints("hdskins", ClientModInitializer.class).forEach(ClientModInitializer::onInitializeClient);
    }

    private static EntityRendererRegistry.Factory factory(Function<EntityRenderDispatcher, EntityRenderer<?>> factory) {
        return (renderer, context) -> factory.apply(renderer);
    }
}
