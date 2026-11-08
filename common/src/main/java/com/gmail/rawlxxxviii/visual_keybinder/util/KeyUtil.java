package com.gmail.rawlxxxviii.visual_keybinder.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Helper methods around {@link KeyMapping} that only rely on vanilla API so the
 * same code can run on both Fabric and NeoForge.
 */
public final class KeyUtil {

    private KeyUtil() {
    }

    public static String getKeyId(KeyMapping keyMapping) {
        return keyMapping.saveString();
    }

    public static boolean sameKey(KeyMapping first, KeyMapping second) {
        if (first == second) {
            return true;
        }
        if (first.isUnbound() || second.isUnbound()) {
            return false;
        }
        return first.saveString().equals(second.saveString());
    }

    public static boolean hasConflict(KeyMapping[] keyMappings) {
        for (int i = 0; i < keyMappings.length; i++) {
            for (int j = i + 1; j < keyMappings.length; j++) {
                if (sameKey(keyMappings[i], keyMappings[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasConflict(KeyMapping[] keyMappings, KeyMapping item) {
        for (KeyMapping keyMapping : keyMappings) {
            if (keyMapping == item || keyMapping.getName().equals(item.getName())) {
                continue;
            }
            if (sameKey(keyMapping, item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasDefaultConflict(KeyMapping[] keyMappings, KeyMapping item) {
        InputConstants.Key defaultKey = item.getDefaultKey();
        if (defaultKey.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        for (KeyMapping keyMapping : keyMappings) {
            if (keyMapping.getName().equals(item.getName())) {
                continue;
            }
            InputConstants.Key other = keyMapping.getDefaultKey();
            if (other.equals(InputConstants.UNKNOWN)) {
                continue;
            }
            if (other.equals(defaultKey)) {
                return true;
            }
        }
        return false;
    }
}
