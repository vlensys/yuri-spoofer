package vlensys.yurispoofer.client.gui;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import vlensys.yurispoofer.client.spoof.CapeSpoofer;
import vlensys.yurispoofer.client.spoof.SpoofConfig;

// cape image editor: a flat, warm crop canvas. scroll = zoom, drag = move; the
// cape-shaped frame is the crop region that gets saved.
public class CapeImageEditorScreen extends Screen {
    private static final SpoofConfig CONFIG = SpoofConfig.INSTANCE;
    private static final int PANEL_W = CapeSpoofer.PANEL_WIDTH;
    private static final int PANEL_H = CapeSpoofer.PANEL_HEIGHT;

    // warm-dark palette, matched to the appearance editor so the two screens feel like one app
    private static final int BG_TOP      = 0xF0100C0A;
    private static final int BG_BOT      = 0xF8080605;
    private static final int VIEW_BG     = 0xB0241A22;   // editing area (translucent warm)
    private static final int DIMMED      = 0x99000000;   // overlay over the cropped-away region
    private static final int FRAME       = 0xFFFFFFFF;   // crop frame (accent)
    private static final int GRID        = 0x3CFFFFFF;   // cape-pixel guide grid inside the frame
    private static final int LINE        = 0xCC5A4652;
    private static final int SURFACE     = 0x8C2A2028;
    private static final int SURFACE_HI  = 0xB23A2C36;
    private static final int TEXT        = 0xFFF2EFEA;
    private static final int TEXT_MUTE   = 0xFFB4AB9E;
    private static final int TEXT_FAINT  = 0xFF837A6E;
    private static final int BTN_SAVE    = 0xFF2F9E44;
    private static final int BTN_SAVE_HI = 0xFF37B24D;

    private static final Identifier BG_TEX =
        Identifier.fromNamespaceAndPath("yuri-spoofer", "textures/gui/background.png");

    private final String sourcePath;
    private Identifier sourceTexture;
    private int rawW = 0, rawH = 0, sourceW = 0, sourceH = 0;
    private int targetScale = 1;
    private int panelW = PANEL_W, panelH = PANEL_H;

    // image placement, in panel-space units (the same space importCapeEdited expects).
    // doubles for smooth zoom/pan; rounded on save.
    private double editX = 0, editY = 0, editW = PANEL_W, editH = PANEL_H;
    private int rotation = 0;
    private boolean loaded = false;
    private String error = null;

    // layout (recomputed in init / on resize)
    private int viewX, viewY, viewW, viewH;
    private int frameX, frameY, frameW, frameH;
    private double frameScale = 1.0;
    private float mx, my;

    // flat buttons
    private final List<int[]> btnRects = new ArrayList<>();
    private final List<String> btnLabels = new ArrayList<>();
    private final List<Runnable> btnActs = new ArrayList<>();
    private int saveBtnIndex = -1;

    public CapeImageEditorScreen(String sourcePath) {
        super(Component.literal("edit cape"));
        this.sourcePath = sourcePath;
    }

    @Override
    protected void init() {
        if (!loaded) loadSource();
        layout();

        btnRects.clear(); btnLabels.clear(); btnActs.clear(); saveBtnIndex = -1;
        addButtons();
    }

    private void addButtons() {
        String[] labels = { "fit", "fill", "rotate", "cancel", "save" };
        Runnable[] acts = {
            () -> { fit(); },
            () -> { fill(); },
            this::rotateImage,
            () -> minecraft.setScreen(new AppearanceEditorScreen()),
            this::saveCape,
        };
        int bw = 56, bh = 18, gap = 8;
        int groupW = labels.length * bw + (labels.length - 1) * gap;
        int sx = (width - groupW) / 2;
        int by = height - 16 - bh;
        for (int i = 0; i < labels.length; i++) {
            int x = sx + i * (bw + gap);
            btnRects.add(new int[]{ x, by, bw, bh });
            btnLabels.add(labels[i]);
            btnActs.add(acts[i]);
            if (labels[i].equals("save")) saveBtnIndex = i;
        }
    }

