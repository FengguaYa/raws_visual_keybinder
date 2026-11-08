package com.gmail.rawlxxxviii.visual_keybinder;

import com.gmail.rawlxxxviii.visual_keybinder.config.ClientConfig;
import com.gmail.rawlxxxviii.visual_keybinder.screen.AlternativeKeybindScreen;
import com.gmail.rawlxxxviii.visual_keybinder.util.FileUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
        LOGGER.info("[visual_keybinder] Initialising client, game dir = {}", gameDir);
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

        boolean opened = false;
        while (OPEN_SCREEN != null && OPEN_SCREEN.consumeClick() && !opened) {
            opened = true;
        }
        if (opened) {
            openScreen(minecraft.screen);
            return;
        }

        if (minecraft.player == null) {
            return;
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
    /**
     * Builds the icon button that is injected into the vanilla controls screen.
     * The icon lives in {@code textures/gui/sprites/icon.png} exactly like in the
     * 1.21 release of the mod.
     */
    public static Button createMenuButton(Screen parent) {
        WidgetSprites sprites = new WidgetSprites(
                Identifier.fromNamespaceAndPath(MOD_ID, "icon"),
                Identifier.fromNamespaceAndPath(MOD_ID, "icon_highlighted"));
        MenuButton button = new MenuButton(0, 0, 27, 20, sprites, ignored -> openScreen(parent), parent);
        button.setTooltip(Tooltip.create(Component.translatable("raws_visual_keybinder.screen.title")));
        return button;
    }

    public static void openScreen(Screen parent) {
        try {
            LOGGER.info("[visual_keybinder] Opening the Visual Keybinder screen");
            Minecraft.getInstance().setScreen(new AlternativeKeybindScreen(parent, Minecraft.getInstance().options));
        } catch (Throwable throwable) {
            LOGGER.error("[visual_keybinder] Could not open the Visual Keybinder screen", throwable);
        }
    }

    /**
     * The vanilla 26.1 options list is full width and would swallow clicks in its
     * area, so the button anchors itself next to the right hand column of the
     * controls screen but stays inside the header, above the list.
     */
    private static final class MenuButton extends ImageButton {

        private final Screen parent;
        private boolean located;

        private MenuButton(int x, int y, int width, int height, WidgetSprites sprites,
                           Button.OnPress onPress, Screen parent) {
            super(x, y, width, height, sprites, onPress);
            this.parent = parent;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            locate();
            super.extractContents(graphics, mouseX, mouseY, partialTick);
        }

        private void locate() {
            if (located) {
                return;
            }
            located = true;

            List<AbstractWidget> widgets = new ArrayList<>();
            harvest(parent.children(), widgets);

            AbstractWidget target = null;
            for (AbstractWidget widget : widgets) {
                if (widget == this) {
                    continue;
                }
                if (widget.getX() >= parent.width / 2 && widget.getWidth() >= 100) {
                    if (target == null || widget.getY() < target.getY()) {
                        target = widget;
                    }
                }
            }

            if (target != null) {
                setX(Math.min(target.getX() + target.getWidth() + 16 + ClientConfig.menuButtonOffsetX,
                        parent.width - 32));
            } else {
                setX(Math.min(parent.width / 2 + 166 + ClientConfig.menuButtonOffsetX, parent.width - 32));
            }
            // Stay in the header, above the (full width) options list so clicks work.
            setY(8 + ClientConfig.menuButtonOffsetY);
        }

        private static void harvest(Iterable<? extends GuiEventListener> listeners, List<AbstractWidget> out) {
            for (GuiEventListener listener : listeners) {
                if (listener instanceof AbstractWidget widget) {
                    out.add(widget);
                }
                if (listener instanceof ContainerEventHandler container) {
                    harvest(container.children(), out);
                }
            }
        }
    }

    public static InputConstants.Key parseKey(String value) {
        return InputConstants.getKey(value);
    }
}
