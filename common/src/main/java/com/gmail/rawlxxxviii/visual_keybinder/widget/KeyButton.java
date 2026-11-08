package com.gmail.rawlxxxviii.visual_keybinder.widget;

import com.gmail.rawlxxxviii.visual_keybinder.KeyBoardLayout;
import com.gmail.rawlxxxviii.visual_keybinder.KeyboardLayoutKey;
import com.gmail.rawlxxxviii.visual_keybinder.VisualKeybinder;
import com.gmail.rawlxxxviii.visual_keybinder.screen.AlternativeKeybindScreen;
import com.gmail.rawlxxxviii.visual_keybinder.util.KeyUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * One key of the visual keyboard. Uses the original textures (the button sprite
 * plus the {@code gui.png} overlay) so the port keeps the look of the 1.21 mod.
 */
public class KeyButton extends ImageButton {

    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(VisualKeybinder.MOD_ID, "textures/gui/gui.png");

    private static final WidgetSprites KEY_BUTTON_WIDGET_SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(VisualKeybinder.MOD_ID, "key_button"),
            Identifier.fromNamespaceAndPath(VisualKeybinder.MOD_ID, "key_button_highlighted"));

    private static final WidgetSprites KEY_BUTTON_WIDGET_SPRITES_WIDE = new WidgetSprites(
            Identifier.fromNamespaceAndPath(VisualKeybinder.MOD_ID, "key_button_wide"),
            Identifier.fromNamespaceAndPath(VisualKeybinder.MOD_ID, "key_button_wide_highlighted"));

    private final AlternativeKeybindScreen parentScreen;
    private final KeyBoardLayout keyBoardLayout;
    private final KeyboardLayoutKey keyboardLayoutKey;

    public KeyButton(AlternativeKeybindScreen parentScreen, KeyBoardLayout keyBoardLayout,
                     KeyboardLayoutKey keyboardLayoutKey, int x, int y, Runnable onPress) {
        super(
                x, y,
                keyboardLayoutKey.isWide() ? AlternativeKeybindScreen.WIDE_KEY_BUTTON_WIDTH : AlternativeKeybindScreen.KEY_BUTTON_WIDTH,
                AlternativeKeybindScreen.KEY_BUTTON_HEIGHT,
                keyboardLayoutKey.isWide() ? KEY_BUTTON_WIDGET_SPRITES_WIDE : KEY_BUTTON_WIDGET_SPRITES,
                button -> onPress.run(),
                keyboardLayoutKey.getKey().getDisplayName()
        );

        this.parentScreen = parentScreen;
        this.keyBoardLayout = keyBoardLayout;
        this.keyboardLayoutKey = keyboardLayoutKey;
    }

    public KeyboardLayoutKey getKeyboardLayoutKey() {
        return keyboardLayoutKey;
    }

    public KeyMapping[] getKeymappings() {
        return parentScreen.getKeyMappings(keyboardLayoutKey);
    }

    public boolean isEmpty() {
        return getKeymappings().length < 1;
    }

    public boolean hasConflict() {
        return KeyUtil.hasConflict(getKeymappings());
    }

    private float getScale() {
        float minScale = 0.3f;
        float maxScale = 1.2f;

        float scaleX = Math.max(minScale, Math.min(maxScale,
                (float) parentScreen.getLayoutWidth() / (float) keyBoardLayout.getWidth()));
        float scaleY = Math.max(minScale, Math.min(maxScale,
                (float) parentScreen.getLayoutHeight() / (float) keyBoardLayout.getHeight()));
        return Math.min(scaleX, scaleY);
    }

    private float getDiffX() {
        float scale = getScale();
        float centerXOrigin = parentScreen.getLayoutLeft() + parentScreen.getLayoutWidth() / 2f;
        return centerXOrigin - centerXOrigin * scale;
    }

    private float getDiffY() {
        float scale = getScale();
        float centerYOrigin = parentScreen.getLayoutTop() + parentScreen.getLayoutHeight() / 2f;
        return centerYOrigin - centerYOrigin * scale;
    }

    private boolean checkIsMouseOver(double mouseX, double mouseY) {
        float scale = getScale();
        float diffX = getDiffX();
        float diffY = getDiffY();
        return mouseX >= this.getX() * scale + diffX
                && mouseY >= this.getY() * scale + diffY
                && mouseX < (this.getX() + this.width) * scale + diffX
                && mouseY < (this.getY() + this.height) * scale + diffY;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return checkIsMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || !this.active || !this.visible) {
            return false;
        }
        if (!checkIsMouseOver(event.x(), event.y())) {
            return false;
        }
        this.onPress(event);
        return true;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float scale = getScale();
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(getDiffX(), getDiffY());
        pose.scale(scale, scale);

        super.extractContents(graphics, mouseX, mouseY, partialTick);
        renderOverlay(graphics);

        pose.popMatrix();
    }

    private void renderOverlay(GuiGraphicsExtractor graphics) {
        boolean empty = isEmpty();
        boolean conflict = hasConflict();

        int canvasX = keyboardLayoutKey.isWide() ? 120 : 0;
        int u = empty ? canvasX + 3 * this.width : conflict ? canvasX + this.width : canvasX + 2 * this.width;
        int v = this.isHoveredOrFocused() ? AlternativeKeybindScreen.KEY_BUTTON_HEIGHT : 0;

        graphics.blit(GUI_TEXTURE, getX(), getY(), this.width, this.height, u, v, 512, 512);

        boolean selected = parentScreen.getDetailsList() != null
                && parentScreen.getDetailsList().getSelectedKey() != null
                && parentScreen.getDetailsList().getSelectedKey().getKey().equals(keyboardLayoutKey.getKey());
        if (selected) {
            graphics.blit(GUI_TEXTURE, getX(), getY(), this.width, this.height,
                    canvasX + 4 * this.width, 0, 512, 512);
        }

        Font font = Minecraft.getInstance().font;
        int color = empty ? 0xFF808080 : conflict ? AlternativeKeybindScreen.CONFLICT_COLOR : 0xFFFFFFFF;
        graphics.centeredText(font, getMessage(),
                getX() + this.width / 2, getY() + (this.height - 8) / 2, color);
    }
}
