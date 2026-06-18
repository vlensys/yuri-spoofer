package vlensys.yurispoofer.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
import vlensys.yurispoofer.client.spoof.Currencies;
import vlensys.yurispoofer.client.spoof.CustomCape;
import vlensys.yurispoofer.client.spoof.HeadItems;
import vlensys.yurispoofer.client.spoof.Ranks;
import vlensys.yurispoofer.client.spoof.Skills;
import vlensys.yurispoofer.client.spoof.Skulls;
import vlensys.yurispoofer.client.spoof.Slayers;
import vlensys.yurispoofer.client.spoof.SpoofConfig;
import vlensys.yurispoofer.client.spoof.Trims;

// unified spoofer + appearance editor (flat, minimal)
public class AppearanceEditorScreen extends Screen {

    private static final SpoofConfig CONFIG = SpoofConfig.INSTANCE;
    private static final Ranks      RANKS   = Ranks.INSTANCE;
    private static final Skills     SKILLS  = Skills.INSTANCE;
    private static final Slayers    SLAYERS = Slayers.INSTANCE;
    private static final Currencies CURRS   = Currencies.INSTANCE;

    // flat dark palette, white/neutral accent
    private static final int BG_TOP     = 0xF00B0B11;
    private static final int BG_BOT     = 0xF8050508;
    private static final int SURFACE    = 0xC015151B;
    private static final int SURFACE_HI = 0xC820202A;
    private static final int INSET      = 0xB00A0A0E;
    private static final int LINE       = 0xCC2A2A33;
    private static final int SEL_BG     = 0xCC23232E;
    private static final int ACCENT     = 0xFFFFFFFF;
    private static final int TOG_ON     = 0xFFE9E9EE;
    private static final int TOG_OFF    = 0xFF34343C;
    private static final int KNOB_ON    = 0xFF101015;
    private static final int KNOB_OFF   = 0xFF9A9AA2;
    private static final int TEXT       = 0xFFE9E9EE;
    private static final int TEXT_MUTE  = 0xFF9A9AA2;
    private static final int TEXT_FAINT = 0xFF5E5E68;
    private static final int ON_DOT     = 0xFF4CA32C;

    // master enable button (bottom-left): green = on, red = off
    private static final int BTN_ON     = 0xFF2F9E44;
    private static final int BTN_ON_HI  = 0xFF37B24D;
    private static final int BTN_OFF    = 0xFFB23A3A;
    private static final int BTN_OFF_HI = 0xFFC94B4B;

