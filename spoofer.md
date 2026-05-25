# Hypixel SkyBlock Client-Side Spoofing Research

Research date: 2026-05-23

Scope: client-side visual replacement for Hypixel/SkyBlock values. This covers chat, scoreboard, tab, nametags, GUIs, inventory slot overlays, tooltips, profile-style displays, item rendering, armor rendering, and HUD overlays. It does not cover sending fake data to Hypixel or changing what other players see.

## Core concept

Every useful "spoofer" found in existing mods is one of these:

1. **Global text replacement**
   - Intercept `Font`, `FontRenderer`, `StringDecomposer`, or text wrapping.
   - Replace a username/rank/value just before text is measured or drawn.
   - Affects many surfaces automatically: chat, tab, scoreboard, nametags, titles, signs, item tooltips, mod GUIs, vanilla screens.

2. **Targeted vanilla UI replacement**
   - Hook a specific renderer such as scoreboard, tab overlay, tooltip creation, or inventory stack name.
   - Safer and less invasive, but every surface needs its own hook.

3. **Full HUD rebuild**
   - Hide vanilla tab/scoreboard and render a custom HUD from parsed vanilla data.
   - Used by custom scoreboard and tab HUD mods.

4. **Item/NBT visual substitution**
   - Keep the real item server-side, but replace display name, model, skull texture, dye color, glint, tooltip lines, or slot text client-side.

5. **Synthetic progression display**
   - Keep real Hypixel data, but calculate alternate/overflow/custom-goal values and show them in overlays, chat, or tooltips. This is how mods show skill/garden overflow levels or custom skill goals.

The most important lesson: **tab and scoreboard are not special if you hook global text rendering**, but they are special if you want precise replacement without touching all text. A robust spoofer usually needs both: broad text interception plus targeted fixes for scoreboard lines, tooltip construction, and tab display names.

## Surface map

| Surface | Vanilla path/mod path | What can be spoofed | Common hook |
| --- | --- | --- | --- |
| Chat messages | chat component render and chat received events | username, rank, SkyBlock level prefix, added fake stat tag, fake level-up messages | `Font`/`FontRenderer`, `StringDecomposer`, `ClientChatReceivedEvent`, chat component mutation |
| Chat input/autocomplete | chat screen text field and suggestions | local typed name or generated suggestions | text field render/string replacement; less common |
| Tab list/player list | `PlayerTabOverlay`, player info display name | rank, username, SkyBlock level prefix, stat tags, hidden vanilla tab | `PlayerTabOverlay#getNameForDisplay`, tab render method, global font hook, HUD element replacement |
| Scoreboard/sidebar | `Gui.displayScoreboardSidebar`, scoreboard objective lines | Rank line, purse/bits/date/location, custom scoreboard values, fake text lines | scoreboard render mixin, packet/sidebar parser, global font hook |
| Nametag above player | entity label render | username, rank-like prefix, level/stats tag | entity renderer nametag hook, global font hook |
| Titles/actionbar | title/actionbar render | username/rank/value if text contains it; skill XP actionbar replacement | global font hook, actionbar update event |
| Inventory GUI item names | `ItemStack#getHoverName`, custom name component | item rename, skill menu item name, garden level item name | `ItemStack#getHoverName` return modification; tooltip event mutation |
| Inventory tooltip lore | item tooltip construction/rendering | rank tooltip, skill levels, overflow XP, item color/glint info | tooltip constructor, tooltip event, `ItemStack#getTooltip` redirects |
| Inventory slot overlays | custom render after slot draw | skill level number, pet level, minion tier, enchant level | slot draw/slot text render |
| Profile viewer/custom GUI | mod-owned screen | skills, levels, stats, API data, fake/override display | mod screen data model/render widgets |
| Armor/entity render | armor layers, item renderer | fake armor dye, fake trim, fake glint, fake helmet texture | `DyedItemColor`, armor layer, render item mixins |
| Held item/item icon | item stack render | fake item model, skull, glint, dye, durability bar | item renderer and stack substitution |
| Tooltips in custom mod screens | custom widgets and tooltip wrappers | rank, name, skill text, item text | global text hook or mod-specific tooltip event |

## RankSpoof

Sources:

