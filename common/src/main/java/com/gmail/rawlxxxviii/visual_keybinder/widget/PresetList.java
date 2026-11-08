package com.gmail.rawlxxxviii.visual_keybinder.widget;

import com.gmail.rawlxxxviii.visual_keybinder.KeybindingPreset;
import com.gmail.rawlxxxviii.visual_keybinder.screen.AlternativeKeybindScreen;
import com.gmail.rawlxxxviii.visual_keybinder.screen.PresetsScreen;
import com.gmail.rawlxxxviii.visual_keybinder.util.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PresetList extends ScrollList {

    private static final int BUTTON_WIDTH = 58;
    private static final int GAP = 4;

    private final PresetsScreen screen;
    private final Options options;
    private final List<KeybindingPreset> presets = new ArrayList<>();

    public PresetList(PresetsScreen screen, Options options, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.screen = screen;
        this.options = options;
        this.rowHeight = 24;
        rebuild();
    }

    public void rebuild() {
        presets.clear();
        List<KeybindingPreset> loaded = FileUtil.getPresets();
        if (loaded != null) {
            presets.addAll(loaded);
        }
        this.scrollAmount = Math.max(0, Math.min(maxScroll(), this.scrollAmount));
    }

    @Override
    protected int rowCount() {
        return presets.size();
    }

    private int saveButtonX() {
        return getX() + this.width - (BUTTON_WIDTH * 3 + GAP * 2) - 8;
    }

    private int loadButtonX() {
        return saveButtonX() + BUTTON_WIDTH + GAP;
    }

    private int deleteButtonX() {
        return loadButtonX() + BUTTON_WIDTH + GAP;
    }

    @Override
    protected void renderRow(GuiGraphicsExtractor graphics, int index, int rowTop, int mouseX, int mouseY, float partialTick) {
        KeybindingPreset preset = presets.get(index);
        var font = Minecraft.getInstance().font;
        boolean active = screen.isPresetActive(preset);

        graphics.text(font, Component.literal(preset.getName()),
                getX() + 8, rowTop + 8,
                active ? AlternativeKeybindScreen.ACTIVE_COLOR : 0xFFE0E0E0, false);

        drawButton(graphics, font, Component.translatable("raws_visual_keybinder.presets.save"), saveButtonX(), rowTop,
                BUTTON_WIDTH, isInside(mouseX, mouseY, saveButtonX(), rowTop),
                preset.isReadOnly() ? 0xFF808080 : AlternativeKeybindScreen.RESET_COLOR);
        drawButton(graphics, font, Component.translatable("raws_visual_keybinder.presets.load"), loadButtonX(), rowTop,
                BUTTON_WIDTH, isInside(mouseX, mouseY, loadButtonX(), rowTop),
                active ? 0xFF808080 : 0xFFFFFFFF);
        drawButton(graphics, font, Component.translatable("raws_visual_keybinder.presets.delete"), deleteButtonX(), rowTop,
                BUTTON_WIDTH, isInside(mouseX, mouseY, deleteButtonX(), rowTop),
                preset.isReadOnly() ? 0xFF808080 : AlternativeKeybindScreen.DANGER_COLOR);
    }

    private void drawButton(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, Component label,
                            int x, int y, int width, boolean hovered, int color) {
        int background = hovered ? 0xFF565656 : 0xFF3A3A3A;
        graphics.fill(x, y + 3, x + width, y + 21, background);
        graphics.outline(x, y + 3, width, 18, hovered ? 0xFFBFBFBF : 0xFF666666);
        graphics.centeredText(font, label, x + width / 2, y + 9, color);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + BUTTON_WIDTH && mouseY >= y && mouseY < y + 24;
    }

    @Override
    protected boolean clickRow(int index, double mouseX, double mouseY, int button) {
        KeybindingPreset preset = presets.get(index);
        int rowTop = getY() + index * rowHeight - (int) scrollAmount;

        if (isInside(mouseX, mouseY, saveButtonX(), rowTop)) {
            if (!preset.isReadOnly()) {
                screen.savePreset(preset.getName());
            }
            return true;
        }
        if (isInside(mouseX, mouseY, loadButtonX(), rowTop)) {
            screen.loadPreset(preset.getName());
            return true;
        }
        if (isInside(mouseX, mouseY, deleteButtonX(), rowTop)) {
            if (!preset.isReadOnly()) {
                screen.deletePreset(preset.getName());
            }
            return true;
        }
        return false;
    }
}
