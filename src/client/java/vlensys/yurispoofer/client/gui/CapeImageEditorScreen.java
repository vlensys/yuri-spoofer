package vlensys.yurispoofer.client.gui;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import vlensys.yurispoofer.client.spoof.CapeSpoofer;
import vlensys.yurispoofer.client.spoof.SpoofConfig;

// cape image editor
public class CapeImageEditorScreen extends Screen {
    private static final SpoofConfig CONFIG = SpoofConfig.INSTANCE;
    private static final int CANVAS_W = CapeSpoofer.CAPE_WIDTH;
    private static final int CANVAS_H = CapeSpoofer.CAPE_HEIGHT;

    private static final int BG_TOP = 0xE00E0E14;
    private static final int BG_BOT = 0xF0060609;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_HI = 0xFFFFFFFF;
    private static final int PANEL_SH = 0xFF545459;
    private static final int DARK = 0xFF1B1B20;
    private static final int GRID = 0x335C5C63;
    private static final int TEXT_LT = 0xFFF0F0F2;
    private static final int TEXT_MUTE = 0xFF9A9AA0;
    private static final int WARN = 0xFFFFD166;

    private final String sourcePath;
    private Identifier sourceTexture;
    private int sourceW = 0, sourceH = 0;
    private int editX = 0, editY = 0, editW = CANVAS_W, editH = CANVAS_H;
    private String status = "";
    private boolean loaded = false;

    private EditBox xBox, yBox, wBox, hBox;

    public CapeImageEditorScreen(String sourcePath) {
        super(Component.literal("cape image editor"));
        this.sourcePath = sourcePath;
    }

    @Override
    protected void init() {
        if (!loaded) loadSource();

        int controlY = Math.min(height - 58, canvasY() + canvasScale() * CANVAS_H + 20);
        int x = Math.max(12, (width - 308) / 2);
        xBox = field(x + 16, controlY, 44, "x", editX);
        yBox = field(x + 76, controlY, 44, "y", editY);
        wBox = field(x + 136, controlY, 50, "w", editW);
        hBox = field(x + 202, controlY, 50, "h", editH);

        addRenderableWidget(Button.builder(Component.literal("fit"), b -> { fitSource(); syncBoxes(); })
            .pos(x, controlY + 24).size(44, 18).build());
        addRenderableWidget(Button.builder(Component.literal("center"), b -> { centerFrame(); syncBoxes(); })
            .pos(x + 48, controlY + 24).size(54, 18).build());
        addRenderableWidget(Button.builder(Component.literal("64 x 32"), b -> { useRecommended(); syncBoxes(); })
            .pos(x + 106, controlY + 24).size(62, 18).build());
        addRenderableWidget(Button.builder(Component.literal("save"), b -> saveCape())
            .pos(x + 172, controlY + 24).size(54, 18).build());
        addRenderableWidget(Button.builder(Component.literal("cancel"), b -> minecraft.setScreen(new AppearanceEditorScreen()))
            .pos(x + 230, controlY + 24).size(64, 18).build());
    }