- Repository: https://github.com/cxntered/RankSpoof
- Modern `RankComponentModifier`: https://raw.githubusercontent.com/cxntered/RankSpoof/modern/src/main/java/dev/cxntered/rankspoof/component/RankComponentModifier.java
- Modern scoreboard mixin: https://raw.githubusercontent.com/cxntered/RankSpoof/modern/src/main/java/dev/cxntered/rankspoof/mixin/GuiMixin.java
- Modern font mixin: https://raw.githubusercontent.com/cxntered/RankSpoof/modern/src/main/java/dev/cxntered/rankspoof/mixin/FontMixin.java
- Modern tooltip mixin: https://raw.githubusercontent.com/cxntered/RankSpoof/modern/src/main/java/dev/cxntered/rankspoof/mixin/ClientTextTooltipMixin.java
- Modern wrap mixin: https://raw.githubusercontent.com/cxntered/RankSpoof/modern/src/main/java/dev/cxntered/rankspoof/mixin/ComponentRenderUtilsMixin.java
- Legacy main replacement: https://raw.githubusercontent.com/cxntered/RankSpoof/legacy/src/main/java/dev/cxntered/rankspoof/RankSpoof.java
- Legacy font mixin: https://raw.githubusercontent.com/cxntered/RankSpoof/legacy/src/main/java/dev/cxntered/rankspoof/mixin/minecraft/MixinFontRenderer.java
- Legacy scoreboard mixin: https://raw.githubusercontent.com/cxntered/RankSpoof/legacy/src/main/java/dev/cxntered/rankspoof/mixin/minecraft/MixinGuiIngame.java
- Legacy tooltip mixin: https://raw.githubusercontent.com/cxntered/RankSpoof/legacy/src/main/java/dev/cxntered/rankspoof/mixin/minecraft/MixinGuiScreen.java
- Legacy VanillaHUD compatibility: https://raw.githubusercontent.com/cxntered/RankSpoof/legacy/src/main/java/dev/cxntered/rankspoof/mixin/compatibility/MixinScoreboard_VanillaHUD.java

### What it changes

RankSpoof is the clearest rank spoofer reference. It replaces the local player's rank prefix client-side.

It supports:

- normal ranked player text, e.g. `[MVP+] Name`
- unranked gray player text, e.g. `Name`
- rank-only username style, where the rank prefix is missing but the username is rank-colored
- profile/character tooltip line `Rank: ...`
- SkyBlock scoreboard line `Rank: ...`
- global rendered text that contains the local username
- wrapped text, so width/wrapping remains consistent after replacement

### Where it appears

| Location | Modern implementation | Legacy implementation | Notes |
| --- | --- | --- | --- |
| Chat | `FontMixin` on text preparation and width | `MixinFontRenderer.renderString` and `getStringWidth` | Any rendered line containing the local name can change. |
| Tab list | `FontMixin` usually catches rendered tab text | `MixinFontRenderer` catches tab text | Modern component replacement preserves styles better. |
| Scoreboard | `GuiMixin` targets `Rank: ` line | `MixinGuiIngame` redirects `ScorePlayerTeam.formatPlayerName` in scoreboard render | This targeted hook exists because scoreboard formatting can be awkward. |
| Tooltips | `ClientTextTooltipMixin` changes `Rank: ` tooltip | `MixinGuiScreen.renderToolTip` changes Character Information tooltip | Important for SkyBlock menu/profile tooltip surfaces. |
| Wrapped GUI text | `ComponentRenderUtilsMixin` modifies wrap input | no exact modern equivalent needed in old string path | Prevents bad line wrapping after spoof text gets wider. |
| Modded VanillaHUD scoreboard | not needed unless same mod exists modern | `MixinScoreboard_VanillaHUD` | Compatibility layer for third-party scoreboard renderer. |

### How modern RankSpoof works

Modern Minecraft text is component-based. `RankComponentModifier.replaceRank(Component)`:

- checks if the component contains the local username
- builds a spoofed rank component from legacy formatting codes
- walks sibling components
- detects rank prefix starts with a regex like `\[[A-Za-z]+`
- supports split ranks like `[MVP`, `++`, `]`
- finds the point where the rank ends and username begins
- replaces the rank+name segment while preserving inherited style, hover, click, insertion, and font
- avoids replacing team prefixes, because those can look like bold single-letter team labels

This is better than raw string replacement because Hypixel rank components are often split across siblings with separate colors.

### How legacy RankSpoof works

Legacy 1.8.9 RankSpoof uses plain strings:

- detects the local username
- ignores team-prefix lines matching bold team letters
- uses regexes for ranked text, no-rank gray text, and player-name-only colored text
- replaces the first matching segment with configured rank text
- hooks both render and width measurement

The width hook is important. If you only change render text, layout can clip or overlap because the UI measured the old string.

### RankSpoof color support

Modern config accepts `&` formatting codes, converts them to `§`, and shows a YACL preview. It also documents color/formatting codes and supports RGB-style `#RRGGBB` input in the UI description.

Default spoofed rank in modern branch is `&c[&6ዞ&c]`, which is styled like a special/admin-like prefix, but any text can be configured.

## FakeName / Name Changer

Sources:

- Repository: https://github.com/ayleafs/fake-names
- README: https://raw.githubusercontent.com/ayleafs/fake-names/master/README.md
- ASM transformer: https://raw.githubusercontent.com/ayleafs/fake-names/master/src/main/java/me/leafs/fakename/asm/FontTransformer.java
- Name replacement: https://raw.githubusercontent.com/ayleafs/fake-names/master/src/main/java/me/leafs/fakename/utils/NameUtils.java
- Hypixel forum thread: https://hypixel.net/threads/forge-1-8-9-name-changer.3357455/

