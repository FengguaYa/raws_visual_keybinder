package com.gmail.rawlxxxviii.visual_keybinder;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class FilterEditBox extends EditBox {

    public FilterEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            return super.mouseClicked(event, doubleClick);
        } else if (event.button() == 1) {
            if (event.x() >= (double) getX() && event.x() < (double) (getX() + width)
                    && event.y() >= (double) getY() && event.y() < (double) (getY() + height)) {
                setValue("");
                return true;
            }
        }
        return false;
    }
}
