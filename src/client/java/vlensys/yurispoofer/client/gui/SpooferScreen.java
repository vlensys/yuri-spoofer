package vlensys.yurispoofer.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import vlensys.yurispoofer.client.spoof.Currencies;
import vlensys.yurispoofer.client.spoof.Ranks;
import vlensys.yurispoofer.client.spoof.Skills;
import vlensys.yurispoofer.client.spoof.Slayers;
import vlensys.yurispoofer.client.spoof.SpoofConfig;

public class SpooferScreen extends Screen {

    private static final SpoofConfig CONFIG  = SpoofConfig.INSTANCE;
    private static final Ranks       RANKS   = Ranks.INSTANCE;
    private static final Skills      SKILLS  = Skills.INSTANCE;
    private static final Slayers     SLAYERS = Slayers.INSTANCE;
    private static final Currencies  CURRS   = Currencies.INSTANCE;

    private static final int HEADER_H  = 28;
    private static final int ROW_H     = 22;
    private static final int SECTION_H = 16;
    private static final int GAP       = 6;
    private static final int TOGGLE_W  = 52;
    private static final int TOGGLE_H  = 18;

    private int scroll    = 0;
    private int maxScroll = 0;
    private int cx, cw;

    private boolean scheduledRebuild = false;

    private EditBox nameBox, levelBox, lobbyBox, rankCustomBox;
    private final Map<String, EditBox> skillBoxes    = new LinkedHashMap<>();
    private final Map<String, EditBox> slayerBoxes   = new LinkedHashMap<>();
    private final Map<String, EditBox> currencyBoxes = new LinkedHashMap<>();

    private final List<int[]>    dividers = new ArrayList<>();
    private final List<Object[]> lbls     = new ArrayList<>();

    public SpooferScreen() {
        super(Component.literal("yuri spoofer"));
    }

    /* ── lifecycle ──────────────────────────────────────────────────────────── */