### What it changes

FakeName replaces the local player's real IGN with a configured fake name on-screen. It is not SkyBlock-specific, but it is directly relevant because it demonstrates broad visual replacement.

### Where it appears

| Location | Coverage |
| --- | --- |
| Chat | yes, because chat text is rendered by `FontRenderer` |
| Tab list | yes after tab support was added; broad font hook covers common tab text |
| Scoreboard | yes if the scoreboard line is rendered by `FontRenderer` and includes the name |
| Titles | likely yes if title text reaches hooked font methods |
| Mod GUIs/messages | likely yes if they use vanilla font methods |
| Nametags | likely yes if rendered through hooked string methods |

### How it works

FakeName uses ASM instead of mixins. It transforms `net.minecraft.client.gui.FontRenderer` and injects a call at the beginning of these methods:

- `getStringWidth`
- `renderStringAtPos`
- obfuscated equivalents

The injected code runs `NameUtils.apply(text)`. That method:

- reads the configured fake name
- reads the real account name from the session profile
- returns early if the real name is not in the input
- colorizes the fake name
- appends reset formatting if color codes were used
- does a plain `input.replace(realName, fakeName)`

This is a blunt but highly effective pattern. It catches almost every vanilla text surface because almost every text surface eventually uses font rendering.

### Tradeoff

The upside is coverage. The downside is false positives and conflict risk:

- it can replace inside unrelated text if the username appears there
- it depends on vanilla font paths
- it can conflict with NickHider, RankSpoof, or any other font-transforming mod
- component style/hover/click metadata is not preserved because the replacement is string-based

## Nick Hider

Sources:

- CurseForge page: https://www.curseforge.com/minecraft/mc-mods/nick-hider
- Repository: https://github.com/przxmus/nick-hider
- `TextSanitizer`: https://raw.githubusercontent.com/przxmus/nick-hider/main/src/main/java/dev/przxmus/nickhider/core/TextSanitizer.java
- `IdentityMaskingService`: https://raw.githubusercontent.com/przxmus/nick-hider/main/src/main/java/dev/przxmus/nickhider/core/IdentityMaskingService.java
- `StringDecomposerMixin`: https://raw.githubusercontent.com/przxmus/nick-hider/main/src/main/java/dev/przxmus/nickhider/mixin/StringDecomposerMixin.java
- `PlayerTabOverlayMixin`: https://raw.githubusercontent.com/przxmus/nick-hider/main/src/main/java/dev/przxmus/nickhider/mixin/PlayerTabOverlayMixin.java
- Older Sk1er Nick Hider forum page: https://hypixel.net/threads/nick-hider-mod.1753591/

### What it changes

Nick Hider is privacy masking rather than flex spoofing, but the technical pattern is directly useful:

- local player name
- other player names
- skins
- capes
- UUID strings/prefixes
- tab display names
- text in common vanilla render paths

The CurseForge page explicitly describes it as masking identity details in common vanilla text/rendering paths and notes that third-party custom UI pipelines may still show unmasked data.

### Where it appears

| Location | Mechanism |
| --- | --- |
| Chat | `StringDecomposerMixin` sanitizes formatted strings before decomposition/render |
| Tab list | `PlayerTabOverlayMixin` replaces `getNameForDisplay` return value |
| Scoreboard | global text sanitization can catch names rendered in sidebar text |
| Nametags | entity nametag mixin exists in repo |
| Skins/capes | player info/skin mixins exist in repo |
| Tooltips/GUIs | global text sanitization catches many vanilla text paths |
| UUID-like text | `TextSanitizer` replaces dashed, compact, uppercase, and NBT int-array UUID forms |

### How it works

Nick Hider builds a replacement map from online players:

- collect local player and online `PlayerInfo` names/UUIDs
- decide whether a target is local or other player
- use config to decide whether to mask local name, other names, skin, and cape
- generate stable alias names/templates
- generate synthetic UUIDs for masked identities
- sort replacements by longest original string first
- replace names and UUIDs in text

The important difference from FakeName is that this is multi-player aware and UUID-aware. For a robust SkyBlock spoofer, this pattern matters because tab list/player names can include multiple players, party members, dungeon teammates, or profile viewer references.

## Levelhead

Sources:

- Repository: https://github.com/Sk1erLLC/Levelhead
- Chat display: https://raw.githubusercontent.com/Sk1erLLC/Levelhead/master/src/main/kotlin/club/sk1er/mods/levelhead/render/ChatRender.kt
- Tab display renderer: https://raw.githubusercontent.com/Sk1erLLC/Levelhead/master/src/main/kotlin/club/sk1er/mods/levelhead/render/TabRender.kt
- Tab mixin: https://raw.githubusercontent.com/Sk1erLLC/Levelhead/master/src/main/java/club/sk1er/mods/levelhead/mixin/MixinGuiPlayerTabOverlay.java
- Display objects: https://raw.githubusercontent.com/Sk1erLLC/Levelhead/master/src/main/kotlin/club/sk1er/mods/levelhead/display/LevelheadDisplay.kt

