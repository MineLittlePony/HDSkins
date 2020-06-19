package com.minelittlepony.hdskins.fabric;

import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.client.HDSkinsEvents;
import com.minelittlepony.hdskins.fabric.callback.ClientLogInCallback;
import com.minelittlepony.hdskins.fabric.callback.ScreenInitCallback;
import com.minelittlepony.hdskins.fabric.core.YarnFields;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class HDSkinsFabric extends HDSkins implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        init();

        ResourceManagerHelper mgr = ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES);
        mgr.registerReloadListener(new FabricResourceListener(new Identifier(MOD_ID, "resources"), resources));
        mgr.registerReloadListener(new FabricResourceListener(new Identifier(MOD_ID, "equipment_list"), equipmentList));

        HDSkinsEvents events = new HDSkinsEvents(this, new YarnFields());

        ClientTickCallback.EVENT.register(events::onTick);
        ScreenInitCallback.EVENT.register(events::onScreenInit);
        ClientLogInCallback.EVENT.register(events::onClientLogin);

        FabricLoader.getInstance().getEntrypoints("hdskins", ClientModInitializer.class).forEach(ClientModInitializer::onInitializeClient);
    }

    private static EntityRendererRegistry.Factory factory(Function<EntityRenderDispatcher, EntityRenderer<?>> factory) {
        return (renderer, context) -> factory.apply(renderer);
    }
}
