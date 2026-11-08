package com.gmail.rawlxxxviii.visual_keybinder.util;

import com.gmail.rawlxxxviii.visual_keybinder.VisualKeybinder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Optional bridge to MaLiLib's own keybind system. MaLiLib keybinds are not
 * vanilla {@code KeyMapping}s, so they are read through reflection and only
 * when MaLiLib is actually installed.
 */
public final class MalilibSupport {

    private static Boolean available;
    private static Method getKeybindManager;
    private static Method getKeybindCategories;
    private static Method categoryGetHotkeys;
    private static Method categoryGetModName;
    private static Method categoryGetCategory;
    private static Method hotkeyGetKeybind;
    private static Method configGetTranslatedName;
    private static Method configGetName;
    private static Method keybindGetKeys;
    private static Method keybindClearKeys;
    private static Method keybindAddKey;
    private static Method keybindGetDisplayString;
    private static Method keybindMarkDirty;
    private static Method keybindResetToDefault;
    private static Method keybindIsModified;

    private MalilibSupport() {
    }

    public static synchronized boolean isAvailable() {
        if (available != null) {
            return available;
        }
        try {
            Class<?> inputEventHandler = Class.forName("fi.dy.masa.malilib.event.InputEventHandler");
            getKeybindManager = inputEventHandler.getMethod("getKeybindManager");
            Class<?> keybindManager = Class.forName("fi.dy.masa.malilib.hotkeys.IKeybindManager");
            getKeybindCategories = keybindManager.getMethod("getKeybindCategories");

            Class<?> category = Class.forName("fi.dy.masa.malilib.hotkeys.KeybindCategory");
            categoryGetHotkeys = category.getMethod("getHotkeys");
            categoryGetModName = category.getMethod("getModName");
            categoryGetCategory = category.getMethod("getCategory");

            Class<?> hotkey = Class.forName("fi.dy.masa.malilib.hotkeys.IHotkey");
            hotkeyGetKeybind = hotkey.getMethod("getKeybind");
            Class<?> config = Class.forName("fi.dy.masa.malilib.config.IConfigBase");
            configGetTranslatedName = config.getMethod("getTranslatedName");
            configGetName = config.getMethod("getName");

            Class<?> keybind = Class.forName("fi.dy.masa.malilib.hotkeys.IKeybind");
            keybindGetKeys = keybind.getMethod("getKeys");
            keybindClearKeys = keybind.getMethod("clearKeys");
            keybindAddKey = keybind.getMethod("addKey", int.class);
            keybindGetDisplayString = keybind.getMethod("getKeysDisplayString");
            keybindMarkDirty = keybind.getMethod("markDirty");
            keybindResetToDefault = keybind.getMethod("resetToDefault");
            keybindIsModified = keybind.getMethod("isModified");

            available = true;
        } catch (Throwable throwable) {
            available = false;
        }
        return available;
    }

    public static List<Category> getCategories() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        List<Category> result = new ArrayList<>();
        try {
            Object manager = getKeybindManager.invoke(null);
            List<?> categories = (List<?>) getKeybindCategories.invoke(manager);
            for (Object rawCategory : categories) {
                String modName = (String) categoryGetModName.invoke(rawCategory);
                String categoryName = (String) categoryGetCategory.invoke(rawCategory);
                List<Hotkey> hotkeys = new ArrayList<>();
                List<?> rawHotkeys = (List<?>) categoryGetHotkeys.invoke(rawCategory);
                for (Object rawHotkey : rawHotkeys) {
                    Hotkey hotkey = new Hotkey(rawHotkey);
                    if (hotkey.isValid()) {
                        hotkeys.add(hotkey);
                    }
                }
                if (!hotkeys.isEmpty()) {
                    result.add(new Category(modName, categoryName, hotkeys));
                }
            }
        } catch (Throwable throwable) {
            VisualKeybinder.LOGGER.warn("[visual_keybinder] Could not read MaLiLib keybinds", throwable);
        }
        return result;
    }

    public static final class Category {
        private final String modName;
        private final String categoryName;
        private final List<Hotkey> hotkeys;

        private Category(String modName, String categoryName, List<Hotkey> hotkeys) {
            this.modName = modName;
            this.categoryName = categoryName;
            this.hotkeys = hotkeys;
        }

        public String getModName() {
            return modName;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public List<Hotkey> getHotkeys() {
            return hotkeys;
        }
    }

    public static final class Hotkey {
        private final Object hotkey;
        private final Object keybind;

        private Hotkey(Object hotkey) {
            this.hotkey = hotkey;
            Object keybind = null;
            try {
                keybind = hotkeyGetKeybind.invoke(hotkey);
            } catch (Throwable ignored) {
            }
            this.keybind = keybind;
        }

        public boolean isValid() {
            return keybind != null;
        }

        public String getName() {
            try {
                String translated = (String) configGetTranslatedName.invoke(hotkey);
                if (translated != null && !translated.isBlank()) {
                    return translated;
                }
            } catch (Throwable ignored) {
            }
            try {
                return String.valueOf(configGetName.invoke(hotkey));
            } catch (Throwable ignored) {
                return "?";
            }
        }

        public List<Integer> getKeyCodes() {
            try {
                List<?> keys = (List<?>) keybindGetKeys.invoke(keybind);
                List<Integer> result = new ArrayList<>(keys.size());
                for (Object key : keys) {
                    if (key instanceof Integer integer) {
                        result.add(integer);
                    }
                }
                return result;
            } catch (Throwable ignored) {
                return Collections.emptyList();
            }
        }

        public String getDisplayString() {
            try {
                return String.valueOf(keybindGetDisplayString.invoke(keybind));
            } catch (Throwable ignored) {
                return "";
            }
        }

        public boolean isUnbound() {
            return getKeyCodes().isEmpty();
        }

        public boolean isDefault() {
            try {
                return !((Boolean) keybindIsModified.invoke(keybind));
            } catch (Throwable ignored) {
                return true;
            }
        }

        public void applyKey(int glfwKeyCode) {
            try {
                keybindClearKeys.invoke(keybind);
                if (glfwKeyCode >= 0) {
                    keybindAddKey.invoke(keybind, glfwKeyCode);
                }
                keybindMarkDirty.invoke(keybind);
            } catch (Throwable throwable) {
                VisualKeybinder.LOGGER.warn("[visual_keybinder] Could not update MaLiLib keybind", throwable);
            }
        }

        public void reset() {
            try {
                keybindResetToDefault.invoke(keybind);
                keybindMarkDirty.invoke(keybind);
            } catch (Throwable throwable) {
                VisualKeybinder.LOGGER.warn("[visual_keybinder] Could not reset MaLiLib keybind", throwable);
            }
        }
    }
}
