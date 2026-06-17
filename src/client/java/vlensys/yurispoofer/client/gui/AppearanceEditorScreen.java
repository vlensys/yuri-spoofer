package vlensys.yurispoofer.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import vlensys.yurispoofer.client.spoof.ArmorPiece;
import vlensys.yurispoofer.client.spoof.ArmorSpoofer;
import vlensys.yurispoofer.client.spoof.CapeSpoofer;
import vlensys.yurispoofer.client.spoof.Capes;
import vlensys.yurispoofer.client.spoof.ColorWheel;
import vlensys.yurispoofer.client.spoof.Colors;
import vlensys.yurispoofer.client.spoof.CustomCape;
import vlensys.yurispoofer.client.spoof.HeadItems;
import vlensys.yurispoofer.client.spoof.Skulls;
import vlensys.yurispoofer.client.spoof.SpoofConfig;
import vlensys.yurispoofer.client.spoof.Trims;

// appearance editor
public class AppearanceEditorScreen extends Screen {

    private static final SpoofConfig CONFIG = SpoofConfig.INSTANCE;

    // palette
    private static final int BG_TOP   = 0xC00E0E14;
    private static final int BG_BOT   = 0xE0060609;
    private static final int PANEL    = 0xFFC6C6C6;
    private static final int PANEL_HI = 0xFFFFFFFF;
    private static final int PANEL_SH = 0xFF545459;
    private static final int CHIP_HI  = 0xFFD7D7D7;
    private static final int INSET    = 0xFF9C9CA0;
    private static final int DARK     = 0xFF1B1B20;
    private static final int DARK_HI  = 0xFF2A2A32;
    private static final int SEL_BG   = 0xFF24446E;
    private static final int SEL_LINE = 0xFFFFFFFF;
    private static final int ON_COL   = 0xFF4CA32C;
    private static final int OFF_TRK  = 0xFF6C6C72;
    private static final int KNOB     = 0xFFE8E8EC;
    private static final int TEXT_DK  = 0xFF2E2E33;
    private static final int TEXT_MUTE= 0xFF5C5C63;
    private static final int TEXT_FAINT = 0xFF8A8A90;
    private static final int TEXT_LT  = 0xFFF0F0F2;

    private static final int TAB_ARMOR = 0, TAB_CAPE = 1, TAB_SKULL = 2;
    private static final String[] TAB_NAMES = { "Armor", "Cape", "Skull" };
    private static final String[] SLOTS = { "HEAD", "CHEST", "LEGS", "FEET" };
    private static final String[] SLOT_NAMES = { "Helmet", "Chestplate", "Leggings", "Boots" };

    // trim defaults
    private static final String DEFAULT_PAT = "coast";
    private static final String DEFAULT_MAT = "redstone";

    private int tab = TAB_ARMOR;
    private float mx, my;

    // layout
    private int px, py, pw, ph;
    private int contentY;
    private int dx0, dy0, dx1, dy1, dollSize;
    private final int[][] tabRects = new int[3][];

    // hotspots
    private final List<int[]> hot = new ArrayList<>();
    private final List<Runnable> hotAct = new ArrayList<>();

    // doll
    private float dollYaw, dollYawTarget, dollTilt, dollTiltTarget;
    private long lastNanos = 0L;
    private boolean dollInit = false;

    // armor
    private String selSlot = "HEAD";
    private boolean hsvLoaded = false;
    private float colH, colS, colV = 1f;
    private int[] wheel, valBar;
    private int trimY;

    // cape gallery
    private final List<String> capeIds = new ArrayList<>();
    private int[] capeView;
    private int capeScroll = 0, capeCols = 3, capeTileW = 0, capeTileH = 46, capeMaxScroll = 0;

    // skull gallery
    private EditBox skullSearchBox, skullCustomBox;
    private String skullQuery = "";
    private final List<Skulls.Head> skullResults = new ArrayList<>();
    private int[] skullView;
    private int skullScroll = 0, skullCols = 6, skullTile = 26, skullMaxScroll = 0;

    public AppearanceEditorScreen() {
        super(Component.literal("Appearance"));
    }

    // lifecycle