### What it changes

Levelhead is not a fake-rank mod. It is useful because it shows how to add client-side values in chat and tab:

- displays Hypixel Network Level or other configured stat above heads
- prepends a stat tag to chat messages
- draws a stat tag in the tab list near ping/scoreboard area

### Where it appears

| Location | Mechanism |
| --- | --- |
| Chat | listens to `ClientChatReceivedEvent`, identifies clickable/hoverable player component, prepends `[tag]` |
| Tab | hooks `GuiPlayerTabOverlay.drawPing`, draws the cached tag near the ping area |
| Tab layout | modifies string width calculation to reserve extra room |
| Above heads | separate above-head renderer |

### Why it matters

For any SkyBlock level/rank spoofer, tab rendering has two problems:

1. You must draw the replacement/additional value.
2. You must reserve width so the tab list does not overlap or clip.

Levelhead handles both: draw hook plus width hook. RankSpoof handles this through font width replacement. Either approach works.

## SkyHanni

Sources:

- Repository: https://github.com/hannibal002/SkyHanni
- Scoreboard data: https://raw.githubusercontent.com/hannibal002/SkyHanni/beta/src/main/java/at/hannibal2/skyhanni/data/ScoreboardData.kt
- GUI mixin: https://raw.githubusercontent.com/hannibal002/SkyHanni/beta/src/main/java/at/hannibal2/skyhanni/mixins/transformers/gui/MixinGui.java
- Skill API: https://raw.githubusercontent.com/hannibal002/SkyHanni/beta/src/main/java/at/hannibal2/skyhanni/api/SkillApi.kt
- Skill progress display: https://raw.githubusercontent.com/hannibal002/SkyHanni/beta/src/main/java/at/hannibal2/skyhanni/features/skillprogress/SkillProgress.kt
- Skill tooltip: https://raw.githubusercontent.com/hannibal002/SkyHanni/beta/src/main/java/at/hannibal2/skyhanni/features/skillprogress/SkillTooltip.kt
- Garden level display: https://raw.githubusercontent.com/hannibal002/SkyHanni/beta/src/main/java/at/hannibal2/skyhanni/features/garden/GardenLevelDisplay.kt
- Scoreboard patterns: https://raw.githubusercontent.com/hannibal002/SkyHanni/beta/src/main/java/at/hannibal2/skyhanni/features/gui/customscoreboard/ScoreboardPattern.kt

### What it changes

SkyHanni is not mainly a spoofer, but it has several client-side replacement patterns:

- hides vanilla scoreboard and renders a custom scoreboard
- modifies vanilla scoreboard line text before draw
- colors month names on the scoreboard
- fixes/replaces Piggy Bank purse line
- tracks skill XP from actionbar, tab, and skill menu
- hides skill XP from actionbar and replaces it with custom HUD display
- shows overflow skill levels and fake/extra level-up chat messages for overflow progress
- changes skill menu tooltip lines and can set a custom item name in the skill menu tooltip flow
- shows overflow Garden levels beyond the normal max, including tooltip and chat replacement

### Where it appears

| Location | Feature |
| --- | --- |
| Scoreboard | `ScoreboardData.tryToReplaceScoreboardLine` can replace Piggy/rank-like lines and color month names |
| Scoreboard HUD | custom scoreboard hides vanilla and redraws configured elements |
| Chat | skill overflow level-up message; garden overflow level-up message |
| Actionbar | can remove original skill XP gain segment from actionbar |
| HUD overlay | skill progress, all-skills display, ETA, garden level display |
| Skill menu item/tooltips | skill tooltip modifies progress, overflow XP, custom goal progress, and item name |
| Garden/SkyBlock menu item/tooltips | garden tooltip modifies max-level text into overflow progress and may rename item to higher Garden Level |
| Tab list | `SkillApi` reads skill level/progress lines from tab; not spoofing tab, but tab-derived data drives fake/overflow displays |

### Skill level handling

SkyHanni `SkillApi` gathers skill data from three places:

- actionbar skill XP gain segments like `+1.1 Mining (48.39%)`
- tab list lines like `Farming 35: 12.4%`, `Mining 60: MAX`, or `Combat 49: 7,678/4M`
- the `Your Skills` inventory, parsing item names and lore progress bars

It stores per-skill:

- base level
- current XP
- needed XP
- total XP
- overflow level
- overflow current/needed/total XP
- last gain
- custom goal level

