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
    private static final int PANEL_W = CapeSpoofer.PANEL_WIDTH;
    private static final int PANEL_H = CapeSpoofer.PANEL_HEIGHT;

    private static final int BG_TOP = 0xE00E0E14;
    private static final int BG_BOT = 0xF0060609;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_HI = 0xFFFFFFFF;
    private static final int PANEL_SH = 0xFF545459;
    private static final int DARK = 0xFF1B1B20;
    private static final int GRID = 0x335C5C63;
    private static final int TEXT_LT = 0xFFF0F0F2;
    private static final int TEXT_MUTE = 0xFF9A9AA0;

    private final String sourcePath;
    private Identifier sourceTexture;
    private int rawW = 0, rawH = 0, sourceW = 0, sourceH = 0;
    private int targetScale = 1;
    private int panelW = PANEL_W, panelH = PANEL_H;
    private int editX = 0, editY = 0, editW = PANEL_W, editH = PANEL_H;
    private int rotation = 0;
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

        int controlY = Math.min(height - 58, canvasY() + canvasHeight() + 18);
        int x = Math.max(12, (width - 362) / 2);
        xBox = field(x + 16, controlY, 50, "x", editX);
        yBox = field(x + 82, controlY, 50, "y", editY);
        wBox = field(x + 148, controlY, 56, "w", editW);
        hBox = field(x + 220, controlY, 56, "h", editH);

        addRenderableWidget(Button.builder(Component.literal("fit"), b -> { fitSource(); syncBoxes(); })
            .pos(x, controlY + 24).size(44, 18).build());
        addRenderableWidget(Button.builder(Component.literal("center"), b -> { centerFrame(); syncBoxes(); })
            .pos(x + 48, controlY + 24).size(54, 18).build());
        addRenderableWidget(Button.builder(Component.literal("full"), b -> { fillPanel(); syncBoxes(); })
            .pos(x + 106, controlY + 24).size(44, 18).build());
        addRenderableWidget(Button.builder(Component.literal("rotate"), b -> rotateImage())
            .pos(x + 154, controlY + 24).size(58, 18).build());
        addRenderableWidget(Button.builder(Component.literal("save"), b -> saveCape())
            .pos(x + 216, controlY + 24).size(54, 18).build());
        addRenderableWidget(Button.builder(Component.literal("cancel"), b -> minecraft.setScreen(new AppearanceEditorScreen()))
            .pos(x + 274, controlY + 24).size(64, 18).build());
    }

    private EditBox field(int x, int y, int w, String name, int value) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal(name));
        box.setMaxLength(7);
        box.setValue(String.valueOf(value));
        addRenderableWidget(box);
        return box;
    }

    private void loadSource() {
        try {
            refreshTexture();
            targetScale = recommendedScale();
            panelW = PANEL_W * targetScale;
            panelH = PANEL_H * targetScale;
            fitSource();
            refreshStatus();
        } catch (Exception e) {
            status = "could not open image";
        }
        loaded = true;
    }

    private void refreshTexture() throws Exception {
        NativeImage img;
        try (InputStream input = Files.newInputStream(Path.of(sourcePath))) {
            img = NativeImage.read(input);
        }
        rawW = img.getWidth();
        rawH = img.getHeight();
        NativeImage preview = rotate(img, rotation);
        img.close();
        sourceW = preview.getWidth();
        sourceH = preview.getHeight();
        sourceTexture = Identifier.fromNamespaceAndPath("yuri-spoofer", "cape/editor/" + Integer.toHexString(sourcePath.hashCode()) + "/" + rotation);
        minecraft.getTextureManager().register(sourceTexture, new DynamicTexture(() -> "yuri-cape-editor", preview));
    }

    private int recommendedScale() {
        int rw = Math.max(1, sourceW);
        int rh = Math.max(1, sourceH);
        int scale = (int) Math.ceil(Math.max((double) rw / PANEL_W, (double) rh / PANEL_H));
        return Math.max(1, Math.min(CapeSpoofer.MAX_SCALE, scale));
    }

    private void refreshStatus() {
        int outW = CapeSpoofer.SHEET_WIDTH * targetScale;
        int outH = CapeSpoofer.SHEET_HEIGHT * targetScale;
        status = "high res cape, saves " + outW + " x " + outH + ", panel " + panelW + " x " + panelH;
    }

    private void fitSource() {
        if (sourceW <= 0 || sourceH <= 0) return;
        double scale = Math.min((double) panelW / sourceW, (double) panelH / sourceH);
        editW = Math.max(1, (int) Math.round(sourceW * scale));
        editH = Math.max(1, (int) Math.round(sourceH * scale));
        centerFrame();
    }

    private void centerFrame() {
        editX = (panelW - editW) / 2;
        editY = (panelH - editH) / 2;
    }

    private void fillPanel() {
        editX = 0;
        editY = 0;
        editW = panelW;
        editH = panelH;
    }

    private void rotateImage() {
        rotation = (rotation + 1) & 3;
        try {
            refreshTexture();
            targetScale = recommendedScale();
            panelW = PANEL_W * targetScale;
            panelH = PANEL_H * targetScale;
            fitSource();
            syncBoxes();
            refreshStatus();
        } catch (Exception e) {
            status = "could not rotate image";
        }
    }

    private void syncBoxes() {
        xBox.setValue(String.valueOf(editX));
        yBox.setValue(String.valueOf(editY));
        wBox.setValue(String.valueOf(editW));
        hBox.setValue(String.valueOf(editH));
    }

    private void readBoxes() {
        editX = parseBox(xBox, editX, -262144, 262144);
        editY = parseBox(yBox, editY, -262144, 262144);
        editW = parseBox(wBox, editW, 1, 262144);
        editH = parseBox(hBox, editH, 1, 262144);
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
        String id = CapeSpoofer.INSTANCE.importCapeEdited(sourcePath, editX, editY, editW, editH, targetScale, rotation);
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
        String dims = rawW > 0 ? rawW + " x " + rawH + " source, rot " + rotation * 90 : "";
        g.text(font, dims, width - 14 - font.width(dims), 15, TEXT_MUTE, true);
        g.text(font, status, 14, 30, TEXT_MUTE, false);

        renderCanvas(g);
        super.extractRenderState(g, mouseX, mouseY, partial);
        labelFields(g);
    }

    private void renderCanvas(GuiGraphicsExtractor g) {
        double s = canvasScale();
        int x = canvasX();
        int y = canvasY();
        int w = canvasWidth();
        int h = canvasHeight();
        inset(g, x - 2, y - 2, w + 4, h + 4, DARK);

        if (sourceTexture != null && editW > 0 && editH > 0) {
            g.enableScissor(x, y, x + w, y + h);
            g.blit(
                RenderPipelines.GUI_TEXTURED,
                sourceTexture,
                x + (int) Math.round(editX * s),
                y + (int) Math.round(editY * s),
                0f,
                0f,
                Math.max(1, (int) Math.round(editW * s)),
                Math.max(1, (int) Math.round(editH * s)),
                sourceW,
                sourceH,
                sourceW,
                sourceH
            );
            g.disableScissor();
        }

        int gridStep = Math.max(1, targetScale);
        for (int gx = 0; gx <= panelW; gx += gridStep) {
            int px = x + (int) Math.round(gx * s);
            g.fill(px, y, px + 1, y + h, GRID);
        }
        for (int gy = 0; gy <= panelH; gy += gridStep) {
            int py = y + (int) Math.round(gy * s);
            g.fill(x, py, x + w, py + 1, GRID);
        }
        outline(g, x, y, w, h, PANEL_HI);
    }

    private void labelFields(GuiGraphicsExtractor g) {
        if (xBox == null) return;
        g.text(font, "x", xBox.getX() - 10, xBox.getY() + 5, TEXT_MUTE, false);
        g.text(font, "y", yBox.getX() - 10, yBox.getY() + 5, TEXT_MUTE, false);
        g.text(font, "w", wBox.getX() - 12, wBox.getY() + 5, TEXT_MUTE, false);
        g.text(font, "h", hBox.getX() - 10, hBox.getY() + 5, TEXT_MUTE, false);
    }

    private double canvasScale() {
        double maxW = (width - 80.0) / Math.max(1, panelW);
        double maxH = (height - 150.0) / Math.max(1, panelH);
        return Math.max(0.05, Math.min(16.0, Math.min(maxW, maxH)));
    }

    private int canvasWidth() {
        return Math.max(1, (int) Math.round(panelW * canvasScale()));
    }

    private int canvasHeight() {
        return Math.max(1, (int) Math.round(panelH * canvasScale()));
    }

    private int canvasX() {
        return (width - canvasWidth()) / 2;
    }

    private int canvasY() {
        return 52;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new AppearanceEditorScreen());
    }

    private static NativeImage rotate(NativeImage src, int rotation) {
        int rot = ((rotation % 4) + 4) % 4;
        if (rot == 0) {
            NativeImage out = new NativeImage(src.getWidth(), src.getHeight(), true);
            copy(src, out, 0);
            return out;
        }
        int rw = rot % 2 == 0 ? src.getWidth() : src.getHeight();
        int rh = rot % 2 == 0 ? src.getHeight() : src.getWidth();
        NativeImage out = new NativeImage(rw, rh, true);
        copy(src, out, rot);
        return out;
    }

    private static void copy(NativeImage src, NativeImage out, int rotation) {
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                out.setPixel(x, y, rotatedPixel(src, x, y, rotation));
            }
        }
    }

    private static int rotatedPixel(NativeImage img, int x, int y, int rotation) {
        return switch (rotation) {
            case 1 -> img.getPixel(y, img.getHeight() - 1 - x);
            case 2 -> img.getPixel(img.getWidth() - 1 - x, img.getHeight() - 1 - y);
            case 3 -> img.getPixel(img.getWidth() - 1 - y, x);
            default -> img.getPixel(x, y);
        };
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