    @Override
    protected void init() {
        syncBoxes();
        skullSearchBox = null; skullCustomBox = null;
        hot.clear(); hotAct.clear();
        wheel = null; valBar = null; skullView = null; capeView = null;

        int margin = 14;
        py = 40;
        ph = height - py - margin;
        pw = (int) (width * 0.50) - margin;
        px = margin;
        dx0 = px + pw + margin;
        dx1 = width - margin;
        dy0 = py;
        dy1 = height - margin;
        dollSize = Math.max(45, (int) ((dy1 - dy0) * 0.42));

        int tabW = (pw - 16 - 8) / 3, tabH = 22, tabY = py + 8, tabX = px + 8;
        for (int i = 0; i < 3; i++) {
            int rx = tabX + i * (tabW + 4);
            tabRects[i] = new int[]{ rx, tabY, tabW, tabH };
            final int t = i;
            addHot(rx, tabY, tabW, tabH, () -> { tab = t; rebuildWidgets(); });
        }
        contentY = tabY + tabH + 14;

        switch (tab) {
            case TAB_ARMOR -> initArmor();
            case TAB_CAPE  -> initCape();
            case TAB_SKULL -> initSkull();
        }
        applyDollTargets();
    }

    private void addHot(int x, int y, int w, int h, Runnable r) {
        hot.add(new int[]{ x, y, w, h });
        hotAct.add(r);
    }

    private void applyDollTargets() {
        if (tab == TAB_CAPE) { dollYawTarget = 180f; dollTiltTarget = 0f; }   // back
        else { dollYawTarget = 22f; dollTiltTarget = 0f; }                    // front
        if (!dollInit) { dollYaw = dollYawTarget; dollTilt = dollTiltTarget; dollInit = true; }
    }

    // armor

    private void initArmor() {
        int y = contentY;
        addHot(px + 12, y - 2, pw - 24, 20, () -> { CONFIG.setSpoofArmor(!CONFIG.getSpoofArmor()); rebuildWidgets(); });
        y += 30;

        int chipW = (pw - 24 - 8) / 2, chipH = 22;
        for (int i = 0; i < 4; i++) {
            int sx = px + 12 + (i % 2) * (chipW + 8);
            int sy = y + (i / 2) * (chipH + 6);
            final int idx = i;
            addHot(sx, sy, chipW, chipH, () -> { selSlot = SLOTS[idx]; loadHsvFrom(CONFIG.armorPiece(selSlot).getColor()); rebuildWidgets(); });
        }
        y += 2 * (chipH + 6) + 12;

        ArmorPiece piece = CONFIG.armorPiece(selSlot);
        if (!hsvLoaded) loadHsvFrom(piece.getColor());

        addHot(px + 12, y - 2, pw - 24, 20, () -> {
            ArmorPiece p = CONFIG.armorPiece(selSlot);
            boolean now = !p.getOn();
            if (now && selSlot.equals("HEAD") && CONFIG.getSpoofSkull()) return; // skull blocked
            p.setOn(now);
            rebuildWidgets();
        });
        y += 30;

        ColorWheel.ensureBuilt();
        int size = Math.min(ColorWheel.SIZE, pw - 24 - 70);
        wheel = new int[]{ px + 12, y, size };
        valBar = new int[]{ px + 12 + size + 14, y, 14, size };
        y += size + 14;

        trimY = y;
        addHot(px + 12, y, 20, 22, () -> cycleTrim(true, -1));
        addHot(px + pw - 12 - 20, y, 20, 22, () -> cycleTrim(true, 1));
        addHot(px + 12, y + 26, 20, 22, () -> cycleTrim(false, -1));
        addHot(px + pw - 12 - 20, y + 26, 20, 22, () -> cycleTrim(false, 1));
    }