    @Override
    protected void init() {
        syncConfig();
        nameBox = null; levelBox = null; lobbyBox = null; rankCustomBox = null;
        skillBoxes.clear(); slayerBoxes.clear(); currencyBoxes.clear();
        dividers.clear(); lbls.clear();

        cw = Math.min(width - 16, 460);
        cx = (width - cw) / 2;

        addRenderableWidget(Button.builder(
            Component.literal("Master: " + (CONFIG.getMasterEnabled() ? "ON" : "OFF")),
            b -> { CONFIG.setMasterEnabled(!CONFIG.getMasterEnabled()); scheduledRebuild = true; }
        ).pos(cx + cw - 86, 5).size(84, 18).build());

        int y = HEADER_H + GAP;

        y = section(y, "Profile");
        y = toggleRow(y, "Spoof name", CONFIG.getSpoofName(),
            v -> { CONFIG.setSpoofName(v); scheduledRebuild = true; });
        nameBox = editRight(y, "Name", CONFIG.getFakeName(), 64, "Fake name", 170);
        y += ROW_H;

        y = section(y + GAP, "Rank");
        y = toggleRow(y, "Spoof rank", CONFIG.getSpoofRank(),
            v -> { CONFIG.setSpoofRank(v); scheduledRebuild = true; });
        y = rankGrid(y);
        if (Ranks.CUSTOM.equals(CONFIG.getRankPreset())) {
            rankCustomBox = editRight(y, "Custom rank", CONFIG.getRankText(), 64, "&c[&6OWNER&c]", cw - 82);
            y += ROW_H;
        }

        y = section(y + GAP, "Location");
        y = toggleRow(y, "Spoof level", CONFIG.getSpoofLevel(),
            v -> { CONFIG.setSpoofLevel(v); scheduledRebuild = true; });
        levelBox = editRight(y, "Level", CONFIG.getLevelText(), 8, "500", 52);
        y += ROW_H;
        y = toggleRow(y, "Spoof lobby", CONFIG.getSpoofLobby(),
            v -> { CONFIG.setSpoofLobby(v); scheduledRebuild = true; });
        lobbyBox = editRight(y, "Lobby", CONFIG.getLobbyText(), 64, "YURI01", 96);
        y += ROW_H;

        y = section(y + GAP, "Skills");
        y = toggleRow(y, "Spoof skills", CONFIG.getSpoofSkills(),
            v -> { CONFIG.setSpoofSkills(v); scheduledRebuild = true; });
        y = levelGrid(y, SKILLS.getALL(), skillBoxes, CONFIG.getSkillLevels());

        y = section(y + GAP, "Slayers");
        y = toggleRow(y, "Spoof slayers", CONFIG.getSpoofSlayers(),
            v -> { CONFIG.setSpoofSlayers(v); scheduledRebuild = true; });
        y = levelGrid(y, SLAYERS.getALL(), slayerBoxes, CONFIG.getSlayerLevels());

        y = section(y + GAP, "Currency");
        y = toggleRow(y, "Spoof currency", CONFIG.getSpoofCurrency(),
            v -> { CONFIG.setSpoofCurrency(v); scheduledRebuild = true; });
        y = currencyGrid(y);

        maxScroll = Math.max(0, y + GAP - this.height);
        scroll    = Math.min(scroll, maxScroll);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (scheduledRebuild) {
            scheduledRebuild = false;
            rebuildWidgets();
        }
        super.render(g, mx, my, partial);

        // title (fixed)
        g.drawString(font, "yuri spoofer", cx + 4, 10, 0xFFFFFFFF, false);
        // header bottom rule
        g.fill(cx, HEADER_H - 1, cx + cw, HEADER_H, 0xFF555555);

        // section rules
        for (int[] d : dividers) {
            if (d[1] >= HEADER_H - 1 && d[1] < this.height)
                g.fill(d[0], d[1], d[2], d[1] + 1, 0xFF555555);
        }
        // row labels
        for (Object[] l : lbls) {
            int ly = (int) l[1];
            if (ly >= HEADER_H - font.lineHeight && ly < this.height)
                g.drawString(font, (String) l[2], (int) l[0], ly, (int) l[3], false);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (super.mouseScrolled(mx, my, dx, dy)) return true;
        int next = (int) Math.max(0, Math.min(maxScroll, scroll - dy * 12));
        if (next != scroll) { scroll = next; rebuildWidgets(); }
        return true;
    }

    @Override
    public void onClose() {
        syncConfig();
        CONFIG.save();
        super.onClose();
    }

    /* ── config sync ────────────────────────────────────────────────────────── */

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

    /* ── section / row builders ─────────────────────────────────────────────── */

    private int section(int logY, String title) {
        int sy = logY - scroll;
        int lw = font.width(title);
        lbl(cx, sy + 3, title, 0xFF999999);
        dividers.add(new int[]{cx + lw + 4, sy + 7, cx + cw});
        return logY + SECTION_H;
    }

    private int toggleRow(int logY, String text, boolean current, Consumer<Boolean> set) {
        int wy = logY - scroll;
        lbl(cx + 2, wy + 5, text, 0xFFFFFFFF);
        maybeAdd(Button.builder(
            Component.literal(current ? "ON" : "OFF"),
            b -> set.accept(!current)
        ).pos(cx + cw - TOGGLE_W - 2, wy + 1).size(TOGGLE_W, TOGGLE_H).build());
        return logY + ROW_H;
    }

    private EditBox editRight(int logY, String label, String value, int maxLen, String hint, int boxW) {
        int wy = logY - scroll;
        lbl(cx + 2, wy + 5, label, 0xFFDDDDDD);
        return newBox(cx + cw - boxW, wy + 2, boxW, 18, label, maxLen, value, hint);
    }

    private int rankGrid(int logY) {
        List<String> order = RANKS.getORDER();
        int perRow = 5, gap = 3;
        int btnW = (cw - gap * (perRow - 1)) / perRow;
        int col = 0, row = 0;
        for (String key : order) {
            boolean sel = key.equals(CONFIG.getRankPreset());
            String prefix = RANKS.prefixFor(key, "");
            Component label = rankLabel(prefix, key, sel);
            String k = key;
            maybeAdd(Button.builder(label,
                b -> { CONFIG.setRankPreset(k); scheduledRebuild = true; }
            ).pos(cx + col * (btnW + gap), logY + row * ROW_H - scroll).size(btnW, ROW_H - 3).build());
            if (++col >= perRow) { col = 0; row++; }
        }
        return logY + ((order.size() + perRow - 1) / perRow) * ROW_H;
    }

    private static Component rankLabel(String prefix, String key, boolean selected) {
        Component text = prefix.isEmpty()
            ? Component.literal(key).withStyle(ChatFormatting.GRAY)
            : parseLegacy(prefix);
        if (selected) return Component.literal("> ").append(text);
        return text;
    }

    private static Component parseLegacy(String s) {
        MutableComponent root = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                if (buf.length() > 0) {
                    root.append(Component.literal(buf.toString()).setStyle(style));
                    buf.setLength(0);
                }
                style = applyLegacyCode(style, Character.toLowerCase(s.charAt(++i)));
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) root.append(Component.literal(buf.toString()).setStyle(style));
        return root;
    }

