package com.gmail.rawlxxxviii.visual_keybinder.widget;

import com.gmail.rawlxxxviii.visual_keybinder.KeybindingPreset;
import com.gmail.rawlxxxviii.visual_keybinder.screen.AlternativeKeybindScreen;
import com.gmail.rawlxxxviii.visual_keybinder.screen.PresetsScreen;
import com.gmail.rawlxxxviii.visual_keybinder.util.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PresetList extends ScrollList {

    private static final int BUTTON_WIDTH = 58;
    private static final int GAP = 4;

    private final PresetsScreen screen;
    private final Options options;
    private final List<Row> rows = new ArrayList<>();

    public PresetList(PresetsScreen screen, Options options, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.screen = screen;
        this.options = options;
        this.rowHeight = 24;
        rebuild();
    }

    public void rebuild() {
        rows.clear();
        List<KeybindingPreset> presets = FileUtil.getPresets();
        if (presets != null) {
            for (KeybindingPreset preset : presets) {
                rows.add(new Row(preset, screen.isPresetActive(preset)));
            }
        }
        this.scrollAmount = Math.max(0, Math.min(maxScroll(), this.scrollAmount));
    }

    @Override
    protected int rowCount() {
        return rows.size();
    }

    private int saveButtonX() {
        return getX() + this.width - (BUTTON_WIDTH * 3 + GAP * 2) - 8;
    }

    @Override
    protected void renderRow(GuiGraphicsExtractor graphics, int index, int rowTop, int mouseX, int mouseY, float partialTick) {
        Row row = rows.get(index);
        var font = Minecraft.getInstance().font;

        graphics.text(font, Component.literal(row.preset.getName()),
                getX() + 8, rowTop + 8,
                row.active ? AlternativeKeybindScreen.ACTIVE_COLOR : 0xFFE0E0E0, false);

        row.saveButton.setX(saveButtonX());
        row.saveButton.setY(rowTop + 2);
        row.saveButton.setWidth(BUTTON_WIDTH);
        row.saveButton.active = !row.preset.isReadOnly();
        row.saveButton.setMessage(KeyBindingList.colored(Component.translatable("raws_visual_keybinder.presets.save"),
                row.preset.isReadOnly() ? 0x808080 : AlternativeKeybindScreen.RESET_COLOR));
        row.saveButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

        row.loadButton.setX(saveButtonX() + BUTTON_WIDTH + GAP);
        row.loadButton.setY(rowTop + 2);
        row.loadButton.setWidth(BUTTON_WIDTH);
        row.loadButton.active = !row.active;
        row.loadButton.setMessage(KeyBindingList.colored(Component.translatable("raws_visual_keybinder.presets.load"),
                row.active ? 0x808080 : 0xFFFFFF));
        row.loadButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

        row.deleteButton.setX(saveButtonX() + (BUTTON_WIDTH + GAP) * 2);
        row.deleteButton.setY(rowTop + 2);
        row.deleteButton.setWidth(BUTTON_WIDTH);
        row.deleteButton.active = !row.preset.isReadOnly();
        row.deleteButton.setMessage(KeyBindingList.colored(Component.translatable("raws_visual_keybinder.presets.delete"),
                row.preset.isReadOnly() ? 0x808080 : AlternativeKeybindScreen.DANGER_COLOR));
        row.deleteButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean clickRow(int index, MouseButtonEvent event) {
        Row row = rows.get(index);
        if (row.saveButton.mouseClicked(event, false)) {
            return true;
        }
        if (row.loadButton.mouseClicked(event, false)) {
            return true;
        }
        if (row.deleteButton.mouseClicked(event, false)) {
            return true;
        }
        return false;
    }

    private final class Row {
        private final KeybindingPreset preset;
        private final boolean active;
        private final Button saveButton;
        private final Button loadButton;
        private final Button deleteButton;

        private Row(KeybindingPreset preset, boolean active) {
            this.preset = preset;
            this.active = active;

            this.saveButton = Button.builder(Component.empty(), button -> screen.savePreset(preset.getName()))
                    .bounds(0, 0, BUTTON_WIDTH, 20)
                    .build();
            this.loadButton = Button.builder(Component.empty(), button -> screen.loadPreset(preset.getName()))
                    .bounds(0, 0, BUTTON_WIDTH, 20)
                    .build();
            this.deleteButton = Button.builder(Component.empty(), button -> screen.deletePreset(preset.getName()))
                    .bounds(0, 0, BUTTON_WIDTH, 20)
                    .build();
        }
    }
}