    private void cycleTrim(boolean pattern, int dir) {
        if (selSlot.equals("HEAD") && CONFIG.getSpoofSkull()) return; // skull blocked
        ArmorPiece p = CONFIG.armorPiece(selSlot);
        List<String> opts = new ArrayList<>();
        opts.add(Trims.NONE);
        opts.addAll(pattern ? Trims.INSTANCE.getPATTERN_IDS() : Trims.INSTANCE.getMATERIAL_IDS());
        String cur = pattern ? p.getTrimPattern() : p.getTrimMaterial();
        int i = Math.max(0, opts.indexOf(cur));
        i = (i + dir + opts.size()) % opts.size();
        String chosen = opts.get(i);
        if (pattern) {
            p.setTrimPattern(chosen);
            if (!chosen.isEmpty() && p.getTrimMaterial().isEmpty()) p.setTrimMaterial(DEFAULT_MAT);
        } else {
            p.setTrimMaterial(chosen);
            if (!chosen.isEmpty() && p.getTrimPattern().isEmpty()) p.setTrimPattern(DEFAULT_PAT);
        }
        if (!chosen.isEmpty()) p.setOn(true);
    }

    // cape

    private void initCape() {
        int y = contentY;
        addHot(px + 12, y - 2, pw - 24, 20, () -> { CONFIG.setSpoofCape(!CONFIG.getSpoofCape()); rebuildWidgets(); });
        y += 28;

        capeIds.clear();
        capeIds.add(Capes.ADD);                                       // import tile
        for (CustomCape c : CONFIG.getCustomCapes()) capeIds.add(c.getId());
        capeIds.add(Capes.NONE);
        for (Capes.Cape c : Capes.INSTANCE.getPRESETS()) capeIds.add(c.getId());

        capeCols = 3;
        capeTileW = (pw - 24 - 8 * (capeCols - 1)) / capeCols;
        capeTileH = 46;

        capeView = new int[]{ px + 12, y, pw - 24, dy1 - 18 - y };
        int rows = (capeIds.size() + capeCols - 1) / capeCols;
        capeMaxScroll = Math.max(0, rows * (capeTileH + 8) - capeView[3]);
        capeScroll = Math.min(capeScroll, capeMaxScroll);
    }

    // skull

    private void initSkull() {
        int y = contentY;
        addHot(px + 12, y - 2, pw - 24, 20, () -> {
            boolean now = !CONFIG.getSpoofSkull();
            if (now && CONFIG.armorPiece("HEAD").getOn()) return; // helmet blocked
            CONFIG.setSpoofSkull(now);
            rebuildWidgets();
        });
        y += 28;

        skullSearchBox = makeBox(px + 12, y, pw - 24 - 56, "search " + Skulls.INSTANCE.count() + " heads", skullQuery, 48);
        skullSearchBox.setResponder(s -> { skullQuery = s; skullScroll = 0; recomputeSkulls(); });
        addHot(px + pw - 12 - 50, y, 50, 18, () -> { CONFIG.setSkullId(Skulls.CUSTOM); rebuildWidgets(); });
        y += 24;

        recomputeSkulls();
        skullTile = 26;
        skullCols = Math.max(4, (pw - 24 + 4) / (skullTile + 4));

        int bottom = Skulls.CUSTOM.equals(CONFIG.getSkullId()) ? dy1 - 28 : dy1 - 6;
        skullView = new int[]{ px + 12, y, pw - 24, bottom - y };
        int rows = (skullResults.size() + skullCols - 1) / skullCols;
        skullMaxScroll = Math.max(0, rows * (skullTile + 4) - skullView[3]);
        skullScroll = Math.min(skullScroll, skullMaxScroll);

        if (Skulls.CUSTOM.equals(CONFIG.getSkullId()))
            skullCustomBox = makeBox(px + 12, dy1 - 22, pw - 24, "base64 / hash / texture URL", CONFIG.getSkullCustomTexture(), 512);
    }

    private void recomputeSkulls() {
        skullResults.clear();
        skullResults.addAll(Skulls.INSTANCE.search(skullQuery, 300));
    }

    // render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        this.mx = mouseX; this.my = mouseY;
        g.fillGradient(0, 0, width, height, BG_TOP, BG_BOT);
        super.extractRenderState(g, mouseX, mouseY, partial);
        animateDoll();

        g.text(font, "Appearance Editor", px + 2, 18, TEXT_LT, true);
        String sv = "self-view only";
        g.text(font, sv, width - 14 - font.width(sv), 19, TEXT_FAINT, true);

        card(g, px, py, pw, ph);
        dollCard(g);

