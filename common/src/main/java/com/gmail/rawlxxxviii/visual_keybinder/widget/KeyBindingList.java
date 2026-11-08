package com.gmail.rawlxxxviii.visual_keybinder.widget;

import com.gmail.rawlxxxviii.visual_keybinder.KeyboardLayoutKey;
import com.gmail.rawlxxxviii.visual_keybinder.screen.AlternativeKeybindScreen;
import com.gmail.rawlxxxviii.visual_keybinder.util.KeyUtil;
import com.gmail.rawlxxxviii.visual_keybinder.util.MalilibSupport;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class KeyBindingList extends ScrollList {

    public static final int MODE_ALL = 0;
    public static final int MODE_DETAIL = 1;

    private static final int KIND_TEXT = 0;
    private static final int KIND_HEADER = 1;
    private static final int KIND_NAME = 2;
    private static final int KIND_BUTTONS = 3;

    private static final int BUTTON_GAP = 4;

    private final AlternativeKeybindScreen screen;
    private final Options options;
    private final int mode;

    private KeyboardLayoutKey selectedKey;
    private String nameFilter = "";
    private String categoryFilter = "";

    private final List<Row> rows = new ArrayList<>();

    public KeyBindingList(AlternativeKeybindScreen screen, Options options, int mode,
                          int x, int y, int width, int height) {
        super(x, y, width, height);
        this.screen = screen;
        this.options = options;
        this.mode = mode;
        rebuild();
    }

    public void setSelectedKey(KeyboardLayoutKey key) {
        this.selectedKey = key;
        rebuild();
    }

    public KeyboardLayoutKey getSelectedKey() {
        return selectedKey;
    }

    public void setNameFilter(String value) {
        this.nameFilter = value == null ? "" : value;
        rebuild();
    }

    public void setCategoryFilter(String value) {
        this.categoryFilter = value == null ? "" : value;
        rebuild();
    }

    public void rebuild() {
        rows.clear();
        KeyMapping[] all = options.keyMappings.clone();
        Arrays.sort(all);

        if (mode == MODE_DETAIL) {
            buildDetailRows(all);
        } else {
            buildAllRows(all);
        }
        this.scrollAmount = Math.max(0, Math.min(maxScroll(), this.scrollAmount));
    }

    private void buildAllRows(KeyMapping[] all) {
        String lastCategory = null;
        for (KeyMapping keyMapping : all) {
            String categoryName = keyMapping.getCategory().label().getString();

            if (!categoryFilter.isEmpty() && !matches(categoryName, categoryFilter)) {
                continue;
            }
            if (!nameFilter.isEmpty() && !matches(Component.translatable(keyMapping.getName()).getString(), nameFilter)) {
                continue;
            }
            if (!categoryName.equals(lastCategory)) {
                lastCategory = categoryName;
                rows.add(Row.text(keyMapping.getCategory().label(), AlternativeKeybindScreen.CATEGORY_COLOR));
            }
            addBindingRows(keyMapping);
        }

        // MaLiLib keybinds are not vanilla KeyMappings, show them in their own categories.
        for (MalilibSupport.Category category : MalilibSupport.getCategories()) {
            String categoryName = category.getModName() + " / " + category.getCategoryName();
            if (!categoryFilter.isEmpty() && !matches(categoryName, categoryFilter)) {
                continue;
            }
            boolean addedHeader = false;
            for (MalilibSupport.Hotkey hotkey : category.getHotkeys()) {
                if (!nameFilter.isEmpty() && !matches(hotkey.getName(), nameFilter)) {
                    continue;
                }
                if (!addedHeader) {
                    addedHeader = true;
                    rows.add(Row.text(Component.literal(categoryName), AlternativeKeybindScreen.CATEGORY_COLOR));
                }
                addMalilibRows(hotkey);
            }
        }

        if (rows.isEmpty()) {
            rows.add(Row.text(Component.translatable("raws_visual_keybinder.screen.no_results"), 0xFF9A9A9A));
        }
    }

    private void buildDetailRows(KeyMapping[] all) {
        rows.add(Row.header(Component.translatable("raws_visual_keybinder.screen.selected_key",
                selectedKey == null
                        ? Component.translatable("raws_visual_keybinder.screen.none")
                        : selectedKey.getKey().getDisplayName())));

        if (selectedKey == null) {
            rows.add(Row.text(Component.translatable("raws_visual_keybinder.screen.no_selection"), 0xFF9A9A9A));
            return;
        }

        KeyMapping[] keyMappings = screen.getKeyMappings(selectedKey);
        boolean hasBinding = false;
        String lastCategory = null;
        for (KeyMapping keyMapping : keyMappings) {
            hasBinding = true;
            String categoryName = keyMapping.getCategory().label().getString();
            if (!categoryName.equals(lastCategory)) {
                lastCategory = categoryName;
                rows.add(Row.text(keyMapping.getCategory().label(), AlternativeKeybindScreen.CATEGORY_COLOR));
            }
            addBindingRows(keyMapping);
        }

        int glfwKey = selectedKey.getKey().getValue();
        boolean addedMalilibHeader = false;
        for (MalilibSupport.Category category : MalilibSupport.getCategories()) {
            for (MalilibSupport.Hotkey hotkey : category.getHotkeys()) {
                if (!hotkey.getKeyCodes().contains(glfwKey)) {
                    continue;
                }
                hasBinding = true;
                if (!addedMalilibHeader) {
                    addedMalilibHeader = true;
                    rows.add(Row.text(Component.literal(category.getModName() + " / " + category.getCategoryName()),
                            AlternativeKeybindScreen.CATEGORY_COLOR));
                }
                addMalilibRows(hotkey);
            }
        }

        if (!hasBinding) {
            rows.add(Row.text(Component.translatable("raws_visual_keybinder.screen.no_bindings"), 0xFF9A9A9A));
        }
    }

    private void addBindingRows(KeyMapping keyMapping) {
        rows.add(Row.name(keyMapping));
        rows.add(Row.buttons(keyMapping, this));
    }

    private void addMalilibRows(MalilibSupport.Hotkey hotkey) {
        rows.add(Row.malilibName(hotkey));
        rows.add(Row.malilibButtons(hotkey, this));
    }

    private boolean matches(String value, String filter) {
        return value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private int editWidth() {
        return Math.max(60, Math.min(170, (int) (this.width * 0.45f) - 6));
    }

    private int editX() {
        return getX() + 6;
    }

    private int resetX() {
        return editX() + editWidth() + BUTTON_GAP;
    }

    private int resetWidth() {
        return Math.max(50, Math.min(130, (int) (this.width * 0.35f)));
    }

    static Component colored(Component text, int argb) {
        return text.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(argb & 0xFFFFFF)));
    }

    @Override
    protected int rowCount() {
        return rows.size();
    }

    @Override
    protected void renderRow(GuiGraphicsExtractor graphics, int index, int rowTop, int mouseX, int mouseY, float partialTick) {
        Row row = rows.get(index);
        var font = Minecraft.getInstance().font;
        int left = getX() + 6;

        switch (row.kind) {
            case KIND_HEADER -> {
                graphics.fill(getX(), rowTop, getX() + this.width, rowTop + rowHeight,
                        AlternativeKeybindScreen.LIST_TITLE_BACKGROUND_COLOR);
                String text = font.plainSubstrByWidth(row.label.getString(), Math.max(20, this.width - 12));
                graphics.text(font, text, left, rowTop + 6, 0xFFFFFFFF, false);
            }
            case KIND_TEXT -> {
                String text = font.plainSubstrByWidth(row.label.getString(), Math.max(20, this.width - 12));
                graphics.text(font, text, left, rowTop + 6, row.color, false);
            }
            case KIND_NAME -> {
                if (row.mapping != null) {
                    KeyMapping keyMapping = row.mapping;
                    if (KeyUtil.hasConflict(options.keyMappings, keyMapping)) {
                        renderName(graphics, font, Component.translatable(keyMapping.getName()).getString(),
                                left, rowTop, AlternativeKeybindScreen.CONFLICT_COLOR);
                    } else {
                        renderName(graphics, font, Component.translatable(keyMapping.getName()).getString(),
                                left, rowTop, 0xFFE0E0E0);
                    }
                } else if (row.hotkey != null) {
                    renderName(graphics, font, row.hotkey.getName(), left, rowTop, 0xFFE0E0E0);
                }
            }
            case KIND_BUTTONS -> renderButtons(graphics, row, rowTop, mouseX, mouseY, partialTick);
            default -> {
            }
        }
    }

    private void renderName(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                            String text, int left, int rowTop, int color) {
        graphics.text(font, font.plainSubstrByWidth(text, Math.max(20, this.width - 12)),
                left, rowTop + 6, color, false);
    }

    private void renderButtons(GuiGraphicsExtractor graphics, Row row, int rowTop, int mouseX, int mouseY, float partialTick) {
        Button changeButton = row.changeButton;
        Button resetButton = row.resetButton;
        if (changeButton == null || resetButton == null) {
            return;
        }

        if (row.mapping != null) {
            KeyMapping keyMapping = row.mapping;
            boolean conflicting = KeyUtil.hasConflict(options.keyMappings, keyMapping);
            changeButton.setMessage(colored(
                    screen.getKeyMappingToChange() == keyMapping
                            ? Component.literal("> ... <")
                            : keyMapping.getTranslatedKeyMessage(),
                    conflicting ? AlternativeKeybindScreen.CONFLICT_COLOR : 0xFFFFFF));
            resetButton.active = !keyMapping.isDefault();
            resetButton.setMessage(colored(
                    Component.literal("(").append(keyMapping.getDefaultKey().getDisplayName().getString()).append(")"),
                    keyMapping.isDefault() ? 0x808080 : AlternativeKeybindScreen.RESET_COLOR));
        } else if (row.hotkey != null) {
            MalilibSupport.Hotkey hotkey = row.hotkey;
            changeButton.setMessage(colored(
                    screen.getMalilibToChange() == hotkey
                            ? Component.literal("> ... <")
                            : Component.literal(hotkey.getDisplayString()),
                    hotkey.isUnbound() ? AlternativeKeybindScreen.UNBOUND_COLOR : 0xFFFFFF));
            resetButton.active = true;
            resetButton.setMessage(colored(Component.literal("(default)"), 0x808080));
        }

        changeButton.setX(editX());
        changeButton.setY(rowTop);
        changeButton.setWidth(editWidth());
        changeButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

        resetButton.setX(resetX());
        resetButton.setY(rowTop);
        resetButton.setWidth(resetWidth());
        resetButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean clickRow(int index, MouseButtonEvent event) {
        Row row = rows.get(index);
        if (row.kind != KIND_BUTTONS) {
            return false;
        }
        if (row.changeButton != null && row.changeButton.mouseClicked(event, false)) {
            return true;
        }
        if (row.resetButton != null && row.resetButton.mouseClicked(event, false)) {
            return true;
        }
        return false;
    }

    private static final class Row {
        private final int kind;
        private final Component label;
        private final int color;
        private final KeyMapping mapping;
        private final MalilibSupport.Hotkey hotkey;
        private final Button changeButton;
        private final Button resetButton;

        private Row(int kind, Component label, int color, KeyMapping mapping, MalilibSupport.Hotkey hotkey,
                    Button changeButton, Button resetButton) {
            this.kind = kind;
            this.label = label;
            this.color = color;
            this.mapping = mapping;
            this.hotkey = hotkey;
            this.changeButton = changeButton;
            this.resetButton = resetButton;
        }

        static Row text(Component label, int color) {
            return new Row(KIND_TEXT, label, color, null, null, null, null);
        }

        static Row header(Component label) {
            return new Row(KIND_HEADER, label, 0xFFFFFFFF, null, null, null, null);
        }

        static Row name(KeyMapping mapping) {
            return new Row(KIND_NAME, null, 0xFFFFFFFF, mapping, null, null, null);
        }

        static Row malilibName(MalilibSupport.Hotkey hotkey) {
            return new Row(KIND_NAME, null, 0xFFFFFFFF, null, hotkey, null, null);
        }

        static Row buttons(KeyMapping mapping, KeyBindingList list) {
            Button changeButton = Button.builder(Component.empty(), button -> list.screen.beginEditing(mapping))
                    .bounds(0, 0, 100, 20)
                    .build();

            Button resetButton = Button.builder(Component.empty(), button -> {
                        mapping.setKey(mapping.getDefaultKey());
                        list.options.save();
                        KeyMapping.resetMapping();
                        list.screen.onBindingsChanged();
                    })
                    .bounds(0, 0, 80, 20)
                    .build();

            return new Row(KIND_BUTTONS, null, 0xFFFFFFFF, mapping, null, changeButton, resetButton);
        }

        static Row malilibButtons(MalilibSupport.Hotkey hotkey, KeyBindingList list) {
            Button changeButton = Button.builder(Component.empty(), button -> list.screen.beginEditingMalilib(hotkey))
                    .bounds(0, 0, 100, 20)
                    .build();

            Button resetButton = Button.builder(Component.empty(), button -> {
                        hotkey.reset();
                        list.screen.onBindingsChanged();
                    })
                    .bounds(0, 0, 80, 20)
                    .build();

            return new Row(KIND_BUTTONS, null, 0xFFFFFFFF, null, hotkey, changeButton, resetButton);
        }
    }
}
