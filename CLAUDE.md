    # CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

> **Note:** Minecraft 26.1.2 ships **unobfuscated** (it runs on Java 25 and no longer publishes
> client mappings), so the build uses the **no-remap** Loom plugin (`net.fabricmc.fabric-loom`,
> `disableObfuscation=true`) instead of the obfuscated remap variant. `java`/`JAVA_HOME` should point
> to a JDK 25 (the mod still compiles at release 21). Gradle 9.5.1 is required by Loom 1.17.11.

```powershell
# Build the mod (produces jar in build/libs/)
.\gradlew build

# Run the Minecraft client in dev mode
.\gradlew runClient

# Run client-side game tests
.\gradlew runClientGametest

# Generate Minecraft sources (useful for browsing vanilla code)
.\gradlew genSources
```

## Architecture Overview

This is a **Fabric client-side mod** for Minecraft 26.1.2 targeting Hypixel SkyBlock. It spoofs visual display elements (player name, rank, SkyBlock level, lobby ID) and the local player's own appearance (cape, armor color, custom skull/head) without modifying network packets — purely a visual/rendering-layer override. All appearance spoofing is **self-view only** (it changes what you see on your own client, not what other players see).

The codebase is split into two source sets:
- `src/main/` — server-side initializer (minimal; mostly a stub)
- `src/client/` — all real logic; Kotlin + Java mixins

### Core Data Flow

```
User opens GUI (Ctrl+H) → SpooferScreen → SpoofConfig (persisted JSON)
                                                    ↓
Minecraft renders text → Mixin hook intercepts → Spoofer.spoof(component)
                                                    ↓
                              Component → legacy §-string → regex replace → re-parse → return
```

### Key Files

| File | Role |
|------|------|
| `src/client/kotlin/.../client/spoof/Spoofer.kt` | Core text replacement engine; converts components to §-strings, applies regex substitutions, re-parses |
| `src/client/kotlin/.../client/spoof/SpoofConfig.kt` | JSON config (Fabric config dir); master toggle + per-feature toggles + custom text fields |
| `src/client/kotlin/.../client/spoof/Ranks.kt` | Rank preset table with §-coded prefixes and derived name colors |
| `src/client/java/.../client/gui/SpooferScreen.java` | Text-spoof settings GUI (opened with `H`): rank grid, name/level/lobby/skill/slayer/currency inputs |
| `src/client/java/.../client/gui/AppearanceEditorScreen.java` | Appearance editor (opened by the inventory paintbrush): live player paper-doll + Armor / Cape / Skull tabs, color wheel, trim pickers, icon galleries |
| `src/client/java/.../client/gui/PaintbrushButton.java` | Icon button added to the inventory (via Fabric `ScreenEvents.AFTER_INIT`) that opens the editor |
| `src/client/kotlin/.../client/spoof/CapeSpoofer.kt`, `Capes.kt` | Cape override; official Mojang capes referenced by public hash from textures.minecraft.net + custom file/URL |
| `src/client/kotlin/.../client/spoof/ArmorSpoofer.kt`, `Trims.kt`, `Colors.kt`, `ColorWheel.kt` | Per-slot fake armor (dyed leather + trim) + trim registry helpers + HSV color wheel |
| `src/client/kotlin/.../client/spoof/SkullSpoofer.kt`, `Skulls.kt`, `HeadIcons.kt` | Custom skull/head helmet + bundled SkyBlock head DB (`assets/yuri-spoofer/skulls.json`) + async gallery icons |
| `src/client/java/.../client/mixin/` | Mixin hooks (text + render-state) |

### Mixin Hooks (where spoofing is injected)

Text (spoofing surfaces):
- `PlayerTabOverlayMixin` → `getNameForDisplay()` — tab list names
- `PlayerTeamMixin` → `formatNameForTeam()` — scoreboard team names
- `EntityRendererMixin` → `getNameTag()` — above-head nametags
- `GuiGraphicsMixin` → `GuiGraphicsExtractor.text(Component)` — GUI labels and HUD text (26.x renamed `GuiGraphics.drawString`)

Appearance (local player only — one hook, render-state edits, all vanilla rendering):
- `AvatarRendererMixin` → `AvatarRenderer.extractRenderState()` — for the local player it: swaps the `PlayerSkin` cape, sets `wornHeadType`/`wornHeadProfile` to a custom skull, and sets fake dyed+trimmed leather armor on the `*Equipment` slot fields. No per-layer mixins are needed — the vanilla CapeLayer / CustomHeadLayer / HumanoidArmorLayer draw the overridden render state.

### Spoofer Logic (`Spoofer.kt`)

The engine converts a `Component` to a §-coded legacy string, then applies targeted regex substitutions:
- SkyBlock level: `[000]` pattern (with optional emblem prefix)
- Rank bracket: `[MVP+]` / `[VIP]` etc.
- Player name coloring based on rank prefix color
- Lobby/server ID
- Display name

It distinguishes **chat mode** (rank bracket shown) from **tab mode** (`showRank=false`, only color applied). After substitution, it re-parses back to a `Component` using `LegacyComponentSerializer`.

## Technology Stack

- **Minecraft**: 26.1.2 (unobfuscated — official names, no client mappings)
- **Mod loader**: Fabric Loader 0.19.3 + Fabric API 0.152.1+26.1.2
- **Language**: Kotlin 2.3.21 (via Fabric Language Kotlin 1.13.11)
- **Build**: Gradle 9.5.1 with Fabric Loom no-remap plugin `net.fabricmc.fabric-loom` v1.17.11 (`disableObfuscation=true`); mod deps use plain `implementation` (no `modImplementation` in no-remap mode)
- **Tests**: Fabric `ClientGameTest` framework (`SpooferGuiGameTest.kt`, task `runClientGametest`)

## Research Docs

- `spoofer.md` — spoofing strategy research, surface map, reference mods, SkyBlock color specs, edge cases
- `skidded-features.md` — attribution for borrowed code