    // §/& legacy color codes 0-f
    private static final int[] LEGACY = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
    };

    private static final int TAB_PROFILE = 0, TAB_STATS = 1, TAB_ARMOR = 2, TAB_CAPE = 3, TAB_SKULL = 4;
    private static final String[] TAB_NAMES = { "Profile", "Stats", "Armor", "Cape", "Skull" };
    private static final String[] SLOTS = { "HEAD", "CHEST", "LEGS", "FEET" };
    private static final String[] SLOT_NAMES = { "Helmet", "Chestplate", "Leggings", "Boots" };

    private static final String DEFAULT_PAT = "coast";
    private static final String DEFAULT_MAT = "redstone";

    private static final int ROW_H = 20, SECTION_H = 15, RT = 18;
    private static final int PILL_W = 22, PILL_H = 12;

    // yuri button (bottom-left): toggles the image background
    private static final int YURI_BTN_W = 64, YURI_BTN_H = 18;
    private static final Identifier BG_TEX =
        Identifier.fromNamespaceAndPath("yuri-spoofer", "textures/gui/background.png");
    private int[] yuriBtn;

    private int tab = TAB_PROFILE;
    private float mx, my;

    // layout
    private int px, pw, tabY, contentY, panelBottom, leftViewportH;
    private int dx0, dy0, dx1, dy1, dollSize;
    private final int[][] tabRects = new int[TAB_NAMES.length][];
    private int[] masterRect;

    // hotspots + deferred left-panel draws
    private final List<int[]> hot = new ArrayList<>();
    private final List<Runnable> hotAct = new ArrayList<>();
    private final List<Consumer<GuiGraphicsExtractor>> leftDraws = new ArrayList<>();
    private int leftScroll = 0, leftMaxScroll = 0;

    // doll
    private float dollYaw, dollYawTarget, dollTilt, dollTiltTarget;
    private long lastNanos = 0L;
    private boolean dollInit = false;

    // profile / stats
    private EditBox nameBox, levelBox, lobbyBox, rankCustomBox;
    private final Map<String, EditBox> skillBoxes    = new LinkedHashMap<>();
    private final Map<String, EditBox> slayerBoxes   = new LinkedHashMap<>();
    private final Map<String, EditBox> currencyBoxes = new LinkedHashMap<>();

    // armor
    private String selSlot = "HEAD";
    private boolean hsvLoaded = false;
    private float colH, colS, colV = 1f;
    private int[] wheel, valBar;
    private int trimY;
    private EditBox hexBox;

    // trim pattern gallery (visual selector, like the cape grid)
    private final List<String> trimPatternIds = new ArrayList<>();
    private int[] trimView;
    private int trimScroll = 0, trimCols = 3, trimTileW = 0, trimTileH = 40, trimMaxScroll = 0;

    // cape gallery
    private final List<String> capeIds = new ArrayList<>();
    private int[] capeView;
    private int capeScroll = 0, capeCols = 3, capeTileW = 0, capeTileH = 46, capeMaxScroll = 0;
    private EditBox capeNameBox;

    // skull gallery
    private EditBox skullSearchBox, skullCustomBox;
    private String skullQuery = "";
    private final List<Skulls.Head> skullResults = new ArrayList<>();
    private int[] skullView;
    private int skullScroll = 0, skullCols = 6, skullTile = 26, skullMaxScroll = 0;

    public AppearanceEditorScreen() {
        super(Component.literal("yuri"));
    }

    // lifecycle

    @Override
    protected void init() {
        syncAll();
        nameBox = null; levelBox = null; lobbyBox = null; rankCustomBox = null;
        skillBoxes.clear(); slayerBoxes.clear(); currencyBoxes.clear();
        capeNameBox = null; skullSearchBox = null; skullCustomBox = null; hexBox = null;
        hot.clear(); hotAct.clear(); leftDraws.clear();
        wheel = null; valBar = null; skullView = null; capeView = null; trimView = null;

        int margin = 14;
        px = margin;
        int usableW = width - margin * 2;
        pw = (int) (usableW * 0.50) - 6;
        tabY = 30;
        contentY = tabY + 18;
        panelBottom = height - margin - 30; // reserve bottom-left for the yuri button
        if (panelBottom < contentY + 60) panelBottom = Math.min(height - margin, contentY + 60);
        leftViewportH = panelBottom - contentY;

        dx0 = px + pw + 12;
        dy0 = contentY;
        dx1 = width - margin;
        dy1 = height - margin;
        dollSize = Math.max(45, (int) ((dy1 - dy0) * 0.42));

        // tabs across the top
        int tx = margin;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int tw = font.width(TAB_NAMES[i]);
            tabRects[i] = new int[]{ tx, tabY, tw, 12 };
            final int t = i;
            addHot(tx, tabY - 2, tw + 4, 16, () -> { tab = t; leftScroll = 0; rebuildWidgets(); });
            tx += tw + 14;
        }

        // master toggle (top-right)
        int mx0 = width - margin - PILL_W;
        masterRect = new int[]{ mx0, 11, PILL_W, PILL_H };
        addHot(mx0, 10, PILL_W, 14, () -> { CONFIG.setMasterEnabled(!CONFIG.getMasterEnabled()); rebuildWidgets(); });

        // yuri button (bottom-left): toggles the image background (green = on, red = off)
        yuriBtn = new int[]{ margin, height - margin - YURI_BTN_H, YURI_BTN_W, YURI_BTN_H };
        addHot(yuriBtn[0], yuriBtn[1], yuriBtn[2], yuriBtn[3],
            () -> { CONFIG.setBgImage(!CONFIG.getBgImage()); CONFIG.save(); });

        switch (tab) {
            case TAB_PROFILE -> initProfile();
            case TAB_STATS   -> initStats();
            case TAB_ARMOR   -> initArmor();
            case TAB_CAPE    -> initCape();
            case TAB_SKULL   -> initSkull();
        }
        applyDollTargets();
    }

    @Override
    public void rebuildWidgets() {
        syncAll();
        CONFIG.save();
        super.rebuildWidgets();
    }

    private void addHot(int x, int y, int w, int h, Runnable r) {
        hot.add(new int[]{ x, y, w, h });
        hotAct.add(r);
    }

    private void applyDollTargets() {
        if (tab == TAB_CAPE) { dollYawTarget = 180f; dollTiltTarget = 0f; }
        else { dollYawTarget = 22f; dollTiltTarget = 0f; }
        if (!dollInit) { dollYaw = dollYawTarget; dollTilt = dollTiltTarget; dollInit = true; }
    }

    // profile tab

    private void initProfile() {
        int total = measureProfile();
        leftMaxScroll = Math.max(0, total - leftViewportH);
        leftScroll = Math.max(0, Math.min(leftScroll, leftMaxScroll));
        int ly = 0;

        ly = drawSection(ly, "Profile");
        ly = featureRow(ly, "Name", CONFIG.getSpoofName(),
            () -> CONFIG.setSpoofName(!CONFIG.getSpoofName()),
            Math.min(150, pw - 90), CONFIG.getFakeName(), "Fake name", 64, b -> nameBox = b);

        ly = drawSection(ly, "Rank");
        ly = toggleRow(ly, "Spoof rank", CONFIG.getSpoofRank(),
            () -> CONFIG.setSpoofRank(!CONFIG.getSpoofRank()));
        ly = drawRankGrid(ly);
        if (Ranks.CUSTOM.equals(CONFIG.getRankPreset())) {
            ly = boxRow(ly, "Custom", pw - 60, CONFIG.getRankText(), "&c[&6OWNER&c]", 64, b -> rankCustomBox = b);
        }

        ly = drawSection(ly, "Location");
        ly = featureRow(ly, "Level", CONFIG.getSpoofLevel(),
            () -> CONFIG.setSpoofLevel(!CONFIG.getSpoofLevel()),
            56, CONFIG.getLevelText(), "500", 8, b -> levelBox = b);
        ly = featureRow(ly, "Lobby", CONFIG.getSpoofLobby(),
            () -> CONFIG.setSpoofLobby(!CONFIG.getSpoofLobby()),
            100, CONFIG.getLobbyText(), "YURI01", 64, b -> lobbyBox = b);
    }

    private int measureProfile() {
        int h = SECTION_H + ROW_H;                         // Profile + name
        h += SECTION_H + ROW_H + rankRows() * RT;          // Rank + toggle + grid
        if (Ranks.CUSTOM.equals(CONFIG.getRankPreset())) h += ROW_H;
        h += SECTION_H + ROW_H + ROW_H;                    // Location + level + lobby
        return h;
    }

    // stats tab

    private void initStats() {
        int total = measureStats();
        leftMaxScroll = Math.max(0, total - leftViewportH);
        leftScroll = Math.max(0, Math.min(leftScroll, leftMaxScroll));
        int ly = 0;

        ly = sectionToggle(ly, "Skills", CONFIG.getSpoofSkills(),
            () -> CONFIG.setSpoofSkills(!CONFIG.getSpoofSkills()));
        for (Skills.Skill s : SKILLS.getALL()) {
            Integer cur = CONFIG.getSkillLevels().get(s.getName());
            ly = statRow(ly, s.getName(), 46,
                cur != null && cur > 0 ? String.valueOf(cur) : "", String.valueOf(s.getCap()), 3, skillBoxes);
        }

        ly = sectionToggle(ly, "Slayers", CONFIG.getSpoofSlayers(),
            () -> CONFIG.setSpoofSlayers(!CONFIG.getSpoofSlayers()));
        for (Slayers.Slayer s : SLAYERS.getALL()) {
            Integer cur = CONFIG.getSlayerLevels().get(s.getName());
            ly = statRow(ly, s.getName(), 46,
                cur != null && cur > 0 ? String.valueOf(cur) : "", String.valueOf(s.getCap()), 3, slayerBoxes);
        }

        ly = sectionToggle(ly, "Currency", CONFIG.getSpoofCurrency(),
            () -> CONFIG.setSpoofCurrency(!CONFIG.getSpoofCurrency()));
        for (Currencies.Currency c : CURRS.getALL()) {
            ly = statRow(ly, c.getName(), 104,
                CONFIG.getCurrencyValues().getOrDefault(c.getName(), ""), "value", 20, currencyBoxes);
        }
    }

    private int measureStats() {
        return ROW_H + SKILLS.getALL().size() * ROW_H
             + ROW_H + SLAYERS.getALL().size() * ROW_H
             + ROW_H + CURRS.getALL().size() * ROW_H;
    }

    // left-panel row builders (logical y in, next logical y out)

    private int drawSection(int ly, String title) {
        final int ay = contentY + ly - leftScroll;
        leftDraws.add(g -> {
            g.text(font, title, px + 2, ay + 4, TEXT_MUTE, false);
            int lw = font.width(title);
            g.fill(px + 2 + lw + 6, ay + 7, px + pw, ay + 8, LINE);
        });
        return ly + SECTION_H;
    }

    private int sectionToggle(int ly, String title, boolean on, Runnable toggle) {
        final int ay = contentY + ly - leftScroll;
        if (rowVisible(ay, ROW_H)) addHot(px + 2, ay + 3, PILL_W, 14, () -> { toggle.run(); rebuildWidgets(); });
        leftDraws.add(g -> {
            pill(g, px + 2, ay + 4, on, false);
            g.text(font, title, px + 2 + PILL_W + 8, ay + 5, TEXT_MUTE, false);
            int lw = font.width(title);
            g.fill(px + 2 + PILL_W + 8 + lw + 6, ay + 8, px + pw, ay + 9, LINE);
        });
        return ly + ROW_H;
    }

    private int toggleRow(int ly, String label, boolean on, Runnable toggle) {
        final int ay = contentY + ly - leftScroll;
        if (rowVisible(ay, ROW_H)) addHot(px + 2, ay + 3, PILL_W, 14, () -> { toggle.run(); rebuildWidgets(); });
        leftDraws.add(g -> {
            pill(g, px + 2, ay + 4, on, false);
            g.text(font, label, px + 2 + PILL_W + 8, ay + 5, on ? TEXT : TEXT_MUTE, false);
        });
        return ly + ROW_H;
    }

    // pill toggle + label + value box, on one row
    private int featureRow(int ly, String label, boolean on, Runnable toggle,
                           int boxW, String value, String hint, int maxLen, Consumer<EditBox> sink) {
        final int ay = contentY + ly - leftScroll;
        final boolean fOn = on;
        if (rowVisible(ay, ROW_H)) addHot(px + 2, ay + 3, PILL_W, 14, () -> { toggle.run(); rebuildWidgets(); });
        leftDraws.add(g -> {
            pill(g, px + 2, ay + 4, fOn, false);
            g.text(font, label, px + 2 + PILL_W + 8, ay + 5, fOn ? TEXT : TEXT_MUTE, false);
        });
        EditBox box = boxCulled(px + pw - boxW, ay + 1, boxW, hint, value, maxLen, ay);
        sink.accept(box);
        return ly + ROW_H;
    }

    // label + value box (no toggle)
    private int boxRow(int ly, String label, int boxW, String value, String hint, int maxLen, Consumer<EditBox> sink) {
        final int ay = contentY + ly - leftScroll;
        leftDraws.add(g -> g.text(font, label, px + 4, ay + 5, TEXT_MUTE, false));
        EditBox box = boxCulled(px + pw - boxW, ay + 1, boxW, hint, value, maxLen, ay);
        sink.accept(box);
        return ly + ROW_H;
    }

    private int statRow(int ly, String name, int boxW, String value, String hint, int maxLen, Map<String, EditBox> map) {
        final int ay = contentY + ly - leftScroll;
        leftDraws.add(g -> g.text(font, name, px + 18, ay + 6, TEXT, false));
        map.put(name, boxCulled(px + 120, ay + 3, boxW, 14, hint, value, maxLen, ay));
        return ly + ROW_H;
    }

    private int drawRankGrid(int ly) {
        List<String> order = RANKS.getORDER();
        int cols = 3, gap = 3;
        int tileW = (pw - gap * (cols - 1)) / cols, tileH = RT - 2;
        for (int i = 0; i < order.size(); i++) {
            int c = i % cols, r = i / cols;
            final int tx = px + c * (tileW + gap);
            final int ay = contentY + ly + r * RT - leftScroll;
            final String key = order.get(i);
            final boolean sel = key.equals(CONFIG.getRankPreset());
            final int fw = tileW, fh = tileH;
            if (rowVisible(ay, fh)) addHot(tx, ay, fw, fh, () -> { CONFIG.setRankPreset(key); rebuildWidgets(); });
            leftDraws.add(g -> {
                boolean hov = in(mx, my, new int[]{ tx, ay, fw, fh });
                g.fill(tx, ay, tx + fw, ay + fh, sel ? SEL_BG : (hov ? SURFACE_HI : SURFACE));
                if (sel) outline(g, tx, ay, fw, fh, ACCENT);
                String prefix = RANKS.prefixFor(key, "");
                int ty = ay + (fh - 8) / 2;
                if (prefix.isEmpty()) {
                    g.text(font, "None", tx + (fw - font.width("None")) / 2, ty, sel ? TEXT : TEXT_FAINT, false);
                } else {
                    drawLegacy(g, prefix, tx + (fw - plainWidth(prefix)) / 2, ty, 0xFFFFFF);
                }
            });
        }
        return ly + rankRows() * RT;
    }

    private int rankRows() { return (RANKS.getORDER().size() + 2) / 3; }

    private boolean rowVisible(int ay, int h) { return ay + h > contentY && ay < panelBottom; }

    private EditBox boxCulled(int x, int y, int w, String hint, String value, int maxLen, int rowY) {
        return boxCulled(x, y, w, 18, hint, value, maxLen, rowY);
    }

    private EditBox boxCulled(int x, int y, int w, int h, String hint, String value, int maxLen, int rowY) {
        EditBox box = new TransEditBox(font, x, y, w, h, Component.literal(hint));
        box.setMaxLength(maxLen);
        box.setValue(value);
        box.setHint(Component.literal(hint));
        if (rowY >= contentY && rowY + ROW_H <= panelBottom) addRenderableWidget(box);
        return box;
    }

    // armor tab

    private void initArmor() {
        int y = contentY;
        addHot(px, y, pw, 16, () -> { CONFIG.setSpoofArmor(!CONFIG.getSpoofArmor()); rebuildWidgets(); });
        y += 26;

        int chipW = (pw - 8) / 2, chipH = 22;
        for (int i = 0; i < 4; i++) {
            int sx = px + (i % 2) * (chipW + 8);
            int sy = y + (i / 2) * (chipH + 6);
            final int idx = i;
            addHot(sx, sy, chipW, chipH, () -> { selSlot = SLOTS[idx]; loadHsvFrom(CONFIG.armorPiece(selSlot).getColor()); rebuildWidgets(); });
        }
        y += 2 * (chipH + 6) + 10;

        ArmorPiece piece = CONFIG.armorPiece(selSlot);
        if (!hsvLoaded) loadHsvFrom(piece.getColor());

        addHot(px, y, pw, 16, () -> {
            ArmorPiece p = CONFIG.armorPiece(selSlot);
            boolean now = !p.getOn();
            if (now && selSlot.equals("HEAD") && CONFIG.getSpoofSkull()) return;
            p.setOn(now);
            rebuildWidgets();
        });
        y += 26;

        ColorWheel.ensureBuilt();
        int avail = panelBottom - y - 110;
        int size = Math.min(ColorWheel.SIZE, Math.min(Math.min(pw - 70, 96), Math.max(40, avail)));
        wheel = new int[]{ px, y, size };
        valBar = new int[]{ px + size + 14, y, 14, size };

        int sw = valBar[0] + valBar[2] + 12;
        hexBox = makeBox(sw, y + 28, 56, "#RRGGBB",
            String.format("#%06X", CONFIG.armorPiece(selSlot).getColor() & 0xFFFFFF), 7);
        hexBox.setResponder(this::applyHex);

        y += size + 12;

        // trim material
        trimY = y;
        addHot(px, y, 18, 20, () -> cycleTrim(false, -1));
        addHot(px + pw - 18, y, 18, 20, () -> cycleTrim(false, 1));
        y += 24 + 12; // material row + "Trim Pattern" label

        trimPatternIds.clear();
        trimPatternIds.add(Trims.NONE);
        trimPatternIds.addAll(Trims.INSTANCE.getPATTERN_IDS());
        trimCols = 3;
        trimTileW = (pw - 8 * (trimCols - 1)) / trimCols;
        trimTileH = 40;
        trimView = new int[]{ px, y, pw, Math.max(0, panelBottom - y) };
        int prows = (trimPatternIds.size() + trimCols - 1) / trimCols;
        trimMaxScroll = Math.max(0, prows * (trimTileH + 8) - trimView[3]);
        trimScroll = Math.min(trimScroll, trimMaxScroll);
    }

    private void applyHex(String s) {
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.length() != 6) return;
        int rgb;
        try { rgb = Integer.parseInt(t, 16); } catch (NumberFormatException e) { return; }
        rgb &= 0xFFFFFF;
        if (selSlot.equals("HEAD") && CONFIG.getSpoofSkull()) return;
        loadHsvFrom(rgb);
        ArmorPiece p = CONFIG.armorPiece(selSlot);
        p.setColor(rgb);
        p.setOn(true);
    }

    private void cycleTrim(boolean pattern, int dir) {
        if (selSlot.equals("HEAD") && CONFIG.getSpoofSkull()) return;
        ArmorPiece p = CONFIG.armorPiece(selSlot);
        if (pattern) {
            // pattern can be None; leaving it None does not touch the material
            List<String> opts = new ArrayList<>();
            opts.add(Trims.NONE);
            opts.addAll(Trims.INSTANCE.getPATTERN_IDS());
            int i = Math.max(0, opts.indexOf(p.getTrimPattern()));
            i = (i + dir + opts.size()) % opts.size();
            String chosen = opts.get(i);
            p.setTrimPattern(chosen);
            if (!chosen.isEmpty() && p.getTrimMaterial().isEmpty()) p.setTrimMaterial(DEFAULT_MAT);
        } else {
            // material is never None
            List<String> opts = new ArrayList<>(Trims.INSTANCE.getMATERIAL_IDS());
            String cur = p.getTrimMaterial().isEmpty() ? DEFAULT_MAT : p.getTrimMaterial();
            int i = Math.max(0, opts.indexOf(cur));
            i = (i + dir + opts.size()) % opts.size();
            p.setTrimMaterial(opts.get(i));
        }
        p.setOn(true);
    }

    private void renderArmor(GuiGraphicsExtractor g) {
        int y = contentY;
        toggle(g, px, y, "Enable armor", CONFIG.getSpoofArmor(), false);
        y += 26;

        int chipW = (pw - 8) / 2, chipH = 22;
        for (int i = 0; i < 4; i++) {
            int sx = px + (i % 2) * (chipW + 8);
            int sy = y + (i / 2) * (chipH + 6);
            boolean sel = SLOTS[i].equals(selSlot);
            boolean on = CONFIG.armorPiece(SLOTS[i]).getOn();
            boolean locked = SLOTS[i].equals("HEAD") && CONFIG.getSpoofSkull();
            boolean hov = in(mx, my, new int[]{ sx, sy, chipW, chipH });
            g.fill(sx, sy, sx + chipW, sy + chipH, sel ? SEL_BG : (hov ? SURFACE_HI : SURFACE));
            if (sel) outline(g, sx, sy, chipW, chipH, ACCENT);
            g.fill(sx + 7, sy + chipH / 2 - 3, sx + 13, sy + chipH / 2 + 3, locked ? TEXT_FAINT : (on ? ON_DOT : TOG_OFF));
            g.text(font, SLOT_NAMES[i], sx + 20, sy + 7, on && !locked ? TEXT : TEXT_MUTE, false);
        }
        y += 2 * (chipH + 6) + 10;

        boolean headLocked = selSlot.equals("HEAD") && CONFIG.getSpoofSkull();
        toggle(g, px, y, "Show " + SLOT_NAMES[indexOfSlot(selSlot)], CONFIG.armorPiece(selSlot).getOn() && !headLocked, headLocked);
        y += 26;

        if (wheel == null) return;
        int wx = wheel[0], wy = wheel[1], size = wheel[2];
        insetBox(g, wx - 2, wy - 2, size + 4, size + 4);
        g.blit(RenderPipelines.GUI_TEXTURED, ColorWheel.TEX_ID, wx, wy, 0f, 0f, size, size, ColorWheel.SIZE, ColorWheel.SIZE, ColorWheel.SIZE, ColorWheel.SIZE);
        int[] mp = ColorWheel.markerPos(colH, colS, size);
        ring(g, wx + mp[0], wy + mp[1]);

        int bx = valBar[0], by = valBar[1], bw = valBar[2], bh = valBar[3];
        insetBox(g, bx - 1, by - 1, bw + 2, bh + 2);
        g.fillGradient(bx, by, bx + bw, by + bh, Colors.hsvToArgb(colH, colS, 1f), 0xFF000000);
        int vy = by + (int) ((1f - colV) * bh);
        g.fill(bx - 2, vy - 1, bx + bw + 2, vy + 1, ACCENT);

        int sw = bx + bw + 12;
        insetBox(g, sw - 1, by - 1, 34, 26);
        g.fill(sw, by, sw + 32, by + 24, Colors.hsvToArgb(colH, colS, colV));
        // hex EditBox sits at (sw, by + 28); keep it in sync with the wheel
        if (hexBox != null) {
            String hx = String.format("#%06X", Colors.hsvToRgb(colH, colS, colV) & 0xFFFFFF);
            if (!hexBox.isFocused() && !hexBox.getValue().equalsIgnoreCase(hx)) hexBox.setValue(hx);
        }

        if (headLocked) {
            g.text(font, "Helmet locked while a skull is active.", px, trimY + 6, TEXT_FAINT, false);
            return;
        }

        ArmorPiece p = CONFIG.armorPiece(selSlot);
        String matShown = p.getTrimMaterial().isEmpty() ? DEFAULT_MAT : p.getTrimMaterial();
        cycler(g, px, trimY, "Trim Material", Trims.INSTANCE.display(matShown));
        g.text(font, "Trim Pattern", px, trimView[1] - 11, TEXT_MUTE, false);
        renderTrimGallery(g);
    }

    private void renderTrimGallery(GuiGraphicsExtractor g) {
        if (trimView == null) return;
        int vx = trimView[0], vy = trimView[1], vw = trimView[2], vh = trimView[3];
        if (vh <= 0) return;
        insetBox(g, vx, vy, vw, vh);
        g.enableScissor(vx, vy, vx + vw, vy + vh);
        ArmorPiece p = CONFIG.armorPiece(selSlot);
        int dye = Colors.hsvToRgb(colH, colS, colV);
        String mat = p.getTrimMaterial().isEmpty() ? DEFAULT_MAT : p.getTrimMaterial();
        String selPat = p.getTrimPattern();
        int stepX = trimTileW + 8, stepY = trimTileH + 8;
        for (int i = 0; i < trimPatternIds.size(); i++) {
            int c = i % trimCols, r = i / trimCols;
            int tx = vx + c * stepX;
            int ty = vy + r * stepY - trimScroll;
            if (ty + trimTileH < vy || ty > vy + vh) continue;
            String pid = trimPatternIds.get(i);
            boolean isSel = pid.equals(selPat);
            boolean hov = mx >= tx && mx < tx + trimTileW && my >= vy && my <= vy + vh && my >= ty && my < ty + trimTileH;
            g.fill(tx, ty, tx + trimTileW, ty + trimTileH, isSel ? SEL_BG : (hov ? SURFACE_HI : INSET));
            ItemStack stack = ArmorSpoofer.previewStack(selSlot, dye, pid, mat);
            float sc = 1.7f;
            int isz = Math.round(16 * sc);
            bigItem(g, stack, tx + (trimTileW - isz) / 2, ty + 3, sc);
            String nm = Trims.INSTANCE.display(pid);
            g.text(font, clip(nm, trimTileW / 6 + 1),
                tx + (trimTileW - Math.min(font.width(nm), trimTileW - 2)) / 2, ty + trimTileH - 9,
                isSel || hov ? TEXT : TEXT_FAINT, false);
            if (isSel) outline(g, tx, ty, trimTileW, trimTileH, ACCENT);
        }
        g.disableScissor();
        border(g, vx, vy, vw, vh, LINE);
        scrollbar(g, trimView, trimScroll, trimMaxScroll);
    }

    private void bigItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y, float scale) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.item(stack, 0, 0);
        pose.popMatrix();
    }

    // cape tab

    private void initCape() {
        int y = contentY;
        addHot(px, y, pw, 16, () -> { CONFIG.setSpoofCape(!CONFIG.getSpoofCape()); rebuildWidgets(); });
        y += 24;

        capeIds.clear();
        capeIds.add(Capes.ADD);
        for (CustomCape c : CONFIG.getCustomCapes()) capeIds.add(c.getId());
        capeIds.add(Capes.NONE);
        for (Capes.Cape c : Capes.INSTANCE.getPRESETS()) capeIds.add(c.getId());

        capeCols = 3;
        capeTileW = (pw - 8 * (capeCols - 1)) / capeCols;
        capeTileH = 46;

        boolean customSelected = Capes.INSTANCE.isCustom(CONFIG.getCapeId());
        int bottom = customSelected ? panelBottom - 24 : panelBottom - 12;
        capeView = new int[]{ px, y, pw, bottom - y };
        int rows = (capeIds.size() + capeCols - 1) / capeCols;
        capeMaxScroll = Math.max(0, rows * (capeTileH + 8) - capeView[3]);
        capeScroll = Math.min(capeScroll, capeMaxScroll);

        if (customSelected) {
            capeNameBox = makeBox(px + 38, panelBottom - 20, pw - 38, "name", Capes.INSTANCE.displayFor(CONFIG.getCapeId()), 24);
        }
    }

    private void renderCape(GuiGraphicsExtractor g) {
        toggle(g, px, contentY, "Enable cape", CONFIG.getSpoofCape(), false);
        if (capeView == null) return;
        int vx = capeView[0], vy = capeView[1], vw = capeView[2], vh = capeView[3];
        insetBox(g, vx, vy, vw, vh);

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
            g.fill(tx, ty, tx + capeTileW, ty + capeTileH, isSel ? SEL_BG : (hov ? SURFACE_HI : INSET));

            int phx = capeTileH - 15, pwx = Math.max(6, phx * 10 / 16), ix = tx + (capeTileW - pwx) / 2, iy = ty + 3;
            if (Capes.ADD.equals(id)) {
                g.fill(ix + pwx / 2 - 5, iy + phx / 2 - 1, ix + pwx / 2 + 5, iy + phx / 2 + 1, TEXT);
                g.fill(ix + pwx / 2 - 1, iy + phx / 2 - 5, ix + pwx / 2 + 1, iy + phx / 2 + 5, TEXT);
            } else if (Capes.NONE.equals(id)) {
                // blank cape thumbnail (no design)
                g.fill(ix, iy, ix + pwx, iy + phx, 0xFFBFBFC8);
                border(g, ix, iy, pwx, phx, 0xFF6A6A74);
            } else {
                Identifier tex = CapeSpoofer.INSTANCE.textureIdFor(id);
                if (tex != null) {
                    int[] ts = CapeSpoofer.INSTANCE.textureSizeFor(id);
                    int sx = Math.max(1, ts[0] / CapeSpoofer.SHEET_WIDTH);
                    int sy = Math.max(1, ts[1] / CapeSpoofer.SHEET_HEIGHT);
                    g.blit(RenderPipelines.GUI_TEXTURED, tex, ix, iy,
                        CapeSpoofer.PANEL_X * sx, CapeSpoofer.PANEL_Y * sy, pwx, phx,
                        CapeSpoofer.PANEL_WIDTH * sx, CapeSpoofer.PANEL_HEIGHT * sy, ts[0], ts[1]);
                } else {
                    g.text(font, "…", ix + pwx / 2 - 2, iy + phx / 2 - 4, TEXT_FAINT, false);
                }
            }

            String nm = Capes.INSTANCE.displayFor(id);
            g.text(font, clip(nm, capeTileW / 6 + 1), tx + (capeTileW - Math.min(font.width(nm), capeTileW - 2)) / 2, ty + capeTileH - 9, isSel || hov ? TEXT : TEXT_FAINT, false);
            if (Capes.INSTANCE.isCustom(id)) g.text(font, "×", tx + capeTileW - 8, ty + 1, hov ? 0xFFE06060 : TEXT_FAINT, false);
            if (isSel) outline(g, tx, ty, capeTileW, capeTileH, ACCENT);
        }
        g.disableScissor();
        border(g, vx, vy, vw, vh, LINE); // redraw clean border over tile edges
        scrollbar(g, capeView, capeScroll, capeMaxScroll);

        if (capeNameBox != null) g.text(font, "name", px, panelBottom - 15, TEXT_FAINT, false);
    }

    // skull tab

    private void initSkull() {
        int y = contentY;
        addHot(px, y, pw, 16, () -> {
            boolean now = !CONFIG.getSpoofSkull();
            if (now && CONFIG.armorPiece("HEAD").getOn()) return;
            CONFIG.setSpoofSkull(now);
            rebuildWidgets();
        });
        y += 24;

        skullSearchBox = makeBox(px, y, pw - 56, "search " + Skulls.INSTANCE.count() + " heads", skullQuery, 48);
        skullSearchBox.setResponder(s -> { skullQuery = s; skullScroll = 0; recomputeSkulls(); });
        addHot(px + pw - 50, y, 50, 18, () -> { CONFIG.setSkullId(Skulls.CUSTOM); rebuildWidgets(); });
        y += 24;

        recomputeSkulls();
        skullTile = 26;
        skullCols = Math.max(4, (pw + 4) / (skullTile + 4));

        int bottom = Skulls.CUSTOM.equals(CONFIG.getSkullId()) ? panelBottom - 24 : panelBottom - 6;
        skullView = new int[]{ px, y, pw, bottom - y };
        int rows = (skullResults.size() + skullCols - 1) / skullCols;
        skullMaxScroll = Math.max(0, rows * (skullTile + 4) - skullView[3]);
        skullScroll = Math.min(skullScroll, skullMaxScroll);

        if (Skulls.CUSTOM.equals(CONFIG.getSkullId()))
            skullCustomBox = makeBox(px, panelBottom - 20, pw, "base64 / hash / texture URL", CONFIG.getSkullCustomTexture(), 512);
    }

    private void recomputeSkulls() {
        skullResults.clear();
        skullResults.addAll(Skulls.INSTANCE.search(skullQuery, 300));
    }

    private void renderSkull(GuiGraphicsExtractor g) {
        boolean helmetOn = CONFIG.armorPiece("HEAD").getOn();
        toggle(g, px, contentY, "Enable skull", CONFIG.getSpoofSkull(), helmetOn);
        if (skullSearchBox != null) {
            int bx = skullSearchBox.getX() + skullSearchBox.getWidth() + 6;
            boolean cust = Skulls.CUSTOM.equals(CONFIG.getSkullId());
            boolean hov = in(mx, my, new int[]{ px + pw - 50, contentY + 24, 50, 18 });
            g.fill(px + pw - 50, contentY + 24, px + pw, contentY + 42, cust ? SEL_BG : (hov ? SURFACE_HI : SURFACE));
            if (cust) outline(g, px + pw - 50, contentY + 24, 50, 18, ACCENT);
            g.text(font, "custom", px + pw - 50 + (50 - font.width("custom")) / 2, contentY + 29, cust ? TEXT : TEXT_MUTE, false);
        }
        if (skullView == null) return;
        int vx = skullView[0], vy = skullView[1], vw = skullView[2], vh = skullView[3];
        insetBox(g, vx, vy, vw, vh);

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
            g.fill(tx, ty, tx + skullTile, ty + skullTile, isSel ? SEL_BG : (hov ? SURFACE_HI : INSET));
            g.item(HeadItems.forHash(h.getTex()), tx + (skullTile - 16) / 2, ty + (skullTile - 16) / 2);
            if (isSel) outline(g, tx, ty, skullTile, skullTile, ACCENT);
        }
        g.disableScissor();
        scrollbar(g, skullView, skullScroll, skullMaxScroll);
    }

    // render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        this.mx = mouseX; this.my = mouseY;
        if (CONFIG.getBgImage()) g.blit(RenderPipelines.GUI_TEXTURED, BG_TEX, 0, 0, 0f, 0f, width, height, width, height, width, height);
        else g.fillGradient(0, 0, width, height, BG_TOP, BG_BOT);
        super.extractRenderState(g, mouseX, mouseY, partial);
        animateDoll();

        g.text(font, "yuri", px, 11, TEXT, false);
        drawMaster(g);
        drawTabs(g);
        g.fill(px, tabY + 14, width - 14, tabY + 15, LINE);

        dollCard(g);

        switch (tab) {
            case TAB_PROFILE, TAB_STATS -> {
                g.enableScissor(px, contentY, px + pw, panelBottom);
                for (Consumer<GuiGraphicsExtractor> op : leftDraws) op.accept(g);
                g.disableScissor();
                if (leftMaxScroll > 0) scrollbar(g, new int[]{ px, contentY, pw, leftViewportH }, leftScroll, leftMaxScroll);
            }
            case TAB_ARMOR -> renderArmor(g);
            case TAB_CAPE  -> renderCape(g);
            case TAB_SKULL -> renderSkull(g);
        }

        drawYuri(g);
    }

    private void drawTabs(GuiGraphicsExtractor g) {
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int[] r = tabRects[i];
            boolean sel = tab == i, hov = in(mx, my, new int[]{ r[0], r[1] - 2, r[2] + 4, 16 });
            g.text(font, TAB_NAMES[i], r[0], r[1], sel ? TEXT : (hov ? TEXT_MUTE : TEXT_FAINT), false);
            if (sel) g.fill(r[0], r[1] + 11, r[0] + r[2], r[1] + 12, ACCENT);
        }
    }

    private void drawMaster(GuiGraphicsExtractor g) {
        boolean on = CONFIG.getMasterEnabled();
        String lbl = "master";
        g.text(font, lbl, masterRect[0] - 6 - font.width(lbl), masterRect[1] + 2, on ? TEXT : TEXT_MUTE, false);
        pill(g, masterRect[0], masterRect[1], on, false);
    }

    private void drawYuri(GuiGraphicsExtractor g) {
        int x = yuriBtn[0], y = yuriBtn[1], w = yuriBtn[2], h = yuriBtn[3];
        boolean hov = in(mx, my, yuriBtn);
        int fill = CONFIG.getBgImage() ? (hov ? BTN_ON_HI : BTN_ON) : (hov ? BTN_OFF_HI : BTN_OFF);
        g.fill(x, y, x + w, y + h, fill);
        border(g, x, y, w, h, LINE);
        String lbl = "yuri";
        g.text(font, lbl, x + (w - font.width(lbl)) / 2, y + (h - 8) / 2, TEXT, false);
    }

    // input

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (e.button() == 0) {
            double x = e.x(), y = e.y();
            if (tab == TAB_ARMOR && handleColorPick(x, y)) return true;
            if (tab == TAB_ARMOR && handleTrimClick(x, y)) return true;
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
        if ((tab == TAB_PROFILE || tab == TAB_STATS) && leftMaxScroll > 0
            && in(x, y, new int[]{ px, contentY, pw, leftViewportH })) {
            int next = Math.max(0, Math.min(leftMaxScroll, leftScroll - (int) (sy * 16)));
            if (next != leftScroll) { leftScroll = next; rebuildWidgets(); }
            return true;
        }
        if (tab == TAB_CAPE && capeView != null && in(x, y, capeView)) {
            capeScroll = Math.max(0, Math.min(capeMaxScroll, capeScroll - (int) (sy * (capeTileH + 8))));
            return true;
        }
        if (tab == TAB_ARMOR && trimView != null && in(x, y, trimView)) {
            trimScroll = Math.max(0, Math.min(trimMaxScroll, trimScroll - (int) (sy * (trimTileH + 8))));
            return true;
        }
        if (tab == TAB_SKULL && skullView != null && in(x, y, skullView)) {
            skullScroll = Math.max(0, Math.min(skullMaxScroll, skullScroll - (int) (sy * (skullTile + 4))));
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    private boolean handleTrimClick(double x, double y) {
        if (selSlot.equals("HEAD") && CONFIG.getSpoofSkull()) return false;
        if (trimView == null || !in(x, y, trimView)) return false;
        int stepX = trimTileW + 8, stepY = trimTileH + 8;
        int lx = (int) (x - trimView[0]), c = lx / stepX;
        if (c < 0 || c >= trimCols || lx - c * stepX > trimTileW) return false;
        int r = (int) (y - trimView[1] + trimScroll) / stepY;
        int idx = r * trimCols + c;
        if (idx < 0 || idx >= trimPatternIds.size()) return false;
        String pid = trimPatternIds.get(idx);
        ArmorPiece p = CONFIG.armorPiece(selSlot);
        p.setTrimPattern(pid);
        if (!pid.isEmpty() && p.getTrimMaterial().isEmpty()) p.setTrimMaterial(DEFAULT_MAT);
        p.setOn(true);
        return true;
    }

    private boolean handleCapeClick(double x, double y) {
        syncBoxes();
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
        if (selSlot.equals("HEAD") && CONFIG.getSpoofSkull()) return false;
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
                minecraft.execute(() -> minecraft.setScreen(new CapeImageEditorScreen(p)));
            }
        }, "yuri-cape-picker").start();
    }

    @Override
    public void onClose() {
        syncAll();
        CONFIG.save();
        super.onClose();
    }

    private void syncAll() {
        syncConfig();
        syncBoxes();
    }

    private void syncConfig() {
        if (nameBox       != null) CONFIG.setFakeName(nameBox.getValue());
        if (levelBox      != null) CONFIG.setLevelText(levelBox.getValue());
        if (lobbyBox      != null) CONFIG.setLobbyText(lobbyBox.getValue());
        if (rankCustomBox != null) CONFIG.setRankText(rankCustomBox.getValue());
        for (Map.Entry<String, EditBox> e : skillBoxes.entrySet()) {
            int v = parseIntOrZero(e.getValue().getValue());
            if (v > 0) CONFIG.getSkillLevels().put(e.getKey(), v);
            else        CONFIG.getSkillLevels().remove(e.getKey());
        }
        for (Map.Entry<String, EditBox> e : slayerBoxes.entrySet()) {
            int v = parseIntOrZero(e.getValue().getValue());
            if (v > 0) CONFIG.getSlayerLevels().put(e.getKey(), v);
            else        CONFIG.getSlayerLevels().remove(e.getKey());
        }
        for (Map.Entry<String, EditBox> e : currencyBoxes.entrySet()) {
            String v = e.getValue().getValue().trim();
            if (!v.isEmpty()) CONFIG.getCurrencyValues().put(e.getKey(), v);
            else               CONFIG.getCurrencyValues().remove(e.getKey());
        }
    }

    private void syncBoxes() {
        if (capeNameBox != null && Capes.INSTANCE.isCustom(CONFIG.getCapeId())) {
            CapeSpoofer.INSTANCE.renameCape(CONFIG.getCapeId(), capeNameBox.getValue());
        }
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
        border(g, dx0, dy0, dx1 - dx0, dy1 - dy0, LINE);
        g.fill(dx0 + 1, dy1 - 17, dx1 - 1, dy1 - 1, 0x33000000);
        renderDoll(g, dx0 + 6, dy0 + 6, dx1 - 6, dy1 - 12, dollSize, dollYaw, dollTilt);
        String cap = tab == TAB_CAPE ? "back view" : "front view";
        g.text(font, cap, dx0 + (dx1 - dx0 - font.width(cap)) / 2, dy1 - 11, TEXT_FAINT, false);
    }

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
            lrs.yRot = 0f;
            lrs.xRot = lrs.pose != Pose.FALL_FLYING ? -tiltDeg : 0f;
            lrs.boundingBoxWidth = lrs.boundingBoxWidth / lrs.scale;
            lrs.boundingBoxHeight = lrs.boundingBoxHeight / lrs.scale;
            lrs.scale = 1.0F;
        }
        Vector3f translation = new Vector3f(0f, rs.boundingBoxHeight / 2f + 0.0625f, 0f);
        g.entity(rs, size, translation, rotation, xRotation, x0, y0, x1, y1);
    }

    // drawing helpers

    private void toggle(GuiGraphicsExtractor g, int x, int y, String label, boolean on, boolean locked) {
        g.text(font, locked ? label + " (locked)" : label, x, y + 2, locked ? TEXT_FAINT : TEXT, false);
        int tx = x + pw - PILL_W, ty = y - 1;
        pill(g, tx, ty, on, locked);
    }

    private void pill(GuiGraphicsExtractor g, int x, int y, boolean on, boolean locked) {
        int track = locked ? TOG_OFF : (on ? BTN_ON : BTN_OFF);
        g.fill(x, y, x + PILL_W, y + PILL_H, track);
        border(g, x, y, PILL_W, PILL_H, LINE);
        int kx = on ? x + PILL_W - 9 : x + 1;
        g.fill(kx, y + 1, kx + 8, y + PILL_H - 1, locked ? TEXT_FAINT : 0xFFF2F2F5);
    }

    private void cycler(GuiGraphicsExtractor g, int x, int y, String label, String value) {
        int w = pw, h = 20;
        boolean lh = in(mx, my, new int[]{ x, y, 18, h });
        boolean rh = in(mx, my, new int[]{ x + w - 18, y, 18, h });
        g.fill(x, y, x + 18, y + h, lh ? SURFACE_HI : SURFACE); border(g, x, y, 18, h, LINE);
        g.fill(x + w - 18, y, x + w, y + h, rh ? SURFACE_HI : SURFACE); border(g, x + w - 18, y, 18, h, LINE);
        g.text(font, "<", x + 6, y + 6, TEXT, false);
        g.text(font, ">", x + w - 12, y + 6, TEXT, false);
        int midX = x + 20, midW = w - 40;
        g.fill(midX, y, midX + midW, y + h, INSET); border(g, midX, y, midW, h, LINE);
        g.text(font, label, midX + 6, y + 3, TEXT, false);
        g.text(font, value, midX + 6, y + 12, TEXT_MUTE, false);
    }

    private void scrollbar(GuiGraphicsExtractor g, int[] v, int scroll, int maxScroll) {
        if (maxScroll <= 0) return;
        int vx = v[0], vy = v[1], vw = v[2], vh = v[3];
        int thumbH = Math.max(16, (int) ((float) vh / (vh + maxScroll) * vh));
        int thumbY = vy + (int) ((float) scroll / maxScroll * (vh - thumbH));
        g.fill(vx + vw - 3, vy, vx + vw, vy + vh, INSET);
        g.fill(vx + vw - 3, thumbY, vx + vw, thumbY + thumbH, 0xFF55555E);
    }

    private void ring(GuiGraphicsExtractor g, int cx, int cy) {
        g.fill(cx - 3, cy - 1, cx + 3, cy + 1, ACCENT);
        g.fill(cx - 1, cy - 3, cx + 1, cy + 3, ACCENT);
    }

    private static void insetBox(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, INSET);
        border(g, x, y, w, h, LINE);
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

    // legacy §/& colored text via the non-hooked string overload
    private int drawLegacy(GuiGraphicsExtractor g, String s, int x, int y, int defColor) {
        int color = defColor, cx = x;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < s.length()) {
                if (buf.length() > 0) {
                    g.text(font, buf.toString(), cx, y, 0xFF000000 | color, false);
                    cx += font.width(buf.toString());
                    buf.setLength(0);
                }
                char code = Character.toLowerCase(s.charAt(++i));
                int d = Character.digit(code, 16);
                if (d >= 0 && d < 16) color = LEGACY[d];
                else if (code == 'r') color = defColor;
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            g.text(font, buf.toString(), cx, y, 0xFF000000 | color, false);
            cx += font.width(buf.toString());
        }
        return cx - x;
    }

    private int plainWidth(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < s.length()) i++;
            else b.append(c);
        }
        return font.width(b.toString());
    }

    private EditBox makeBox(int x, int y, int w, String hint, String value, int maxLen) {
        EditBox box = new TransEditBox(font, x, y, w, 18, Component.literal(hint));
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

    private static int parseIntOrZero(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