`SkillProgress` then renders alternate skill displays. For spoofing research, the important part is that it can show a level different from the vanilla visible level if overflow/custom-goal logic is enabled.

### Skill menu tooltip replacement

`SkillTooltip` listens to tooltip events in the `Your Skills` inventory. It can:

- replace `Max Skill level reached!` with progress to the next overflow level
- replace the progress bar line
- add `OVERFLOW XP`
- add custom goal progress
- call `setCustomItemName` to make the skill item name display an overflow level

This is the direct pattern for "changing skill levels" in GUI/tooltips. You do not need to alter server data; you alter the rendered item name and tooltip lines.

### Garden overflow/fake level-up

`GardenLevelDisplay` tracks Garden XP after max level. When the computed overflow level increases beyond 15, it can emit a client-side chat message that looks like a level-up message. It also changes:

- Garden HUD overlay
- Desk tooltip progress
- SkyBlock menu Garden item tooltip
- Garden item name when level is above max

This is a direct example of synthetic progression display.

## Skyblocker

Sources:

- Repository: https://github.com/SkyblockerMod/Skyblocker
- Tab HUD: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/skyblock/tabhud/TabHud.java
- Player list manager: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/skyblock/tabhud/util/PlayerListManager.java
- Skills tab widget: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/skyblock/tabhud/widget/SkillsWidget.java
- Player tab overlay mixin: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/mixins/PlayerTabOverlayMixin.java
- Custom item names: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/skyblock/item/custom/CustomItemNames.java
- Item stack mixin: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/mixins/ItemStackMixin.java
- Custom armor dye colors: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/skyblock/item/custom/CustomArmorDyeColors.java
- Dyed item color mixin: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/mixins/DyedItemColorMixin.java
- Custom helmet textures: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/skyblock/item/custom/CustomHelmetTextures.java
- Skill slot text: https://raw.githubusercontent.com/SkyblockerMod/Skyblocker/main/src/main/java/de/hysky/skyblocker/skyblock/item/slottext/adders/SkillLevelAdder.java

### What it changes

Skyblocker is useful for modern Fabric patterns:

- hides vanilla tab list and renders a custom Tab HUD
- parses tab list lines into widgets
- shows skill progress as progress bars in a custom tab HUD
- hides ping icons in SkyBlock tab
- changes item hover names by UUID
- changes armor dye colors by UUID
- supports animated armor dye colors
- supports custom helmet/head textures
- overlays slot text for skill levels and other levels

### Where it appears

| Location | Feature |
| --- | --- |
| Tab list | replaces vanilla player list HUD with custom Tab HUD when enabled |
| Tab widgets | parses player list into sections: players, info, skills, effects, dungeon widgets |
| Skills tab | shows skill lines as progress components; MAX lines are shown differently |
| Inventory item names | `ItemStackMixin#getHoverName` returns configured custom item name by item UUID |
| Armor/item colors | `DyedItemColorMixin#getOrDefault` returns configured custom/animated dye color |
| Helmet textures | custom player head texture cache from item repository |
| Inventory slot overlays | `SkillLevelAdder` extracts skill level from skill menu item name and draws numeric text on slot |
| Tooltips | item stack mixin adds SkyBlock ID tooltip and modifies tooltip behavior |

### Tab HUD design

Skyblocker's `TabHud` replaces the vanilla player list HUD element with a no-op when the custom tab HUD is enabled. `PlayerListManager` then reads the real tab list from `ClientPacketListener.getOnlinePlayers()`, sorts it using vanilla ordering, and reads each `PlayerInfo.getTabListDisplayName()`.

It strips/organizes Hypixel tab text into widgets. For skills, `SkillsWidget` matches:

```text
SkillName Level: Progress
```

Examples:

- `Farming 35: 12.4`
- `Mining 60: MAX`

It then renders either a progress bar or a MAX label.

For a tab spoofer, this shows the clean approach:

- read original Hypixel tab components
- parse into structured widgets
- optionally replace values
- hide vanilla tab
- draw your own layout

### Skill level slot text

`SkillLevelAdder` is not spoofing the level itself, but it shows where skill menu values appear. It targets inventory title `Your Skills`, extracts level from the item hover name, converts Roman numerals or numeric text, and draws a number overlay in the slot corner. If you wanted to spoof skill levels in the skill menu, this is one of the required surfaces:

- item hover name
- tooltip lore/progress
- slot overlay number
- any custom tab/progress HUD using parsed skill data

## NotEnoughUpdates

Sources:

