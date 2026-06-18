package vlensys.yurispoofer.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// inventory button
public class PaintbrushButton extends AbstractWidget {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("yuri-spoofer", "textures/gui/paintbrush.png");
    private static final int ICON_W = 605;
    private static final int ICON_H = 849;

    // palette
    private static final int HOVER = 0x22FFFFFF;

    private final Runnable action;

    public PaintbrushButton(int x, int y, int size, Runnable action) {
        super(x, y, size, size, Component.literal("Appearance editor"));
        this.action = action;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        action.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x = getX(), y = getY(), w = width, h = height;
        // minimal: just the brush, with a faint highlight on hover (no bevel box)
        if (isHovered()) g.fill(x, y, x + w, y + h, HOVER);
        int iconH = Math.min(h, h);
        int iconW = Math.max(1, iconH * ICON_W / ICON_H);
        int iconX = x + (w - iconW) / 2;
        int iconY = y + (h - iconH) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, ICON, iconX, iconY, 0f, 0f, iconW, iconH, ICON_W, ICON_H, ICON_W, ICON_H);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
