package com.gmail.rawlxxxviii.visual_keybinder.widget;

import com.gmail.rawlxxxviii.visual_keybinder.KeyboardLayoutKey;
import com.gmail.rawlxxxviii.visual_keybinder.screen.AlternativeKeybindScreen;
import com.gmail.rawlxxxviii.visual_keybinder.util.KeyUtil;
import net.minecraft.ChatFormatting;
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

    private static final int EDIT_WIDTH = 110;
    private static final int RESET_WIDTH = 80;
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
            Component category = keyMapping.getCategory().label();
            String categoryName = category.getString();

            if (!categoryFilter.isEmpty() && !matches(categoryName, categoryFilter)) {
                continue;
            }
            if (!nameFilter.isEmpty() && !matches(Component.translatable(keyMapping.getName()).getString(), nameFilter)) {
                continue;
            }
            if (!categoryName.equals(lastCategory)) {
                lastCategory = categoryName;
                rows.add(Row.text(category, AlternativeKeybindScreen.CATEGORY_COLOR));
            }
            rows.add(Row.binding(keyMapping, this));
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
        if (keyMappings.length == 0) {
            rows.add(Row.text(Component.translatable("raws_visual_keybinder.screen.no_bindings"), 0xFF9A9A9A));
            return;
        }

        String lastCategory = null;
        for (KeyMapping keyMapping : keyMappings) {
            Component category = keyMapping.getCategory().label();
            String categoryName = category.getString();
            if (!categoryName.equals(lastCategory)) {
                lastCategory = categoryName;
                rows.add(Row.text(category, AlternativeKeybindScreen.CATEGORY_COLOR));
            }
            rows.add(Row.binding(keyMapping, this));
        }
    }

    private boolean matches(String value, String filter) {
        return value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private int editButtonX() {
        return getX() + this.width - EDIT_WIDTH - RESET_WIDTH - BUTTON_GAP - 8;
    }

    private int resetButtonX() {
        return editButtonX() + EDIT_WIDTH + BUTTON_GAP;
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

        if (row.header) {
            graphics.fill(getX(), rowTop, getX() + this.width, rowTop + rowHeight,
                    AlternativeKeybindScreen.LIST_TITLE_BACKGROUND_COLOR);
            graphics.text(font, row.label, left, rowTop + 6, 0xFFFFFFFF, false);
            return;
        }

        if (row.mapping == null) {
            graphics.text(font, row.label, left, rowTop + 6, row.color, false);
            return;
        }

        KeyMapping keyMapping = row.mapping;
        boolean conflicting = KeyUtil.hasConflict(options.keyMappings, keyMapping);
        int nameColor = conflicting ? AlternativeKeybindScreen.CONFLICT_COLOR : 0xFFE0E0E0;
        graphics.text(font, Component.translatable(keyMapping.getName()), left, rowTop + 6, nameColor, false);

        int editX = editButtonX();
        int resetX = resetButtonX();

        Button changeButton = row.changeButton;
        changeButton.setX(editX);
        changeButton.setY(rowTop);
        changeButton.setWidth(EDIT_WIDTH);
        changeButton.setMessage(colored(
                screen.getKeyMappingToChange() == keyMapping
                        ? Component.literal("> ... <")
                        : keyMapping.getTranslatedKeyMessage(),
                conflicting ? AlternativeKeybindScreen.CONFLICT_COLOR : 0xFFFFFF));
        changeButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Button resetButton = row.resetButton;
        resetButton.setX(resetX);
        resetButton.setY(rowTop);
        resetButton.setWidth(RESET_WIDTH);
        resetButton.active = !keyMapping.isDefault();
        resetButton.setMessage(colored(
                Component.literal("(").append(keyMapping.getDefaultKey().getDisplayName().getString()).append(")"),
                keyMapping.isDefault() ? 0x808080 : AlternativeKeybindScreen.RESET_COLOR));
        resetButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected boolean clickRow(int index, MouseButtonEvent event) {
        Row row = rows.get(index);
        if (row.changeButton != null && row.changeButton.mouseClicked(event, false)) {
            return true;
        }
        if (row.resetButton != null && row.resetButton.mouseClicked(event, false)) {
            return true;
        }
        return false;
    }

    private static final class Row {
        private final Component label;
        private final KeyMapping mapping;
        private final int color;
        private final boolean header;
        private final Button changeButton;
        private final Button resetButton;

        private Row(Component label, KeyMapping mapping, int color, boolean header,
                    Button changeButton, Button resetButton) {
            this.label = label;
            this.mapping = mapping;
            this.color = color;
            this.header = header;
            this.changeButton = changeButton;
            this.resetButton = resetButton;
        }

        static Row text(Component label, int color) {
            return new Row(label, null, color, false, null, null);
        }

        static Row header(Component label) {
            return new Row(label, null, 0xFFFFFFFF, true, null, null);
        }

        static Row binding(KeyMapping mapping, KeyBindingList list) {
            Button changeButton = Button.builder(
                            Component.empty(),
                            button -> list.screen.beginEditing(mapping))
                    .bounds(0, 0, EDIT_WIDTH, 20)
                    .build();

            Button resetButton = Button.builder(
                            Component.literal("(").append(mapping.getDefaultKey().getDisplayName().getString()).append(")"),
                            button -> {
                                mapping.setKey(mapping.getDefaultKey());
                                list.options.save();
                                KeyMapping.resetMapping();
                                list.screen.onBindingsChanged();
                            })
                    .bounds(0, 0, RESET_WIDTH, 20)
                    .build();

            return new Row(null, mapping, 0xFFFFFFFF, false, changeButton, resetButton);
        }
    }
}