- Repository: https://github.com/NotEnoughUpdates/NotEnoughUpdates
- Item customization utility: https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates/master/src/main/java/io/github/moulberry/notenoughupdates/miscgui/itemcustomization/ItemCustomizationUtils.java
- Item customization manager: https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates/master/src/main/java/io/github/moulberry/notenoughupdates/miscgui/itemcustomization/ItemCustomizeManager.java
- Item customization GUI: https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates/master/src/main/java/io/github/moulberry/notenoughupdates/miscgui/itemcustomization/GuiItemCustomize.java
- ItemStack mixin: https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates/master/src/main/java/io/github/moulberry/notenoughupdates/mixins/MixinItemStack.java
- RenderItem mixin: https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates/master/src/main/java/io/github/moulberry/notenoughupdates/mixins/MixinRenderItem.java
- GUI container mixin: https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates/master/src/main/java/io/github/moulberry/notenoughupdates/mixins/MixinGuiContainer.java
- Armor layer mixin: https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates/master/src/main/java/io/github/moulberry/notenoughupdates/mixins/MixinLayerArmorBase.java

### What it changes

NEU's item customizer is a large example of client-side item spoofing:

- custom item name
- custom item model/type
- custom skull texture
- custom enchant glint on/off
- custom glint color
- custom leather armor color
- animated leather armor color
- tooltip dye color hex
- armor render item substitution
- inventory GUI item substitution
- item icon render substitution

### Where it appears

| Location | Feature |
| --- | --- |
| Inventory item name | `MixinItemStack#getDisplayName` returns custom name |
| Tooltip name line | same display name hook affects tooltip title |
| Tooltip lore | tooltip redirects alter leather color tooltip output |
| Inventory item icon | `MixinGuiContainer` passes `ItemCustomizeManager.useCustomItem(stack)` into render calls |
| Held/inventory render | `MixinRenderItem` controls item glint and color |
| Armor on player model | `MixinLayerArmorBase` substitutes armor stack and color/glint |
| GUI editor | `GuiItemCustomize` is the full client-side customization screen |
| Share/import | customizations can be serialized to clipboard with prefixes like `NEUCUSTOMIZE` |

### Implementation details

NEU stores customization by item UUID. On render, it copies the real item stack and mutates the copy:

- replace `Item`
- replace item damage/metadata
- replace `SkullOwner`
- override glint
- override color

This is the correct pattern: never mutate the server-authoritative item unless the mod intentionally writes to local config only. Use copies for rendering.

## Custom Scoreboard

Sources:

- Modrinth: https://modrinth.com/mod/skyblock-custom-scoreboard
- Repository: https://github.com/meowdding/CustomScoreboard
- Renderer: https://raw.githubusercontent.com/meowdding/CustomScoreboard/main/src/main/kotlin/me/owdding/customscoreboard/feature/customscoreboard/CustomScoreboardRenderer.kt
- Scoreboard line: https://raw.githubusercontent.com/meowdding/CustomScoreboard/main/src/main/kotlin/me/owdding/customscoreboard/feature/customscoreboard/ScoreboardLine.kt
- Element base: https://raw.githubusercontent.com/meowdding/CustomScoreboard/main/src/main/kotlin/me/owdding/customscoreboard/feature/customscoreboard/elements/Element.kt
- SkyHanni compatibility: https://raw.githubusercontent.com/meowdding/CustomScoreboard/main/src/main/java/me/owdding/customscoreboard/mixins/compat/SkyHanniCustomScoreboardMixin.java

### What it changes

Custom Scoreboard hides Hypixel's vanilla scoreboard and renders configured client-side lines. It does not need to spoof server data; it fully owns the client display.

It can show:

- purse/piggy
- bank/bits/currencies
- pet
- slayer XP/meter
- events
- tablist lines in scoreboard
- custom separators
- click/hover actions
- custom background/blur/texture/GIF

### Where it appears

| Location | Feature |
| --- | --- |
| Scoreboard HUD | replaces vanilla scoreboard with custom layout |
| Scoreboard Overhaul | can route content into Scoreboard Overhaul |
| Mouse/tooltips | scoreboard lines can be buttons with hover/click actions |
| Commands | line action can run commands, e.g. pet menu |
| Compatibility | can disable SkyHanni's custom scoreboard through mixin |

### Why it matters for spoofing

If the target is scoreboard-only spoofing, this is the cleanest architecture:

- hide vanilla scoreboard
- define each fake/real line as an element
- render the configured element list
- leave chat/tab untouched

For values that must match chat/tab too, this is not enough. You still need text replacement or tab hooks.

## Hypixel SkyBlock Level Prefix Colors

Source:

- Official Hypixel SkyBlock Wiki, SkyBlock Levels: https://wiki.hypixel.net/SkyBlock_Levels

Hypixel shows SkyBlock level prefix colors in **chat** and **tab**. The official wiki lists the color reward thresholds:

| SkyBlock level range | Prefix color | Minecraft code approximation |
| --- | --- | --- |
| 0-39 | Default/gray-style | `§7`/server default |
| 40-79 | White | `§f` |
| 80-119 | Yellow | `§e` |
| 120-159 | Green | `§a` |
| 160-199 | Dark Green | `§2` |
| 200-239 | Aqua | `§b` |
| 240-279 | Cyan/Dark Aqua | `§3` |
| 280-319 | Blue | `§9` |
| 320-359 | Pink/Light Purple | `§d` |
| 360-399 | Purple/Dark Purple | `§5` |
| 400-439 | Gold | `§6` |
| 440-479 | Red | `§c` |
| 480+ | Dark Red | `§4` |

The wiki explicitly lists Dark Red at Level 480 and examples up to `[480] - [500]`. Community screenshots may show higher current levels still using dark red unless Hypixel adds later colors.

### SkyBlock level spoof surfaces

If spoofing SkyBlock level prefix, update every place that can show:

- chat sender prefix: `[480] [MVP+] Name: message`
- tab player entry: `[480] [MVP+] Name`
- player list sections that include the player's own name
- nametag if Hypixel/mods render level in nametag
- profile viewer text if the mod displays SkyBlock level
- custom tab HUD player widget if it parses the old prefix
- any cached rank/name component in third-party mods

RankSpoof-like component replacement can detect the segment before the rank. A targeted regex for SkyBlock names should handle:

```text
[123] [MVP+] Name
[480] [MVP++] Name
[40] Name
[123] [VIP] Name
[123] [YOUTUBE] Name
[123] [ADMIN] Name
[123] Name ♠
```

Do not assume rank is always present.

## Hypixel Rank Colors

Sources:

- Hypixel Support rank article: https://support.hypixel.net/hc/en-us/articles/360019646559-How-to-Obtain-the-Available-Ranks-on-Hypixel
- Hypixel Wiki rank color/plus color reference: https://hypixel.fandom.com/wiki/Ranks

Common rank color behavior:

| Rank | Typical base color | Notes |
| --- | --- | --- |
| Default/no rank | gray name/chat | no bracket prefix |
| VIP | green | `[VIP]` |
| VIP+ | green rank, gold plus | `[VIP+]` |
| MVP | aqua | `[MVP]` |
| MVP+ | aqua rank, configurable plus | `[MVP+]` |
| MVP++ | gold rank/name, configurable pluses; can use aqua option | `[MVP++]` |
| YOUTUBE | red/white styling | special rank |
| ADMIN/GM | red/admin styling | special/staff rank |

MVP+/MVP++ plus colors are unlocked by Hypixel level/gifting. The Hypixel Wiki lists:

| Plus color | Unlock |
| --- | --- |
| Red | default |
| Gold | Hypixel Level 35 |
| Green | Hypixel Level 45 |
| Yellow | Hypixel Level 55 |
| Light Purple | Hypixel Level 65 |
| White | Hypixel Level 75 |
| Blue | Hypixel Level 85 |
| Dark Green | Hypixel Level 95 |
| Dark Red | Hypixel Level 150 |
| Dark Purple | Hypixel Level 200 |
| Black | Hypixel Level 250 |
| Dark Blue | 100 gifted ranks |

Rank formatting in chat/tab is often component-split. `[MVP++]` may not be one text sibling; it can be split into `[MVP`, `++`, `]` so the pluses can have separate colors. This is why RankSpoof uses sibling scanning instead of only one regex.

## Skill Levels: every visible surface

Changing skill levels client-side requires more than one hook because Hypixel shows skills in multiple places.

| Surface | Example value | Required spoof path |
| --- | --- | --- |
| Actionbar XP gain | `+1.1 Mining (48.39%)` | actionbar update event or global text replacement |
| Tab skill section | `Mining 60: MAX`, `Farming 35: 12.4%` | tab list component replacement or custom tab HUD parser |
| Skill menu item name | `Mining LX` / `Mining 60` | `ItemStack#getHoverName` or tooltip event `setCustomItemName` |
| Skill menu tooltip progress | `Progress to Level ...`, progress bar, XP numbers | tooltip event list mutation |
| Skill menu slot overlay | small number drawn on item | slot text renderer/adder |
| HUD skill overlay | mod-owned progress HUD | replace internal skill data model or render output |
| All-skills display | list of all skills and levels | replace internal model/render output |
| ETA display | target level, needed XP, XP/h | replace internal model/render output |
| Profile viewer | skill list and level bars | replace profile viewer data model or widget text |
| Chat level-up | `SKILL LEVEL UP ...` | client-side chat injection or chat message rewrite |

SkyHanni already demonstrates most of this:

- parses actionbar, tab, and inventory
- stores a skill data model
- renders HUD overlays
- mutates skill menu tooltip/name
- injects overflow level-up chat messages

Skyblocker demonstrates the tab/slot overlay side:

- parses tab skills into custom widgets
- overlays skill level text on skill menu slots

For YuriClient, the clean design is to store one `SpoofedSkillState` and let every surface read from it. Do not hardcode skill spoofing in five unrelated render hooks.

## Value categories to support

A complete SkyBlock spoofer would need a model like this:

| Category | Values | Surfaces |
| --- | --- | --- |
| Identity | username, display name, nickname | chat, tab, nametag, scoreboard, titles, mod GUIs |
| Rank | rank prefix, plus count, plus color, rank name color | chat, tab, scoreboard rank line, profile tooltip |
| SkyBlock level | numeric level, prefix color, emblem | chat, tab, profile viewers, custom tab HUD |
| Skills | level, overflow level, XP, progress, custom goal | tab, actionbar, skill menu, tooltips, HUD overlays, profile viewer |
| Currencies | purse, bank, bits, gems, copper, motes, soulflow | scoreboard, custom scoreboard, tab widgets, inventory/tooltips |
| Location/profile | island, area, profile name/type, lobby code | scoreboard, tab, custom HUD |
| Dungeon/party | class level, secrets, score, teammates, deaths | tab, dungeon widgets, scoreboard, party chat overlays |
| Items | name, rarity color, texture, skull, dye, trim, glint, lore | inventory GUI, tooltips, held item, armor render, item entity render |
| Garden/mining/slayer | garden level, HOTM/HOTF, slayer level/meter | scoreboard, tab, menus, tooltips, HUD overlays |

## Recommended architecture for YuriClient

### 1. Central spoof state

Create a single state store:

```text
SpoofState
  enabled
  identity
  rank
  skyBlockLevel
  skills
  currencies
  itemsByUuid
```

Every render hook reads from this. Do not let chat, tab, scoreboard, and tooltip hooks each maintain separate values.

### 2. Text replacement engine

Implement a component-aware replacement engine:

- input: `Component`
- output: `Component`
- preserve hover/click/insertion/font
- handle component siblings
- handle legacy `§` codes
- handle split rank components
- handle no-rank players
- handle team prefixes/dungeon class prefixes

String fallback is still useful for old/plain render paths, but component replacement should be primary on modern Minecraft.

### 3. Global text hook

Use a conservative global text hook for identity/rank/level:

- `Font` preparation/width for modern text render
- `StringDecomposer` for formatted string sanitization
- text wrapping hooks for width-safe wrapping

This catches:

- chat
- tab
- scoreboard
- titles
- actionbar
- nametags
- vanilla GUI labels
- many mod GUI labels

### 4. Targeted tab hook

Tab is important enough to handle directly:

- hook `PlayerTabOverlay#getNameForDisplay` to replace the local player's display component
- if rendering extra tags, reserve width or custom-render the whole tab HUD
- if hiding vanilla tab, rebuild from `ClientPacketListener.getOnlinePlayers()`

### 5. Targeted scoreboard hook

Support both modes:

- **rewrite vanilla lines**: targeted hook before `drawString`
- **custom scoreboard**: hide vanilla sidebar and render configured lines

If only changing `Rank: ` or purse-like values, targeted line rewrite is cheaper. If changing many values and layout, full custom scoreboard is cleaner.

### 6. Tooltip and item hooks

For GUI/tooltips:

- `ItemStack#getHoverName` for item names and skill menu item titles
- tooltip event/list mutation for lore/progress
- tooltip constructor hook for late-built vanilla tooltips
- slot render overlay for small numbers
- item renderer/armor renderer for model/color/glint/texture spoofing

### 7. Surface-specific invalidation

Cache aggressively, but invalidate on:

- config change
- world/server change
- profile change
- scoreboard packet/update
- tab list update
- inventory open
- item UUID change
- resource reload

## Edge cases

- Hypixel rank components may be split across siblings.
- Some tab names include dungeon/team prefixes that look like rank prefixes.
- Unranked players have no bracket rank; do not assume `[VIP]`.
- Nicked players may have rank-like data that differs from real profile data.
- MVP++ can use different name color modes.
- SkyBlock level prefix may have an emblem after/beside the number.
- Some mod GUIs cache already-rendered components and need explicit invalidation.
- If only render text changes and width does not, text clips or overlaps.
- If only width changes and render text does not, layout gaps appear.
- Tooltip text can be created before render, so font hooks alone may not catch semantic tooltip lines.
- Item names and tooltip lore may use separate component paths.
- Server-side item NBT must not be mutated; use copied stacks for visual substitutions.
- Multiple mods can hook the same methods. Priority and compatibility checks matter.

## Practical first milestone

Build this order:

1. Rank/SkyBlock-level component replacement for local player in chat/tab/scoreboard.
2. Add width-safe text hooks.
3. Add direct tab display hook for `PlayerTabOverlay`.
4. Add scoreboard line rewrite for `Rank: ` and `[level] [rank] name` patterns.
5. Add skill menu tooltip/name spoofing for one skill.
6. Add actionbar/tab skill spoof replacement.
7. Add custom scoreboard/custom tab HUD only after the targeted hooks are stable.

This gives useful coverage early without committing to a full custom HUD system immediately.
