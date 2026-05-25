    # CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

> **Note:** `java` must be on PATH and `JAVA_HOME` must point to a JDK 21 installation before running Gradle. The CI uses Java 25, but the mod targets Java 21.

```powershell
# Build the mod (produces jar in build/libs/)
.\gradlew build

# Run the Minecraft client in dev mode
.\gradlew runClient

# Run client-side game tests
.\gradlew ClientGametest

# Generate Minecraft sources (useful for browsing vanilla code)
.\gradlew genSources
```

## Architecture Overview

This is a **Fabric client-side mod** for Minecraft 1.21.11 targeting Hypixel SkyBlock. It spoofs visual display elements (player name, rank, SkyBlock level, lobby ID) without modifying network packets — purely a visual/rendering-layer override.

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
| `src/client/kotlin/.../client/gui/SpooferScreen.kt` | Full settings GUI; rank grid, text inputs, real-time preview |
| `src/client/java/.../client/mixin/` | Five mixin hooks: tab list, scoreboard team, entity nametag, GUI drawString, placeholder |

### Mixin Hooks (where spoofing is injected)

- `PlayerTabOverlayMixin` → `getNameForDisplay()` — tab list names
- `PlayerTeamMixin` → `formatNameForTeam()` — scoreboard team names
- `EntityRendererMixin` → `getNameTag()` — above-head nametags
- `GuiGraphicsMixin` → `drawString(Component)` — GUI labels and HUD text

### Spoofer Logic (`Spoofer.kt`)

The engine converts a `Component` to a §-coded legacy string, then applies targeted regex substitutions:
- SkyBlock level: `[000]` pattern (with optional emblem prefix)
- Rank bracket: `[MVP+]` / `[VIP]` etc.
- Player name coloring based on rank prefix color
- Lobby/server ID
- Display name

It distinguishes **chat mode** (rank bracket shown) from **tab mode** (`showRank=false`, only color applied). After substitution, it re-parses back to a `Component` using `LegacyComponentSerializer`.

## Technology Stack

- **Minecraft**: 1.21.11 (official Mojang mappings)
- **Mod loader**: Fabric Loader 0.19.2 + Fabric API 0.141.4
- **Language**: Kotlin 2.3.21 (via Fabric Language Kotlin 1.13.11)
- **Build**: Gradle with Fabric Loom (loom-remap fork, v1.16-SNAPSHOT)
- **Tests**: Fabric `ClientGameTest` framework (`SpooferGuiGameTest.kt`)

## Research Docs

- `spoofer.md` — spoofing strategy research, surface map, reference mods, SkyBlock color specs, edge cases
- `skidded-features.md` — attribution for borrowed code
