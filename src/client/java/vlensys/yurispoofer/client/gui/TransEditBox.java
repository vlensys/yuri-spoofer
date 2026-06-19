package vlensys.yurispoofer.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

// editbox with a translucent background so the gui backdrop shows through
class TransEditBox extends EditBox {
    TransEditBox(Font font, int x, int y, int w, int h, Component hint) {
        super(font, x, y, w, h, hint);
        setBordered(false);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        g.fill(getX() - 2, getY() - 1, getX() + getWidth() + 2, getY() + getHeight() + 1, 0x73281E26);
        super.extractWidgetRenderState(g, mouseX, mouseY, partial);
    }
}
