package com.gmail.rawlxxxviii.visual_keybinder.fabric;

import com.gmail.rawlxxxviii.visual_keybinder.VisualKeybinder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;

public class VisualKeybinderFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        VisualKeybinder.init(FabricLoader.getInstance().getGameDir(), FabricLoader.getInstance().getConfigDir());

        VisualKeybinder.registerKeyMappings(KeyMappingHelper::registerKeyMapping);

        ClientTickEvents.END_CLIENT_TICK.register(VisualKeybinder::onClientTick);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof ControlsScreen) {
                Screens.getWidgets(screen).add(VisualKeybinder.createMenuButton(screen));
            }
        });
    }
}