        for (int i = 0; i < 3; i++) {
            int[] r = tabRects[i];
            boolean sel = tab == i, hov = in(mx, my, r);
            if (sel) inset(g, r[0], r[1], r[2], r[3], PANEL);
            else     raised(g, r[0], r[1], r[2], r[3], hov ? CHIP_HI : PANEL);
            int tw = font.width(TAB_NAMES[i]);
            g.text(font, TAB_NAMES[i], r[0] + (r[2] - tw) / 2, r[1] + 7, sel ? TEXT_DK : TEXT_MUTE, false);
        }

        switch (tab) {
            case TAB_ARMOR -> renderArmor(g);
            case TAB_CAPE  -> renderCape(g);
            case TAB_SKULL -> renderSkull(g);
        }
    }

    private void renderArmor(GuiGraphicsExtractor g) {
        int y = contentY;
        toggle(g, px + 12, y, "Enable armor", CONFIG.getSpoofArmor(), false);
        y += 30;

        int chipW = (pw - 24 - 8) / 2, chipH = 22;
        for (int i = 0; i < 4; i++) {
            int sx = px + 12 + (i % 2) * (chipW + 8);
            int sy = y + (i / 2) * (chipH + 6);
            boolean sel = SLOTS[i].equals(selSlot);
            boolean on = CONFIG.armorPiece(SLOTS[i]).getOn();
            boolean locked = SLOTS[i].equals("HEAD") && CONFIG.getSpoofSkull();
            boolean hov = in(mx, my, new int[]{ sx, sy, chipW, chipH });
            if (sel) inset(g, sx, sy, chipW, chipH, PANEL);
            else     raised(g, sx, sy, chipW, chipH, hov ? CHIP_HI : PANEL);
            g.fill(sx + 7, sy + chipH / 2 - 3, sx + 13, sy + chipH / 2 + 3, locked ? TEXT_FAINT : (on ? ON_COL : OFF_TRK));
            g.text(font, SLOT_NAMES[i], sx + 20, sy + 7, on && !locked ? TEXT_DK : TEXT_MUTE, false);
        }
        y += 2 * (chipH + 6) + 12;

        boolean headLocked = selSlot.equals("HEAD") && CONFIG.getSpoofSkull();
        toggle(g, px + 12, y, "Show " + SLOT_NAMES[indexOfSlot(selSlot)], CONFIG.armorPiece(selSlot).getOn() && !headLocked, headLocked);
        y += 30;

        if (wheel == null) return;
        int wx = wheel[0], wy = wheel[1], size = wheel[2];
        inset(g, wx - 2, wy - 2, size + 4, size + 4, DARK);
        g.blit(RenderPipelines.GUI_TEXTURED, ColorWheel.TEX_ID, wx, wy, 0f, 0f, size, size, ColorWheel.SIZE, ColorWheel.SIZE, ColorWheel.SIZE, ColorWheel.SIZE);
        int[] mp = ColorWheel.markerPos(colH, colS, size);
        ring(g, wx + mp[0], wy + mp[1]);

        int bx = valBar[0], by = valBar[1], bw = valBar[2], bh = valBar[3];
        inset(g, bx - 1, by - 1, bw + 2, bh + 2, DARK);
        g.fillGradient(bx, by, bx + bw, by + bh, Colors.hsvToArgb(colH, colS, 1f), 0xFF000000);
        int vy = by + (int) ((1f - colV) * bh);
        g.fill(bx - 2, vy - 1, bx + bw + 2, vy + 1, SEL_LINE);

        int sw = bx + bw + 12;
        inset(g, sw - 1, by - 1, 34, 26, Colors.hsvToArgb(colH, colS, colV));
        g.text(font, String.format("#%06X", Colors.hsvToRgb(colH, colS, colV) & 0xFFFFFF), sw, by + 30, TEXT_MUTE, false);

        ArmorPiece p = CONFIG.armorPiece(selSlot);
        String pat = p.getTrimPattern(), mat = p.getTrimMaterial();
        String matForPat = mat.isEmpty() ? DEFAULT_MAT : mat;
        String patForMat = pat.isEmpty() ? DEFAULT_PAT : pat;
        ItemStack patIcon = ArmorSpoofer.previewStack(selSlot, p.getColor(), pat, pat.isEmpty() ? "" : matForPat);
        ItemStack matIcon = ArmorSpoofer.previewStack(selSlot, p.getColor(), mat.isEmpty() ? "" : patForMat, mat);
        cyclerIcon(g, px + 12, trimY, "Trim Pattern", Trims.INSTANCE.display(pat), patIcon);
        cyclerIcon(g, px + 12, trimY + 26, "Trim Material", Trims.INSTANCE.display(mat), matIcon);
        if (headLocked) g.text(font, "Helmet is locked while a skull is active.", px + 12, trimY + 54, TEXT_FAINT, false);
    }

    private void renderCape(GuiGraphicsExtractor g) {
        toggle(g, px + 12, contentY, "Enable cape", CONFIG.getSpoofCape(), false);
        if (capeView == null) return;
        int vx = capeView[0], vy = capeView[1], vw = capeView[2], vh = capeView[3];
        inset(g, vx, vy, vw, vh, DARK);

        g.enableScissor(vx, vy, vx + vw, vy + vh);
        String sel = CONFIG.getCapeId();
        int stepX = capeTileW + 8, stepY = capeTileH + 8;
        for (int i = 0; i < capeIds.size(); i++) {
            int c = i % capeCols, r = i / capeCols;
            int tx = vx + c * stepX;
            int ty = vy + r * stepY - capeScroll;
            if (ty + capeTileH < vy || ty > vy + vh) continue;
            String id = capeIds.get(i);
            boolean isSel = id.equals(sel);
            boolean hov = mx >= tx && mx < tx + capeTileW && my >= vy && my <= vy + vh && my >= ty && my < ty + capeTileH;
            g.fill(tx, ty, tx + capeTileW, ty + capeTileH, isSel ? SEL_BG : (hov ? DARK_HI : DARK));

            int phx = capeTileH - 15, pwx = Math.max(6, phx * 10 / 16), ix = tx + (capeTileW - pwx) / 2, iy = ty + 3;
            if (Capes.ADD.equals(id)) {
                g.fill(ix + pwx / 2 - 5, iy + phx / 2 - 1, ix + pwx / 2 + 5, iy + phx / 2 + 1, TEXT_LT);
                g.fill(ix + pwx / 2 - 1, iy + phx / 2 - 5, ix + pwx / 2 + 1, iy + phx / 2 + 5, TEXT_LT);
            } else if (Capes.NONE.equals(id)) {
                g.text(font, "—", ix + pwx / 2 - 2, iy + phx / 2 - 4, TEXT_FAINT, false);
            } else {
                Identifier tex = CapeSpoofer.INSTANCE.textureIdFor(id);
                if (tex != null) g.blit(RenderPipelines.GUI_TEXTURED, tex, ix, iy, 1f, 1f, pwx, phx, 10, 16, 64, 32);
                else g.text(font, "…", ix + pwx / 2 - 2, iy + phx / 2 - 4, TEXT_FAINT, false);
            }

            String nm = Capes.INSTANCE.displayFor(id);
            g.text(font, clip(nm, capeTileW / 6 + 1), tx + (capeTileW - Math.min(font.width(nm), capeTileW - 2)) / 2, ty + capeTileH - 9, isSel || hov ? TEXT_LT : TEXT_FAINT, false);
            if (Capes.INSTANCE.isCustom(id)) g.text(font, "×", tx + capeTileW - 8, ty + 1, hov ? 0xFFE06060 : TEXT_FAINT, false);
            if (isSel) outline(g, tx, ty, capeTileW, capeTileH, SEL_LINE);
        }
        g.disableScissor();
        scrollbar(g, capeView, capeScroll, capeMaxScroll);

        g.text(font, "click + to import a cape PNG  ·  × removes", px + 12, dy1 - 11, TEXT_FAINT, false);
    }

    private void renderSkull(GuiGraphicsExtractor g) {
        boolean helmetOn = CONFIG.armorPiece("HEAD").getOn();
        toggle(g, px + 12, contentY, "Enable skull", CONFIG.getSpoofSkull(), helmetOn);
        if (skullView == null) return;
        int vx = skullView[0], vy = skullView[1], vw = skullView[2], vh = skullView[3];
        inset(g, vx, vy, vw, vh, DARK);

        g.enableScissor(vx, vy, vx + vw, vy + vh);
        String sel = CONFIG.getSkullId();
        int step = skullTile + 4;
        for (int i = 0; i < skullResults.size(); i++) {
            int c = i % skullCols, r = i / skullCols;
            int tx = vx + 4 + c * step;
            int ty = vy + 4 + r * step - skullScroll;
            if (ty + skullTile < vy || ty > vy + vh) continue;
            Skulls.Head h = skullResults.get(i);
            boolean isSel = h.getId().equals(sel);
            boolean hov = mx >= tx && mx < tx + skullTile && my >= vy && my <= vy + vh && my >= ty && my < ty + skullTile;
            g.fill(tx, ty, tx + skullTile, ty + skullTile, isSel ? SEL_BG : (hov ? DARK_HI : DARK));
            ItemStack stack = HeadItems.forHash(h.getTex());
            g.item(stack, tx + (skullTile - 16) / 2, ty + (skullTile - 16) / 2);
            if (isSel) outline(g, tx, ty, skullTile, skullTile, SEL_LINE);
        }
        g.disableScissor();
        scrollbar(g, skullView, skullScroll, skullMaxScroll);

        if (!sel.isEmpty() && !Skulls.CUSTOM.equals(sel)) {
            Skulls.Head s = Skulls.INSTANCE.byId(sel);
            if (s != null) g.text(font, "> " + clip(s.getDisplay(), 28), px + 12, contentY + 16, TEXT_MUTE, false);
        }
    }

    // input

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (e.button() == 0) {
            double x = e.x(), y = e.y();
            if (tab == TAB_ARMOR && handleColorPick(x, y)) return true;
            if (tab == TAB_CAPE && handleCapeClick(x, y)) return true;
            if (tab == TAB_SKULL && handleSkullClick(x, y)) return true;
            for (int i = hot.size() - 1; i >= 0; i--) {
                int[] r = hot.get(i);
                if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]) { hotAct.get(i).run(); return true; }
            }
        }
        return super.mouseClicked(e, dbl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (e.button() == 0 && tab == TAB_ARMOR && handleColorPick(e.x(), e.y())) return true;
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (tab == TAB_CAPE && capeView != null && in(x, y, capeView)) {
            capeScroll = Math.max(0, Math.min(capeMaxScroll, capeScroll - (int) (sy * (capeTileH + 8))));
            return true;
        }
        if (tab == TAB_SKULL && skullView != null && in(x, y, skullView)) {
            skullScroll = Math.max(0, Math.min(skullMaxScroll, skullScroll - (int) (sy * (skullTile + 4))));
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    private boolean handleCapeClick(double x, double y) {
        if (capeView == null || !in(x, y, capeView)) return false;
        int stepX = capeTileW + 8, stepY = capeTileH + 8;
        int lx = (int) (x - capeView[0]), c = lx / stepX;
        if (c < 0 || c >= capeCols || lx - c * stepX > capeTileW) return false;
        int r = (int) (y - capeView[1] + capeScroll) / stepY;
        int idx = r * capeCols + c;
        if (idx < 0 || idx >= capeIds.size()) return false;
        String id = capeIds.get(idx);
        if (Capes.ADD.equals(id)) { pickCustomCapeFile(); return true; }
        if (Capes.INSTANCE.isCustom(id)) {
            int tx = capeView[0] + c * stepX;
            int ty = capeView[1] + r * stepY - capeScroll;
            if (x >= tx + capeTileW - 9 && y <= ty + 10) { CapeSpoofer.INSTANCE.removeCape(id); rebuildWidgets(); return true; }
        }
        CONFIG.setCapeId(id);
        if (!Capes.NONE.equals(id)) CONFIG.setSpoofCape(true);
        CapeSpoofer.INSTANCE.textureIdFor(id);
        rebuildWidgets();
        return true;
    }

    private boolean handleSkullClick(double x, double y) {
        if (skullView == null || !in(x, y, skullView)) return false;
        int step = skullTile + 4;
        int c = (int) ((x - (skullView[0] + 4)) / step);
        int r = (int) ((y - (skullView[1] + 4) + skullScroll) / step);
        if (c < 0 || c >= skullCols) return false;
        int idx = r * skullCols + c;
        if (idx < 0 || idx >= skullResults.size()) return false;
        CONFIG.setSkullId(skullResults.get(idx).getId());
        return true;
    }

    private boolean handleColorPick(double x, double y) {
        if (selSlot.equals("HEAD") && CONFIG.getSpoofSkull()) return false; // skull blocked
        if (wheel != null && x >= wheel[0] && x < wheel[0] + wheel[2] && y >= wheel[1] && y < wheel[1] + wheel[2]) {
            float[] hs = ColorWheel.pick((float) (x - wheel[0]), (float) (y - wheel[1]), wheel[2]);
            if (hs != null) { colH = hs[0]; colS = hs[1]; applyColor(); return true; }
        }
        if (valBar != null && x >= valBar[0] - 2 && x < valBar[0] + valBar[2] + 2 && y >= valBar[1] && y <= valBar[1] + valBar[3]) {
            colV = clamp(1f - (float) ((y - valBar[1]) / valBar[3]));
            applyColor();
            return true;
        }
        return false;
    }

    private void applyColor() {
        ArmorPiece p = CONFIG.armorPiece(selSlot);
        p.setColor(Colors.hsvToRgb(colH, colS, colV));
        p.setOn(true);
    }

    // cape picker
    private void pickCustomCapeFile() {
        new Thread(() -> {
            String path = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.png"));
                filters.flip();
                path = TinyFileDialogs.tinyfd_openFileDialog("Select a cape PNG", "", filters, "PNG image (*.png)", false);
            } catch (Throwable ignored) {
            }
            if (path != null) {
                String p = path;
                minecraft.execute(() -> {
                    String id = CapeSpoofer.INSTANCE.importCape(p);
                    if (id != null) { CONFIG.setCapeId(id); CONFIG.setSpoofCape(true); }
                    if (minecraft.screen == this) rebuildWidgets();
                });
            }
        }, "yuri-cape-picker").start();
    }

    @Override
    public void onClose() {
        syncBoxes();
        CONFIG.save();
        super.onClose();
    }

    private void syncBoxes() {
        if (skullCustomBox != null) CONFIG.setSkullCustomTexture(skullCustomBox.getValue());
    }

    // doll

    private void animateDoll() {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        float t = 1f - (float) Math.exp(-9.0 * dt);
        dollYaw += (dollYawTarget - dollYaw) * t;
        dollTilt += (dollTiltTarget - dollTilt) * t;
    }

    private void dollCard(GuiGraphicsExtractor g) {
        inset(g, dx0, dy0, dx1 - dx0, dy1 - dy0, DARK);
        g.fill(dx0 + 1, dy1 - 17, dx1 - 1, dy1 - 1, 0x33000000);
        renderDoll(g, dx0 + 6, dy0 + 6, dx1 - 6, dy1 - 12, dollSize, dollYaw, dollTilt);
        String cap = tab == TAB_CAPE ? "back view" : "front view";
        g.text(font, cap, dx0 + (dx1 - dx0 - font.width(cap)) / 2, dy1 - 11, TEXT_FAINT, true);
    }

    // forced yaw
    private void renderDoll(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int size, float yawDeg, float tiltDeg) {
        Player player = minecraft.player;
        if (player == null) return;
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf xRotation = new Quaternionf().rotateX(tiltDeg * (float) (Math.PI / 180.0));
        rotation.mul(xRotation);

        var renderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        EntityRenderState rs = renderer.createRenderState(player, 1.0F);
        rs.shadowPieces.clear();
        rs.outlineColor = 0;
        if (rs instanceof LivingEntityRenderState lrs) {
            lrs.bodyRot = 180f + yawDeg;
            lrs.yRot = 0f;   // fixed head
            lrs.xRot = lrs.pose != Pose.FALL_FLYING ? -tiltDeg : 0f;
            lrs.boundingBoxWidth = lrs.boundingBoxWidth / lrs.scale;
            lrs.boundingBoxHeight = lrs.boundingBoxHeight / lrs.scale;
            lrs.scale = 1.0F;
        }
        Vector3f translation = new Vector3f(0f, rs.boundingBoxHeight / 2f + 0.0625f, 0f);
        g.entity(rs, size, translation, rotation, xRotation, x0, y0, x1, y1);
    }

    // drawing

    private void card(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        raised(g, x, y, w, h, PANEL);
    }

    private void toggle(GuiGraphicsExtractor g, int x, int y, String label, boolean on, boolean locked) {
        g.text(font, locked ? label + " (locked)" : label, x, y + 2, locked ? TEXT_FAINT : TEXT_DK, false);
        int tw = 26, th = 12, tx = x + pw - 24 - tw, ty = y - 1;
        inset(g, tx, ty, tw, th, locked ? OFF_TRK : (on ? ON_COL : OFF_TRK));
        int kx = on ? tx + tw - 9 : tx + 1;
        raised(g, kx, ty + 1, 8, th - 2, locked ? TEXT_FAINT : KNOB);
    }

    private void cyclerIcon(GuiGraphicsExtractor g, int x, int y, String label, String value, ItemStack icon) {
        int w = pw - 24, h = 22;
        boolean lh = in(mx, my, new int[]{ x, y, 20, h });
        boolean rh = in(mx, my, new int[]{ x + w - 20, y, 20, h });
        raised(g, x, y, 20, h, lh ? CHIP_HI : PANEL);
        raised(g, x + w - 20, y, 20, h, rh ? CHIP_HI : PANEL);
        g.text(font, "<", x + 7, y + 7, TEXT_DK, false);
        g.text(font, ">", x + w - 12, y + 7, TEXT_DK, false);
        inset(g, x + 22, y, w - 44, h, INSET);
        g.item(icon, x + 25, y + 3);
        g.text(font, label, x + 46, y + 3, TEXT_DK, false);
        g.text(font, value, x + 46, y + 12, TEXT_MUTE, false);
    }

    private void scrollbar(GuiGraphicsExtractor g, int[] v, int scroll, int maxScroll) {
        if (maxScroll <= 0) return;
        int vx = v[0], vy = v[1], vw = v[2], vh = v[3];
        int thumbH = Math.max(16, (int) ((float) vh / (vh + maxScroll) * vh));
        int thumbY = vy + (int) ((float) scroll / maxScroll * (vh - thumbH));
        g.fill(vx + vw - 3, vy, vx + vw, vy + vh, 0xFF0C0C10);
        g.fill(vx + vw - 3, thumbY, vx + vw, thumbY + thumbH, 0xFFB0B0B6);
    }

    private void ring(GuiGraphicsExtractor g, int cx, int cy) {
        g.fill(cx - 3, cy - 1, cx + 3, cy + 1, SEL_LINE);
        g.fill(cx - 1, cy - 3, cx + 1, cy + 3, SEL_LINE);
    }

    private static void raised(GuiGraphicsExtractor g, int x, int y, int w, int h, int face) {
        g.fill(x, y, x + w, y + h, face);
        g.fill(x, y, x + w, y + 1, PANEL_HI);
        g.fill(x, y, x + 1, y + h, PANEL_HI);
        g.fill(x + w - 1, y, x + w, y + h, PANEL_SH);
        g.fill(x, y + h - 1, x + w, y + h, PANEL_SH);
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

    private EditBox makeBox(int x, int y, int w, String hint, String value, int maxLen) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal(hint));
        box.setMaxLength(maxLen);
        box.setValue(value);
        box.setHint(Component.literal(hint));
        addRenderableWidget(box);
        return box;
    }

    private void loadHsvFrom(int rgb) {
        float[] hsv = Colors.rgbToHsv(rgb);
        colH = hsv[0]; colS = hsv[1]; colV = hsv[2];
        hsvLoaded = true;
    }

    private static int indexOfSlot(String s) {
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i].equals(s)) return i;
        return 0;
    }

    private static boolean in(double x, double y, int[] r) {
        return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static String clip(String s, int max) {
        if (max < 1) max = 1;
        return s.length() <= max ? s : s.substring(0, Math.max(1, max - 1)) + "…";
    }
}
