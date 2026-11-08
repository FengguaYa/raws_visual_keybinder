package com.gmail.rawlxxxviii.visual_keybinder.config;

import com.gmail.rawlxxxviii.visual_keybinder.VisualKeybinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Small, loader independent client configuration. Values are stored in
 * {@code config/visual_keybinder-client.properties} and are read once during mod
 * initialisation.
 */
public final class ClientConfig {

    private static final String FILE_NAME = "visual_keybinder-client.properties";

    public static int menuButtonOffsetX = 0;
    public static int menuButtonOffsetY = 0;
    public static boolean displayConflictContext = false;
    public static boolean displayLayoutButtonTooltips = true;
    public static boolean displayChangeAndResetButtonTooltips = true;

    private static Path file;

    private ClientConfig() {
    }

    public static void load(Path configDir) {
        try {
            Files.createDirectories(configDir);
            file = configDir.resolve(FILE_NAME);
            if (Files.isRegularFile(file)) {
                Properties properties = new Properties();
                try (InputStream in = Files.newInputStream(file)) {
                    properties.load(in);
                }
                menuButtonOffsetX = parseInt(properties, "menu_button_offset_x", 0);
                menuButtonOffsetY = parseInt(properties, "menu_button_offset_y", 0);
                displayConflictContext = parseBoolean(properties, "display_conflict_context", false);
                displayLayoutButtonTooltips = parseBoolean(properties, "display_layout_button_tooltip", true);
                displayChangeAndResetButtonTooltips = parseBoolean(properties, "display_button_tooltip", true);
            } else {
                save();
            }
        } catch (Exception exception) {
            VisualKeybinder.LOGGER.warn("Could not load Visual Keybinder client config", exception);
        }
    }

    public static void save() {
        if (file == null) {
            return;
        }
        try {
            Properties properties = new Properties();
            properties.setProperty("menu_button_offset_x", Integer.toString(menuButtonOffsetX));
            properties.setProperty("menu_button_offset_y", Integer.toString(menuButtonOffsetY));
            properties.setProperty("display_conflict_context", Boolean.toString(displayConflictContext));
            properties.setProperty("display_layout_button_tooltip", Boolean.toString(displayLayoutButtonTooltips));
            properties.setProperty("display_button_tooltip", Boolean.toString(displayChangeAndResetButtonTooltips));
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Raw's Visual Keybinder client settings");
            }
        } catch (IOException exception) {
            VisualKeybinder.LOGGER.warn("Could not save Visual Keybinder client config", exception);
        }
    }

    private static int parseInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean parseBoolean(Properties properties, String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)).trim());
    }
}
