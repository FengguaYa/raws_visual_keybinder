package com.gmail.rawlxxxviii.visual_keybinder.neoforge;

import com.gmail.rawlxxxviii.visual_keybinder.VisualKeybinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = VisualKeybinder.MOD_ID, dist = Dist.CLIENT)
public class VisualKeybinderNeoForge {

    public VisualKeybinderNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        VisualKeybinder.init(FMLPaths.GAMEDIR.get(), FMLPaths.CONFIGDIR.get());

        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onScreenInit);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        VisualKeybinder.registerKeyMappings(event::register, event::registerCategory);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        VisualKeybinder.onClientTick(Minecraft.getInstance());
    }

    private void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof ControlsScreen screen) {
            event.addListener(VisualKeybinder.createMenuButton(screen));
        }
    }
}