    private void layout() {
        int margin = 16, btnRowH = 24;
        viewX = margin;
        viewY = 36;
        viewW = width - margin * 2;
        viewH = height - viewY - margin - btnRowH - 6;
        if (viewH < 60) viewH = 60;

        // crop frame: cape panel aspect (10:16, portrait), centred, with breathing room
        // so the parts being cropped out stay visible around it
        double pad = 0.84;
        int fh = (int) (viewH * pad);
        int fw = (int) Math.round(fh * (PANEL_W / (double) PANEL_H));
        int fwMax = (int) (viewW * pad);
        if (fw > fwMax) { fw = fwMax; fh = (int) Math.round(fw * (PANEL_H / (double) PANEL_W)); }
        frameW = Math.max(1, fw);
        frameH = Math.max(1, fh);
        frameX = viewX + (viewW - frameW) / 2;
        frameY = viewY + (viewH - frameH) / 2;
        frameScale = frameW / (double) panelW;
    }

    private void loadSource() {
        try {
            refreshTexture();
            targetScale = recommendedScale();
            panelW = PANEL_W * targetScale;
            panelH = PANEL_H * targetScale;
            fit();
            error = null;
        } catch (Exception e) {
            error = "could not open image";
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

    // one grid cell = one cape-texture pixel, in panel-space units. editX/Y/W/H stay
    // CONTINUOUS so dragging accumulates smoothly; snapV() snaps only for draw + save,
    // so the image visibly lands on the grid yet can still be moved freely.
    private double cell() { return Math.max(1, targetScale); }
    private double snapV(double v) { double c = cell(); return Math.round(v / c) * c; }
    private double snapSize(double v) { double c = cell(); return Math.max(c, Math.round(v / c) * c); }

    // contain: whole image fits inside the crop frame, centred
    private void fit() {
        if (sourceW <= 0 || sourceH <= 0) return;
        double r = Math.min((double) panelW / sourceW, (double) panelH / sourceH);
        editW = sourceW * r;
        editH = sourceH * r;
        center();
    }

    // stretch: image is distorted to fill the whole crop frame exactly
    private void fill() {
        editX = 0;
        editY = 0;
        editW = panelW;
        editH = panelH;
    }

    private void center() {
        editX = (panelW - editW) / 2.0;
        editY = (panelH - editH) / 2.0;
    }

    private void zoomAt(double factor, double screenX, double screenY) {
        if (editW <= 0 || editH <= 0) return;
        double minW = panelW * 0.15, maxW = panelW * 40.0;
        double nw = editW * factor;
        if (nw < minW) factor = minW / editW;
        if (nw > maxW) factor = maxW / editW;
        double ax = (screenX - frameX) / frameScale;          // anchor in panel space
        double ay = (screenY - frameY) / frameScale;
        double fx = (ax - editX) / editW;                     // fraction of image under cursor
        double fy = (ay - editY) / editH;
        editW *= factor;
        editH *= factor;
        editX = ax - fx * editW;
        editY = ay - fy * editH;
    }

    private void pan(double dxScreen, double dyScreen) {
        editX += dxScreen / frameScale;
        editY += dyScreen / frameScale;
    }

    private void rotateImage() {
        rotation = (rotation + 1) & 3;
        try {
            refreshTexture();
            targetScale = recommendedScale();
            panelW = PANEL_W * targetScale;
            panelH = PANEL_H * targetScale;
            fit();
            layout();
            error = null;
        } catch (Exception e) {
            error = "could not rotate image";
        }
    }

    private void saveCape() {
        String id = CapeSpoofer.INSTANCE.importCapeEdited(
            sourcePath,
            (int) Math.round(snapV(editX)), (int) Math.round(snapV(editY)),
            Math.max(1, (int) Math.round(snapSize(editW))), Math.max(1, (int) Math.round(snapSize(editH))),
            targetScale, rotation);
        if (id == null) {
            error = "could not save cape";
            return;
        }
        CONFIG.setCapeId(id);
        CONFIG.setSpoofCape(true);
        minecraft.setScreen(new AppearanceEditorScreen());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        // our background only; skip vanilla blur + in-world menu tint (the grey wash on yuri mode)
        if (CONFIG.getBgImage()) g.blit(RenderPipelines.GUI_TEXTURED, BG_TEX, 0, 0, 0f, 0f, width, height, width, height, width, height);
        else g.fillGradient(0, 0, width, height, BG_TOP, BG_BOT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        this.mx = mouseX; this.my = mouseY;

        g.text(font, "edit cape", 16, 14, TEXT, false);
        String hint = "scroll · zoom    drag · move";
        g.text(font, hint, width - 16 - font.width(hint), 15, TEXT_FAINT, false);

        renderCanvas(g);
        super.extractRenderState(g, mouseX, mouseY, partial);
        renderButtons(g);
    }

    private void renderCanvas(GuiGraphicsExtractor g) {
        g.fill(viewX, viewY, viewX + viewW, viewY + viewH, VIEW_BG);
        border(g, viewX, viewY, viewW, viewH, LINE);

        if (error != null) {
            g.text(font, error, viewX + (viewW - font.width(error)) / 2, viewY + viewH / 2 - 4, TEXT_MUTE, false);
            return;
        }

        if (sourceTexture != null && editW > 0 && editH > 0) {
            // draw the grid-snapped rect (continuous edit values are snapped only here + on save)
            int ix = frameX + (int) Math.round(snapV(editX) * frameScale);
            int iy = frameY + (int) Math.round(snapV(editY) * frameScale);
            int iw = Math.max(1, (int) Math.round(snapSize(editW) * frameScale));
            int ih = Math.max(1, (int) Math.round(snapSize(editH) * frameScale));
            g.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);
            g.blit(RenderPipelines.GUI_TEXTURED, sourceTexture, ix, iy, 0f, 0f, iw, ih, sourceW, sourceH, sourceW, sourceH);
            g.disableScissor();
        }

        // dim everything outside the crop frame so the kept region reads clearly
        int fr = frameX + frameW, fb = frameY + frameH;
        g.fill(viewX, viewY, viewX + viewW, frameY, DIMMED);            // top
        g.fill(viewX, fb, viewX + viewW, viewY + viewH, DIMMED);        // bottom
        g.fill(viewX, frameY, frameX, fb, DIMMED);                     // left
        g.fill(fr, frameY, viewX + viewW, fb, DIMMED);                 // right

        grid(g);
        outline(g, frameX, frameY, frameW, frameH, FRAME);
        cornerBrackets(g, frameX, frameY, frameW, frameH);
    }

    // faint grid over the crop frame: one cell per cape-texture pixel (PANEL_W x PANEL_H)
    private void grid(GuiGraphicsExtractor g) {
        for (int i = 1; i < PANEL_W; i++) {
            int gx = frameX + (int) Math.round(i * frameW / (double) PANEL_W);
            g.fill(gx, frameY, gx + 1, frameY + frameH, GRID);
        }
        for (int j = 1; j < PANEL_H; j++) {
            int gy = frameY + (int) Math.round(j * frameH / (double) PANEL_H);
            g.fill(frameX, gy, frameX + frameW, gy + 1, GRID);
        }
    }

    private void renderButtons(GuiGraphicsExtractor g) {
        for (int i = 0; i < btnRects.size(); i++) {
            int[] r = btnRects.get(i);
            boolean hov = in(mx, my, r);
            boolean primary = i == saveBtnIndex;
            int fill = primary ? (hov ? BTN_SAVE_HI : BTN_SAVE) : (hov ? SURFACE_HI : SURFACE);
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], fill);
            border(g, r[0], r[1], r[2], r[3], LINE);
            String lbl = btnLabels.get(i);
            g.text(font, lbl, r[0] + (r[2] - font.width(lbl)) / 2, r[1] + (r[3] - 8) / 2, TEXT, false);
        }
    }

