# Repository Guidelines

## Project Structure & Module Organization

This is a Fabric client-side Minecraft mod for `yuri-spoofer`, targeting Minecraft 1.21.11 and Java 21. Gradle uses split environment source sets:

- `src/main/kotlin` and `src/main/java`: common mod entry points and shared code.
- `src/client/kotlin`: client implementation, GUI, config, and spoofing logic.
- `src/client/java`: Sponge Mixin hooks into Minecraft client rendering classes.
- `src/main/resources`: common mod metadata, mixin config, and assets such as `assets/yuri-spoofer/icon.png`.
- `src/client/resources`: client mixin config and language files.
- `run/`: local Minecraft dev runtime output; do not treat logs or screenshots as source.
- `build/`: Gradle output; do not edit or commit generated artifacts.

Core package paths should remain under `vlensys.yurispoofer`.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root. Ensure `JAVA_HOME` points to a JDK 21 installation.

- `.\gradlew build`: compiles Kotlin/Java, processes resources, and creates jars in `build/libs/`.
- `.\gradlew runClient`: launches the Minecraft client in a Fabric development environment.
- `.\gradlew runClientGametest`: runs the configured Fabric client gametest run.
- `.\gradlew genSources`: generates Minecraft sources for IDE navigation.

On Unix-like shells, use `./gradlew` instead of `.\gradlew`.

## Coding Style & Naming Conventions

Kotlin is the primary implementation language; Java is used for mixins. Use 4-space indentation. Keep Kotlin `object` singletons for stateless services such as spoofers and config helpers. Name mixin methods with the `yuri$...` prefix to avoid collisions, as in `yuri$spoofTabName`.

Keep user-facing strings and mod metadata in resource files when appropriate. Prefer focused functions over broad rewrites, especially in render hooks and text replacement code.

## Testing Guidelines

Client behavior is covered with Fabric `ClientGameTest` in `src/client/kotlin/.../test`, currently `SpooferGuiGameTest.kt`. Add tests there for spoofing rules, GUI smoke coverage, and tooltip rewrites. Test methods should exercise realistic Hypixel-style formatted `Component` input and assert the resulting plain string content.

Run `.\gradlew build` before submitting changes. For client-only behavior, also run `.\gradlew runClientGametest` when practical.

## Commit & Pull Request Guidelines

This checkout does not include `.git` history, so no project-specific commit convention is available. Use short, imperative commit subjects, for example `Fix lobby id spoofing in join messages`.

Pull requests should describe the user-visible change, list the commands run, and call out Minecraft/Fabric version changes. Include screenshots for GUI changes, preferably from `run/screenshots/`, and mention any untested client paths.

## Security & Configuration Tips

Do not commit personal `run/` config, logs, or generated jars. Keep dependency and Minecraft version changes centralized in `gradle.properties`, and verify compatibility before updating Fabric, Loom, Kotlin, or owo-lib versions.