    private EditBox field(int x, int y, int w, String name, int value) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal(name));
        box.setMaxLength(5);
        box.setValue(String.valueOf(value));
        addRenderableWidget(box);
        return box;
    }

    private void loadSource() {
        try {
            NativeImage img;
            try (InputStream input = Files.newInputStream(Path.of(sourcePath))) {
                img = NativeImage.read(input);
            }
            sourceW = img.getWidth();
            sourceH = img.getHeight();
            sourceTexture = Identifier.fromNamespaceAndPath("yuri-spoofer", "cape/editor/" + Integer.toHexString(sourcePath.hashCode()));
            minecraft.getTextureManager().register(sourceTexture, new DynamicTexture(() -> "yuri-cape-editor", img));
            fitSource();
            if (sourceW > CANVAS_W || sourceH > CANVAS_H) {
                status = "image too big, use recommended 64 x 32?";
            } else {
                status = "recommended 64 x 32";
            }
        } catch (Exception e) {
            status = "could not open image";
        }
        loaded = true;
    }

    private void fitSource() {
        if (sourceW <= 0 || sourceH <= 0) return;
        double scale = Math.min((double) CANVAS_W / sourceW, (double) CANVAS_H / sourceH);
        editW = Math.max(1, (int) Math.round(sourceW * scale));
        editH = Math.max(1, (int) Math.round(sourceH * scale));
        centerFrame();
    }

    private void centerFrame() {
        editX = (CANVAS_W - editW) / 2;
        editY = (CANVAS_H - editH) / 2;
    }

    private void useRecommended() {
        editX = 0;
        editY = 0;
        editW = CANVAS_W;
        editH = CANVAS_H;
    }

    private void syncBoxes() {
        xBox.setValue(String.valueOf(editX));
        yBox.setValue(String.valueOf(editY));
        wBox.setValue(String.valueOf(editW));
        hBox.setValue(String.valueOf(editH));
    }

    private void readBoxes() {
        editX = parseBox(xBox, editX, -4096, 4096);
        editY = parseBox(yBox, editY, -4096, 4096);
        editW = parseBox(wBox, editW, 1, 4096);
        editH = parseBox(hBox, editH, 1, 4096);
    }

    private int parseBox(EditBox box, int fallback, int min, int max) {
        try {
            int v = Integer.parseInt(box.getValue().trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void saveCape() {
        readBoxes();
        String id = CapeSpoofer.INSTANCE.importCapeEdited(sourcePath, editX, editY, editW, editH);
        if (id == null) {
            status = "could not save cape";
            return;
        }
        CONFIG.setCapeId(id);
        CONFIG.setSpoofCape(true);
        minecraft.setScreen(new AppearanceEditorScreen());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        readBoxes();
        g.fillGradient(0, 0, width, height, BG_TOP, BG_BOT);

        g.text(font, "cape image editor", 14, 14, TEXT_LT, true);
        String dims = sourceW > 0 ? sourceW + " x " + sourceH + " source" : "";
        g.text(font, dims, width - 14 - font.width(dims), 15, TEXT_MUTE, true);
        g.text(font, status, 14, 30, status.startsWith("image too big") ? WARN : TEXT_MUTE, false);

        renderCanvas(g);
        super.extractRenderState(g, mouseX, mouseY, partial);
        labelFields(g);
    }

    private void renderCanvas(GuiGraphicsExtractor g) {
        int s = canvasScale();
        int x = canvasX();
        int y = canvasY();
        int w = CANVAS_W * s;
        int h = CANVAS_H * s;
        inset(g, x - 2, y - 2, w + 4, h + 4, DARK);

        if (sourceTexture != null && editW > 0 && editH > 0) {
            g.enableScissor(x, y, x + w, y + h);
            g.blit(
                RenderPipelines.GUI_TEXTURED,
                sourceTexture,
                x + editX * s,
                y + editY * s,
                0f,
                0f,
                editW * s,
                editH * s,
                sourceW,
                sourceH,
                sourceW,
                sourceH
            );
            g.disableScissor();
        }

        for (int gx = 0; gx <= CANVAS_W; gx += 8) g.fill(x + gx * s, y, x + gx * s + 1, y + h, GRID);
        for (int gy = 0; gy <= CANVAS_H; gy += 8) g.fill(x, y + gy * s, x + w, y + gy * s + 1, GRID);
        outline(g, x, y, w, h, PANEL_HI);
    }

    private void labelFields(GuiGraphicsExtractor g) {
        if (xBox == null) return;
        g.text(font, "x", xBox.getX() - 10, xBox.getY() + 5, TEXT_MUTE, false);
        g.text(font, "y", yBox.getX() - 10, yBox.getY() + 5, TEXT_MUTE, false);
        g.text(font, "w", wBox.getX() - 12, wBox.getY() + 5, TEXT_MUTE, false);
        g.text(font, "h", hBox.getX() - 10, hBox.getY() + 5, TEXT_MUTE, false);
    }

    private int canvasScale() {
        int maxW = Math.max(2, (width - 40) / CANVAS_W);
        int maxH = Math.max(2, (height - 145) / CANVAS_H);
        return Math.max(2, Math.min(8, Math.min(maxW, maxH)));
    }

    private int canvasX() {
        return (width - CANVAS_W * canvasScale()) / 2;
    }

    private int canvasY() {
        return 52;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new AppearanceEditorScreen());
    }

    private static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, int face) {
        g.fill(x, y, x + w, y + h, face);
        g.fill(x, y, x + w, y + 1, PANEL_SH);
        g.fill(x, y, x + 1, y + h, PANEL_SH);
        g.fill(x + w - 1, y, x + w, y + h, PANEL_HI);
        g.fill(x, y + h - 1, x + w, y + h, PANEL_HI);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }
}