    // short L-shaped marks at each frame corner to read as a crop frame
    private void cornerBrackets(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int len = Math.max(4, Math.min(w, h) / 8), t = 2;
        int x1 = x + w, y1 = y + h;
        g.fill(x, y, x + len, y + t, FRAME);            g.fill(x, y, x + t, y + len, FRAME);
        g.fill(x1 - len, y, x1, y + t, FRAME);          g.fill(x1 - t, y, x1, y + len, FRAME);
        g.fill(x, y1 - t, x + len, y1, FRAME);          g.fill(x, y1 - len, x + t, y1, FRAME);
        g.fill(x1 - len, y1 - t, x1, y1, FRAME);        g.fill(x1 - t, y1 - len, x1, y1, FRAME);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (e.button() == 0) {
            for (int i = 0; i < btnRects.size(); i++) {
                if (in(e.x(), e.y(), btnRects.get(i))) { btnActs.get(i).run(); return true; }
            }
        }
        return super.mouseClicked(e, dbl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (e.button() == 0 && error == null
            && in(e.x(), e.y(), new int[]{ viewX, viewY, viewW, viewH })) {
            pan(dx, dy);
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (error == null && sourceW > 0 && in(x, y, new int[]{ viewX, viewY, viewW, viewH })) {
            zoomAt(sy > 0 ? 1.1 : 1.0 / 1.1, x, y);
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
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

    private static boolean in(double x, double y, int[] r) {
        return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
    }

    private static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        border(g, x, y, w, h, c);
    }
}
