package com.gmail.rawlxxxviii.visual_keybinder.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Minimal scrollable list widget. The vanilla selection lists changed a lot in
 * 26.1, so the mod ships its own tiny implementation to stay loader independent.
 */
public abstract class ScrollList extends AbstractWidget {

    protected double scrollAmount;
    protected int rowHeight = 20;

    protected ScrollList(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.active = true;
    }

    protected abstract int rowCount();

    protected abstract void renderRow(GuiGraphicsExtractor graphics, int index, int rowTop,
                                      int mouseX, int mouseY, float partialTick);

    protected abstract boolean clickRow(int index, MouseButtonEvent event);

    protected double maxScroll() {
        return Math.max(0, rowCount() * (double) rowHeight - this.height);
    }

    public void resetScroll() {
        this.scrollAmount = 0;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0x80000000);

        graphics.enableScissor(getX(), getY(), getX() + this.width, getY() + this.height);
        int first = Math.max(0, (int) (scrollAmount / rowHeight));
        for (int i = first; i < rowCount(); i++) {
            int top = getY() + i * rowHeight - (int) scrollAmount;
            if (top >= getY() + this.height) {
                break;
            }
            if (top + rowHeight <= getY()) {
                continue;
            }
            renderRow(graphics, i, top, mouseX, mouseY, partialTick);
        }
        graphics.disableScissor();

        double max = maxScroll();
        if (max > 0) {
            int barHeight = Math.max(12, (int) (this.height * (double) this.height / (rowCount() * (double) rowHeight)));
            int barY = getY() + (int) ((this.height - barHeight) * (scrollAmount / max));
            int barX = getX() + this.width - 6;
            graphics.fill(barX, barY, barX + 4, barY + barHeight, 0xFFC0C0C0);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active || !this.visible || event.button() != 0) {
            return false;
        }
        if (!isMouseOver(event.x(), event.y())) {
            return false;
        }
        int index = (int) ((event.y() - getY() + scrollAmount) / rowHeight);
        if (index < 0 || index >= rowCount()) {
            return false;
        }
        return clickRow(index, event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.active || !this.visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scrollAmount = Math.max(0, Math.min(maxScroll(), this.scrollAmount - scrollY * rowHeight));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
