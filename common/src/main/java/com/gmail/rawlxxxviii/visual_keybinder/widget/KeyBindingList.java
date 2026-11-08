package com.gmail.rawlxxxviii.visual_keybinder.widget;

import com.gmail.rawlxxxviii.visual_keybinder.KeyboardLayoutKey;
import com.gmail.rawlxxxviii.visual_keybinder.config.ClientConfig;
import com.gmail.rawlxxxviii.visual_keybinder.screen.AlternativeKeybindScreen;
import com.gmail.rawlxxxviii.visual_keybinder.util.KeyUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class KeyBindingList extends ScrollList {

    public static final int MODE_ALL = 0;
    public static final int MODE_DETAIL = 1;

    private static final int EDIT_WIDTH = 72;
    private static final int RESET_WIDTH = 68;

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
        this.scrollAmount = Math.min(this.scrollAmount, maxScroll());
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
            rows.add(Row.binding(keyMapping));
        }
        if (rows.isEmpty()) {
            rows.add(Row.text(Component.literal("No results"), 0xFF9A9A9A));
        }
    }

    private void buildDetailRows(KeyMapping[] all) {
        rows.add(Row.text(Component.literal("Selected key: ")
                .append(selectedKey == null ? Component.literal("none") : selectedKey.getKey().getDisplayName()), 0xFFFFFFFF));

        if (selectedKey == null) {
            rows.add(Row.text(Component.literal("Select a key to view/edit bindings"), 0xFF9A9A9A));
            return;
        }

        KeyMapping[] keyMappings = screen.getKeyMappings(selectedKey);
        if (keyMappings.length == 0) {
            rows.add(Row.text(Component.literal("No bindings"), 0xFF9A9A9A));
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
            rows.add(Row.binding(keyMapping));
        }
    }

    private boolean matches(String value, String filter) {
        return value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private int editButtonX() {
        return getX() + this.width - EDIT_WIDTH - RESET_WIDTH - 16;
    }

    private int resetButtonX() {
        return getX() + this.width - RESET_WIDTH - 8;
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
        boolean editHovered = isInside(mouseX, mouseY, editX, rowTop, EDIT_WIDTH, 20);
        boolean resetHovered = isInside(mouseX, mouseY, resetX, rowTop, RESET_WIDTH, 20);

        drawButton(graphics, font,
                screen.getKeyMappingToChange() == keyMapping ? Component.literal("> ... <") : keyMapping.getTranslatedKeyMessage(),
                editX, rowTop, EDIT_WIDTH, editHovered,
                conflicting ? AlternativeKeybindScreen.CONFLICT_COLOR : 0xFFFFFFFF);

        drawButton(graphics, font,
                Component.literal("(").append(keyMapping.getDefaultKey().getDisplayName().getString()).append(")"),
                resetX, rowTop, RESET_WIDTH, resetHovered,
                keyMapping.isDefault() ? 0xFF808080 : AlternativeKeybindScreen.RESET_COLOR);
    }

    private void drawButton(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, Component label,
                            int x, int y, int width, boolean hovered, int color) {
        int background = hovered ? 0xFF565656 : 0xFF3A3A3A;
        graphics.fill(x, y + 1, x + width, y + 19, background);
        graphics.outline(x, y + 1, width, 18, hovered ? 0xFFBFBFBF : 0xFF666666);
        graphics.centeredText(font, label, x + width / 2, y + 7, color);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    protected boolean clickRow(int index, double mouseX, double mouseY, int button) {
        Row row = rows.get(index);
        if (row.mapping == null) {
            return false;
        }
        int rowTop = getY() + index * rowHeight - (int) scrollAmount;
        if (isInside(mouseX, mouseY, editButtonX(), rowTop, EDIT_WIDTH, 20)) {
            screen.beginEditing(row.mapping);
            return true;
        }
        if (isInside(mouseX, mouseY, resetButtonX(), rowTop, RESET_WIDTH, 20)) {
            row.mapping.setKey(row.mapping.getDefaultKey());
            options.save();
            KeyMapping.resetMapping();
            screen.onBindingsChanged();
            return true;
        }
        if (ClientConfig.displayChangeAndResetButtonTooltips) {
            // Nothing else handles row clicks, this keeps the row inert.
            return false;
        }
        return false;
    }

    private static final class Row {
        private final Component label;
        private final KeyMapping mapping;
        private final int color;

        private Row(Component label, KeyMapping mapping, int color) {
            this.label = label;
            this.mapping = mapping;
            this.color = color;
        }

        static Row text(Component label, int color) {
            return new Row(label, null, color);
        }

        static Row binding(KeyMapping mapping) {
            return new Row(null, mapping, 0xFFFFFFFF);
        }
    }
}
