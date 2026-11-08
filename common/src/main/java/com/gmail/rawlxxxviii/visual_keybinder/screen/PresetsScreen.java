package com.gmail.rawlxxxviii.visual_keybinder.screen;

import com.gmail.rawlxxxviii.visual_keybinder.KeybindingPreset;
import com.gmail.rawlxxxviii.visual_keybinder.util.FileUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public class PresetsScreen extends Screen {

    private final Screen parentScreen;
    private final Options options;

    private EditBox newPresetNameEditBox;
    private PresetList presetList;

    public PresetsScreen(Screen parentScreen, Options options) {
        super(Component.literal("Keybinding presets"));
        this.parentScreen = parentScreen;
        this.options = options;
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void repositionElements() {
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        int uiWidth = Math.min(400, this.width - 20);
        int uiLeft = this.width / 2 - uiWidth / 2;
        int uiTop = 40;
        int bottomPadding = 50;
        int uiHeight = this.height - uiTop - bottomPadding;

        this.newPresetNameEditBox = addRenderableWidget(new EditBox(
                this.font,
                uiLeft + 1,
                uiTop,
                Math.max(40, uiWidth - 110),
                20,
                Component.literal("new preset name")
        ));
        this.newPresetNameEditBox.setHint(Component.literal("Preset name"));

        addRenderableWidget(Button.builder(Component.literal("Save preset"), button -> savePreset(newPresetNameEditBox.getValue()))
                .bounds(uiLeft + uiWidth - 100, uiTop, 100, 20)
                .build());

        this.presetList = addRenderableWidget(new PresetList(this, options, uiLeft, uiTop + 30, uiWidth, uiHeight - 30));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 75, this.height - 26, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Unbind all"), button -> unbindAll())
                .bounds(uiLeft, this.height - 26, 100, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Reset all"), button -> resetAll())
                .bounds(uiLeft + 105, this.height - 26, 100, 20)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }

    public void savePreset(String name) {
        name = FileUtil.sanitizeString(name);
        if (name.isBlank()) {
            return;
        }
        try {
            FileUtil.savePreset(options, name);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        this.presetList.rebuild();
    }

    public void deletePreset(String name) {
        try {
            FileUtil.deletePreset(name);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        this.presetList.rebuild();
    }

    public void loadPreset(String name) {
        try {
            FileUtil.loadPreset(options, name);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        this.presetList.rebuild();
    }

    public void unbindAll() {
        for (KeyMapping keyMapping : this.options.keyMappings) {
            keyMapping.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
        }
        options.save();
        KeyMapping.resetMapping();
        this.presetList.rebuild();
    }

    public void resetAll() {
        for (KeyMapping keyMapping : this.options.keyMappings) {
            keyMapping.setKey(keyMapping.getDefaultKey());
        }
        options.save();
        KeyMapping.resetMapping();
        this.presetList.rebuild();
    }

    public boolean isPresetActive(KeybindingPreset preset) {
        if (preset.getLines().size() != options.keyMappings.length) {
            return false;
        }
        int matched = 0;
        for (KeyMapping keyMapping : options.keyMappings) {
            String mappingName = keyMapping.getName();
            String line = preset.getLines().stream()
                    .filter(x -> x.startsWith(mappingName + ":"))
                    .findFirst()
                    .orElse(null);
            if (line == null) {
                continue;
            }
            String value = line.substring(mappingName.length() + 1);
            int colon = value.indexOf(':');
            if (colon != -1) {
                value = value.substring(0, colon);
            }
            if (value.equals(keyMapping.saveString())) {
                matched++;
            }
        }
        return matched == options.keyMappings.length;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
    }
}