    private static Style applyLegacyCode(Style style, char code) {
        ChatFormatting cf = ChatFormatting.getByCode(code);
        if (cf == null) return style;
        if (cf == ChatFormatting.RESET) return Style.EMPTY;
        if (cf.isColor()) return Style.EMPTY.withColor(cf);
        return style.applyFormat(cf);
    }

    private <T> int levelGrid(int logY, List<T> entries, Map<String, EditBox> boxMap, Map<String, Integer> levels) {
        int colStep = cw / 2;
        int colW    = colStep - 4;
        int boxW    = 42;
        for (int i = 0; i < entries.size(); i++) {
            T e      = entries.get(i);
            String name = entryName(e);
            int    cap  = entryCap(e);
            int rx = cx + (i % 2) * colStep;
            int wy = logY + (i / 2) * ROW_H - scroll;
            lbl(rx + 2, wy + 5, name, 0xFFFFFFFF);
            Integer cur = levels.get(name);
            EditBox box = newBox(rx + colW - boxW + 4, wy + 2, boxW, 18, name, 3,
                cur != null && cur > 0 ? String.valueOf(cur) : "", String.valueOf(cap));
            boxMap.put(name, box);
        }
        return logY + ((entries.size() + 1) / 2) * ROW_H;
    }

    private int currencyGrid(int logY) {
        List<Currencies.Currency> all = CURRS.getALL();
        int colStep = cw / 2;
        int colW    = colStep - 4;
        int boxW    = 104;
        for (int i = 0; i < all.size(); i++) {
            Currencies.Currency c = all.get(i);
            int rx = cx + (i % 2) * colStep;
            int wy = logY + (i / 2) * ROW_H - scroll;
            lbl(rx + 2, wy + 5, c.getName(), 0xFFFFFFFF);
            EditBox box = newBox(rx + colW - boxW + 4, wy + 2, boxW, 18, c.getName(), 20,
                CONFIG.getCurrencyValues().getOrDefault(c.getName(), ""), "value");
            currencyBoxes.put(c.getName(), box);
        }
        return logY + ((all.size() + 1) / 2) * ROW_H;
    }

    /* ── widget / render helpers ────────────────────────────────────────────── */

    private EditBox newBox(int x, int y, int w, int h, String name, int maxLen, String value, String hint) {
        EditBox box = new EditBox(font, x, y, w, h, Component.literal(name));
        box.setMaxLength(maxLen);
        box.setValue(value);
        box.setHint(Component.literal(hint));
        maybeAdd(box);
        return box;
    }

    private void maybeAdd(AbstractWidget w) {
        if (w.getY() + w.getHeight() > HEADER_H && w.getY() < this.height)
            addRenderableWidget(w);
    }

    private void lbl(int x, int y, String text, int color) {
        lbls.add(new Object[]{x, y, text, color});
    }

    /* ── util ───────────────────────────────────────────────────────────────── */

    private static String entryName(Object e) {
        if (e instanceof Skills.Skill)   return ((Skills.Skill)   e).getName();
        if (e instanceof Slayers.Slayer) return ((Slayers.Slayer) e).getName();
        throw new IllegalArgumentException();
    }

    private static int entryCap(Object e) {
        if (e instanceof Skills.Skill)   return ((Skills.Skill)   e).getCap();
        if (e instanceof Slayers.Slayer) return ((Slayers.Slayer) e).getCap();
        throw new IllegalArgumentException();
    }

    private static int parseIntOrZero(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
