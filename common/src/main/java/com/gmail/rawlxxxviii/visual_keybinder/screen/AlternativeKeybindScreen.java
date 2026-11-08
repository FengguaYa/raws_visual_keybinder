package com.gmail.rawlxxxviii.visual_keybinder.screen;

import com.gmail.rawlxxxviii.visual_keybinder.FilterEditBox;
import com.gmail.rawlxxxviii.visual_keybinder.KeyBoardLayout;
import com.gmail.rawlxxxviii.visual_keybinder.KeyboardLayoutKey;
import com.gmail.rawlxxxviii.visual_keybinder.VisualKeybinder;
import com.gmail.rawlxxxviii.visual_keybinder.config.ClientConfig;
import com.gmail.rawlxxxviii.visual_keybinder.util.FileUtil;
import com.gmail.rawlxxxviii.visual_keybinder.widget.KeyBindingList;
import com.gmail.rawlxxxviii.visual_keybinder.widget.KeyButton;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlternativeKeybindScreen extends Screen {

    public static final int CATEGORY_COLOR = 0xFF8FB2EC;
    public static final int ACTIVE_COLOR = 0xFF1FB0FF;
    public static final int DANGER_COLOR = 0xFFE66060;
    public static final int CONFLICT_COLOR = 0xFFF3A427;
    public static final int RESET_COLOR = 0xFFA1C87B;
    public static final int UNBOUND_COLOR = 0xFF8E959A;
    public static final int LIST_TITLE_BACKGROUND_COLOR = 0x9E000000;

    public static final int KEY_BUTTON_WIDTH = 16;
    public static final int WIDE_KEY_BUTTON_WIDTH = 60;
    public static final int KEY_BUTTON_HEIGHT = 16;

    private static final int PAGE_PADDING_TOP = 30;
    private static final int FILTERS_HEIGHT = 25;
    private static final int PAGE_PADDING_BOTTOM = 35;
    private static final int PAGE_PADDING_RIGHT = 5;
    private static final int PAGE_PADDING_LEFT = 5;
    private static final int PAGE_MID_GAP_HORIZONTAL = 5;
    private static final int PAGE_MID_GAP_VERTICAL = 10;

    private final Screen parentScreen;
    private final Options options;

    private KeyBindingList allBindingsList;
    private KeyBindingList detailsList;

    private EditBox keyMappingNameFilterEditBox;
    private EditBox keyMappingCategoryFilterEditBox;

    private final List<KeyBoardLayout> keyboardLayouts = new ArrayList<>();
    private KeyBoardLayout activeKeyboardLayout;
    private int activeKeyboardLayoutIndex = 0;
    private final List<KeyButton> keyButtons = new ArrayList<>();

    private Button rotateLayoutButton;

    private int layoutTop = 30;
    private int layoutLeft = 30;
    private int layoutWidth = 30;
    private int layoutHeight = 30;

    private int detailsListTop = 30;
    private int detailsListLeft = 30;
    private int detailsListWidth = 30;
    private int detailsListHeight = 30;

    private int allListTop = 30;
    private int allListLeft = 30;
    private int allListWidth = 30;
    private int allListHeight = 30;

    private KeyMapping keyMappingToChange;

    public AlternativeKeybindScreen(Screen parentScreen, Options options) {
        super(Component.translatable("raws_visual_keybinder.screen.title"));
        this.parentScreen = parentScreen;
        this.options = options;
        VisualKeybinder.LOGGER.info("[visual_keybinder] Creating the keybinder screen");
        getKeyboardLayouts();
    }

    public Options getOptions() {
        return options;
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
        // Keep the vanilla world visible, do not blur the background.
    }

    @Override
    protected void repositionElements() {
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        int top = PAGE_PADDING_TOP;
        int bottomBar = PAGE_PADDING_BOTTOM;
        int availableHeight = this.height - top - bottomBar - PAGE_MID_GAP_VERTICAL;

        int upperHeight = (int) (availableHeight * 0.58);
        detailsListHeight = upperHeight;
        layoutHeight = availableHeight - upperHeight;

        detailsListTop = top;
        detailsListLeft = PAGE_PADDING_LEFT;
        detailsListWidth = (int) (this.width * 0.45 - PAGE_PADDING_LEFT - PAGE_PADDING_RIGHT);

        allListLeft = detailsListLeft + detailsListWidth + PAGE_MID_GAP_HORIZONTAL;
        allListWidth = this.width - allListLeft - PAGE_PADDING_RIGHT;

        layoutLeft = PAGE_PADDING_LEFT;
        layoutWidth = this.width - PAGE_PADDING_RIGHT - layoutLeft;
        layoutTop = detailsListTop + upperHeight + PAGE_MID_GAP_VERTICAL;

        allListTop = top + FILTERS_HEIGHT;
        allListHeight = upperHeight - FILTERS_HEIGHT;

        this.detailsList = addRenderableWidget(new KeyBindingList(this, options, KeyBindingList.MODE_DETAIL,
                detailsListLeft, detailsListTop, detailsListWidth, detailsListHeight));

        this.allBindingsList = addRenderableWidget(new KeyBindingList(this, options, KeyBindingList.MODE_ALL,
                allListLeft, allListTop, allListWidth, allListHeight));

        this.keyMappingNameFilterEditBox = addRenderableWidget(new FilterEditBox(
                this.font,
                allListLeft,
                top,
                Math.max(40, allListWidth / 2 - 5),
                20,
                Component.translatable("raws_visual_keybinder.screen.filter_name")
        ));
        this.keyMappingNameFilterEditBox.setHint(Component.translatable("raws_visual_keybinder.screen.filter_name"));

        this.keyMappingCategoryFilterEditBox = addRenderableWidget(new FilterEditBox(
                this.font,
                allListLeft + allListWidth / 2,
                top,
                Math.max(40, allListWidth / 2),
                20,
                Component.translatable("raws_visual_keybinder.screen.filter_category")
        ));
        this.keyMappingCategoryFilterEditBox.setHint(Component.translatable("raws_visual_keybinder.screen.filter_category"));

        this.keyMappingNameFilterEditBox.setResponder(allBindingsList::setNameFilter);
        this.keyMappingCategoryFilterEditBox.setResponder(allBindingsList::setCategoryFilter);
        this.keyMappingCategoryFilterEditBox.setTextColor(CATEGORY_COLOR);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 75, this.height - 26, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("raws_visual_keybinder.screen.presets").withStyle(ChatFormatting.WHITE),
                        button -> this.minecraft.setScreen(new PresetsScreen(this, options)))
                .bounds(5, this.height - 26, 100, 20)
                .build());

        this.rotateLayoutButton = addRenderableWidget(Button.builder(Component.empty(),
                        button -> setActiveKeyboardLayout(getActiveKeyboardLayoutIndex() + 1))
                .bounds(this.width - 205, this.height - 26, 200, 20)
                .build());

        setActiveKeyboardLayout(activeKeyboardLayoutIndex);

        if (this.detailsList.getSelectedKey() != null) {
            this.detailsList.setSelectedKey(this.detailsList.getSelectedKey());
        }
        VisualKeybinder.LOGGER.info("[visual_keybinder] Keybinder screen initialised with {} layouts", keyboardLayouts.size());
    }

    public int getLayoutLeft() {
        return layoutLeft;
    }

    public int getLayoutTop() {
        return layoutTop;
    }

    public int getLayoutWidth() {
        return layoutWidth;
    }

    public int getLayoutHeight() {
        return layoutHeight;
    }

    public int getActiveKeyboardLayoutIndex() {
        return activeKeyboardLayoutIndex;
    }

    private void getKeyboardLayouts() {
        this.keyboardLayouts.clear();
        try {
            this.keyboardLayouts.addAll(FileUtil.getKeyboardLayoutsFromFile());
        } catch (Throwable throwable) {
            VisualKeybinder.LOGGER.error("[visual_keybinder] Could not read keyboard layouts", throwable);
        }
    }

    public KeyBoardLayout getActiveKeyboardLayout() {
        return activeKeyboardLayout;
    }

    public void setActiveKeyboardLayout(int index) {
        if (keyboardLayouts.isEmpty()) {
            this.activeKeyboardLayout = null;
            this.activeKeyboardLayoutIndex = index;
            updateRotateButtonMessage();
            return;
        }
        if (index >= keyboardLayouts.size()) {
            index = 0;
        } else if (index < 0) {
            index = keyboardLayouts.size() - 1;
        }
        this.activeKeyboardLayout = keyboardLayouts.get(index);
        this.activeKeyboardLayoutIndex = index;
        createLayoutButtons(this.activeKeyboardLayout);
        updateRotateButtonMessage();
    }

    private void updateRotateButtonMessage() {
        if (this.rotateLayoutButton == null) {
            return;
        }
        if (activeKeyboardLayout == null) {
            this.rotateLayoutButton.setMessage(Component.translatable(keyboardLayouts.isEmpty() ? "raws_visual_keybinder.screen.no_layouts" : "raws_visual_keybinder.screen.select_layout"));
        } else {
            this.rotateLayoutButton.setMessage(Component.literal(activeKeyboardLayout.getName().getString())
                    .append(" " + (getActiveKeyboardLayoutIndex() + 1) + "/" + keyboardLayouts.size()));
        }
    }

    private void createLayoutButtons(KeyBoardLayout keyboardLayout) {
        keyButtons.forEach(this::removeWidget);
        keyButtons.clear();

        for (KeyboardLayoutKey item : keyboardLayout.getKeyboardLayoutKeys()) {
            int x = layoutLeft + item.getX()
                    + (layoutWidth / 2 - keyboardLayout.getWidth() / 2)
                    - keyboardLayout.getMinX();
            int y = layoutTop + item.getY()
                    + (layoutHeight / 2 - keyboardLayout.getHeight() / 2)
                    - keyboardLayout.getMinY();

            KeyButton button = new KeyButton(this, keyboardLayout, item, x, y, () -> onKeyButtonPressed(item));
            keyButtons.add(addRenderableWidget(button));
        }
    }

    private void onKeyButtonPressed(KeyboardLayoutKey item) {
        if (detailsList != null && detailsList.getSelectedKey() != null
                && detailsList.getSelectedKey().getKey().equals(item.getKey())) {
            detailsList.setSelectedKey(null);
            return;
        }
        detailsList.setSelectedKey(item);
    }

    public void beginEditing(KeyMapping keyMapping) {
        this.keyMappingToChange = keyMapping;
        this.detailsList.rebuild();
        this.allBindingsList.rebuild();
    }

    public KeyMapping getKeyMappingToChange() {
        return keyMappingToChange;
    }

    public void setKeyMappingToChange(KeyMapping keyMappingToChange) {
        this.keyMappingToChange = keyMappingToChange;
    }

    public KeyBindingList getDetailsList() {
        return detailsList;
    }

    public void onBindingsChanged() {
        if (this.detailsList != null) {
            this.detailsList.rebuild();
        }
        if (this.allBindingsList != null) {
            this.allBindingsList.rebuild();
        }
    }

    public KeyMapping[] getKeyMappings(KeyboardLayoutKey key) {
        String name = key.getKey().getName();
        return Arrays.stream(options.keyMappings)
                .filter(keyMapping -> keyMapping.saveString().equals(name))
                .toArray(KeyMapping[]::new);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.keyMappingToChange != null) {
            if (event.key() == 256) {
                this.keyMappingToChange = null;
            } else {
                this.keyMappingToChange.setKey(InputConstants.Type.KEYSYM.getOrCreate(event.key()));
                this.keyMappingToChange = null;
                options.save();
                KeyMapping.resetMapping();
            }
            onBindingsChanged();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.keyMappingToChange != null) {
            this.keyMappingToChange.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            this.keyMappingToChange = null;
            options.save();
            KeyMapping.resetMapping();
            onBindingsChanged();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, this.height - 30, this.width, this.height, 0x78000000, 0x78000000);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

        if (ClientConfig.displayLayoutButtonTooltips && this.keyMappingToChange == null) {
            for (KeyButton keyButton : keyButtons) {
                if (keyButton.isMouseOver(mouseX, mouseY)) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(keyButton.getKeyboardLayoutKey().getKey().getDisplayName());
                    KeyMapping[] keyMappings = keyButton.getKeymappings();
                    if (keyMappings.length == 0) {
                        tooltip.add(Component.translatable("raws_visual_keybinder.screen.no_bindings").withStyle(ChatFormatting.DARK_GRAY));
                    } else {
                        tooltip.add(Component.literal(""));
                        for (KeyMapping keyMapping : keyMappings) {
                            tooltip.add(Component.literal(" - ")
                                    .append(Component.translatable(keyMapping.getName()))
                                    .withStyle(ChatFormatting.GRAY));
                        }
                    }
                    graphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                }
            }
        }

        if (this.keyMappingToChange != null) {
            Component hint = Component.translatable("raws_visual_keybinder.screen.editing_hint");
            String text = hint.getString();
            int textWidth = this.font.width(text);
            int x = this.width / 2 - textWidth / 2;
            int y = this.height / 2 - 4;
            graphics.fill(x - 6, y - 6, x + textWidth + 6, y + 14, 0xE0101010);
            graphics.outline(x - 6, y - 6, textWidth + 12, 20, ACTIVE_COLOR);
            graphics.text(this.font, text, x, y, ACTIVE_COLOR, false);
        }
    }

    @Nullable
    public Screen getParentScreen() {
        return parentScreen;
    }

    public static String modId() {
        return VisualKeybinder.MOD_ID;
    }
}
