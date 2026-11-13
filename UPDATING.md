# Updating PS4Control for a new Minecraft version

No mod can literally auto-update across Minecraft versions — but this project is
set up so an update is a 3-line change plus (usually) zero-to-few code fixes.

## Steps

1. Edit `gradle.properties`:
   ```properties
   minecraft_version=<new version>
   loader_version=<latest from https://fabricmc.net/develop>
   fabric_version=<latest for that MC version, from https://modrinth.com/mod/fabric-api>
   lwjgl_version=<check the MC version manifest's libraries list>
   ```
2. If Loom complains (new MC majors sometimes need a newer Loom), bump the
   version in the `plugins { id 'net.fabricmc.fabric-loom' version '...' }` line
   of `build.gradle` (list: https://maven.fabricmc.net/net/fabricmc/fabric-loom).
   **Note:** MC 26.1+ uses the `net.fabricmc.fabric-loom` plugin id (not the old
   `fabric-loom`) and needs no mappings dependency — the game ships unobfuscated.
3. Build (requires JDK 25+ for MC 26.x):
   ```
   gradlew build
   ```
4. If compilation fails, Mojang renamed something. The usual suspects:
   - `SDLEventHandler#pollEvents` (per-frame hook in `mixin/SDLEventHandlerMixin.java`)
   - `KeyMapping` field names in `InputHandler.handleGameplay` (`keyUp`, `keyAttack`, ...)
     and the static `KeyMapping.set/click(InputConstants.Key, ...)` path
   - `MouseHandler#onButton/onMove` (virtual cursor)
   - `Minecraft#gui.screen()`, `Entity#turn`, `Inventory#getSelectedSlot`
   Inspect the real jar under `.gradle/loom-cache/minecraftMaven/.../minecraft-merged-*.jar`
   with `javap` to find the new names, then update the Java files.
5. Also update `"minecraft": "26.x"` in `src/main/resources/fabric.mod.json`
   if the major version changes.
6. The jar appears in `build/libs/ps4control-<version>.jar`.

## Version pins used for the initial build

- Minecraft 26.3, Loader 0.20.1, Fabric API 0.166.2+26.3, LWJGL 3.4.3,
  Loom 1.19.4, Gradle 9.9.1, **JDK 25** (`C:\Users\The_R\.jdks\jdk-25`)
- 26.x uses **SDL via LWJGL** — GLFW is gone. The mod uses SDL's GameController
  API, which ships with the game, so it adds no native libraries.
