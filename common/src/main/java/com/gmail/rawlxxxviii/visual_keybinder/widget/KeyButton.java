package com.gmail.rawlxxxviii.visual_keybinder.widget;

import com.gmail.rawlxxxviii.visual_keybinder.KeyBoardLayout;
import com.gmail.rawlxxxviii.visual_keybinder.KeyboardLayoutKey;
import com.gmail.rawlxxxviii.visual_keybinder.screen.AlternativeKeybindScreen;
import com.gmail.rawlxxxviii.visual_keybinder.util.KeyUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * One key of the visual keyboard. The button is drawn manually so the whole
 * keyboard can be scaled with the 2D pose matrix of the new GUI renderer.
 */
public class KeyButton extends AbstractWidget {

    private final AlternativeKeybindScreen parentScreen;
    private final KeyBoardLayout keyBoardLayout;
    private final KeyboardLayoutKey keyboardLayoutKey;
    private final Runnable onPress;

    public KeyButton(AlternativeKeybindScreen parentScreen, KeyBoardLayout keyBoardLayout,
                     KeyboardLayoutKey keyboardLayoutKey, int x, int y, Runnable onPress) {
        super(x, y,
                keyboardLayoutKey.isWide() ? AlternativeKeybindScreen.WIDE_KEY_BUTTON_WIDTH : AlternativeKeybindScreen.KEY_BUTTON_WIDTH,
                AlternativeKeybindScreen.KEY_BUTTON_HEIGHT,
                keyboardLayoutKey.getKey().getDisplayName());
        this.parentScreen = parentScreen;
        this.keyBoardLayout = keyBoardLayout;
        this.keyboardLayoutKey = keyboardLayoutKey;
        this.onPress = onPress;
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
        this.onPress.run();
        return true;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.isHovered = checkIsMouseOver(mouseX, mouseY);

        boolean empty = isEmpty();
        boolean conflict = hasConflict();
        boolean selected = parentScreen.getDetailsList() != null
                && parentScreen.getDetailsList().getSelectedKey() != null
                && parentScreen.getDetailsList().getSelectedKey().getKey().equals(keyboardLayoutKey.getKey());

        int background = this.isHovered ? 0xFF4A4A4A : 0xFF2B2B2B;
        int border = selected ? 0xFF1FB0FF : conflict ? AlternativeKeybindScreen.CONFLICT_COLOR : 0xFF5A5A5A;
        int textColor = empty ? 0xFF8E959A : conflict ? AlternativeKeybindScreen.CONFLICT_COLOR : 0xFFFFFFFF;

        float scale = getScale();
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(getDiffX(), getDiffY());
        pose.scale(scale, scale);

        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, background);
        graphics.outline(getX(), getY(), this.width, this.height, border);
        graphics.centeredText(Minecraft.getInstance().font, getMessage(),
                getX() + this.width / 2, getY() + (this.height - 8) / 2, textColor);

        pose.popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
