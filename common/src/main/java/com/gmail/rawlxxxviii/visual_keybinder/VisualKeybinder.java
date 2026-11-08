package com.gmail.rawlxxxviii.visual_keybinder;

import com.gmail.rawlxxxviii.visual_keybinder.config.ClientConfig;
import com.gmail.rawlxxxviii.visual_keybinder.screen.AlternativeKeybindScreen;
import com.gmail.rawlxxxviii.visual_keybinder.util.FileUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Loader independent entry point. Both the Fabric and the NeoForge entry points
 * call into this class during client initialisation.
 */
public final class VisualKeybinder {

    public static final String MOD_ID = "visual_keybinder";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String KEY_CATEGORY = "key.category." + MOD_ID + ".main";

    public static KeyMapping.Category CATEGORY;
    public static KeyMapping OPEN_SCREEN;
    public static final KeyMapping[] PRESET_KEYS = new KeyMapping[5];

    private static Path gameDir;
    private static boolean initialized;

    private VisualKeybinder() {
    }

    public static void init(Path gameDir, Path configDir) {
        VisualKeybinder.gameDir = gameDir;
        ClientConfig.load(configDir);
    }

    public static Path getGameDir() {
        if (gameDir == null) {
            throw new IllegalStateException("Visual Keybinder was not initialised before the game directory was requested");
        }
        return gameDir;
    }

    public static void registerKeyMappings(Consumer<KeyMapping> registrar) {
        registerKeyMappings(registrar, category -> {
        });
    }

    public static void registerKeyMappings(Consumer<KeyMapping> registrar, Consumer<KeyMapping.Category> categoryRegistrar) {
        if (CATEGORY == null) {
            CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        }
        categoryRegistrar.accept(CATEGORY);
        OPEN_SCREEN = register(registrar, "open_screen", GLFW.GLFW_KEY_F10);
        PRESET_KEYS[0] = register(registrar, "switch_to_preset_1", GLFW.GLFW_KEY_UNKNOWN);
        PRESET_KEYS[1] = register(registrar, "switch_to_preset_2", GLFW.GLFW_KEY_UNKNOWN);
        PRESET_KEYS[2] = register(registrar, "switch_to_preset_3", GLFW.GLFW_KEY_UNKNOWN);
        PRESET_KEYS[3] = register(registrar, "switch_to_preset_4", GLFW.GLFW_KEY_UNKNOWN);
        PRESET_KEYS[4] = register(registrar, "switch_to_preset_5", GLFW.GLFW_KEY_UNKNOWN);
    }

    private static KeyMapping register(Consumer<KeyMapping> registrar, String name, int defaultKey) {
        KeyMapping keyMapping = new KeyMapping("key." + MOD_ID + "." + name, defaultKey, CATEGORY);
        registrar.accept(keyMapping);
        return keyMapping;
    }

    public static void onClientTick(Minecraft minecraft) {
        if (!initialized) {
            ensureInitialized(minecraft);
        }
        if (minecraft.player == null) {
            return;
        }

        boolean opened = false;
        while (OPEN_SCREEN != null && OPEN_SCREEN.consumeClick() && !opened) {
            opened = true;
        }
        if (opened) {
            minecraft.setScreen(new AlternativeKeybindScreen(minecraft.screen, minecraft.options));
        }

        for (int i = 0; i < PRESET_KEYS.length; i++) {
            KeyMapping keyMapping = PRESET_KEYS[i];
            if (keyMapping != null && keyMapping.consumeClick()) {
                loadPreset(i);
            }
        }
    }

    private static void ensureInitialized(Minecraft minecraft) {
        try {
            if (FileUtil.getLayoutFileList().isEmpty()) {
                FileUtil.createDefaultLayouts();
            }
            if (!FileUtil.isInitializedFileCreated()) {
                FileUtil.loadInitialPreset(minecraft.options);
                FileUtil.createInitializedFile();
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not initialise Visual Keybinder data files", exception);
        }
        initialized = true;
    }

    public static void loadPreset(int index) {
        if (index < 0) {
            return;
        }
        try {
            var presets = FileUtil.getPresets();
            if (presets == null || presets.size() <= index) {
                displayClientMessage(Component.translatable(MOD_ID + ".action_result_message.failed_to_get_presets", index + 1));
                return;
            }
            var preset = presets.get(index);
            FileUtil.loadPreset(Minecraft.getInstance().options, preset.getName());
            displayClientMessage(Component.translatable(MOD_ID + ".action_result_message.preset_loaded", preset.getName())
                    .withStyle(ChatFormatting.GREEN));
        } catch (IOException exception) {
            LOGGER.warn("Could not load preset {}", index + 1, exception);
            displayClientMessage(Component.translatable(MOD_ID + ".action_result_message.loading_preset_failed", index + 1)
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private static void displayClientMessage(Component message) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendOverlayMessage(message);
        }
    }

    /**
     * Builds the "VK" button that is injected into the vanilla controls screen.
     */
    public static Button createMenuButton(Screen parent) {
        return Button.builder(
                        Component.literal("VK"),
                        button -> Minecraft.getInstance().setScreen(new AlternativeKeybindScreen(parent, Minecraft.getInstance().options))
                )
                .bounds(
                        parent.width / 2 + 150 + 10 + ClientConfig.menuButtonOffsetX,
                        parent.height / 6 - 12 + ClientConfig.menuButtonOffsetY,
                        27, 20
                )
                .tooltip(Tooltip.create(Component.literal("Visual Keybinder")))
                .build();
    }

    public static InputConstants.Key parseKey(String value) {
        return InputConstants.getKey(value);
    }
}
