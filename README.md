# PS4Control

[![Build](https://github.com/MikhailStakhov/ps4control/actions/workflows/build.yml/badge.svg)](https://github.com/MikhailStakhov/ps4control/actions/workflows/build.yml)

A lightweight Fabric mod that adds PS4 (DualShock 4) / PS5 (DualSense) controller
support to Minecraft Java Edition using **SDL's GameController API via LWJGL** —
the *exact same* SDL library Minecraft 26.x already loads for its own windowing
and input. **No bundled native libraries of our own**, so the "Xulu platform x64"
crash you get with Controlify (which ships its own conflicting SDL natives)
cannot happen here.

> Why Controlify broke: Minecraft 26.x switched from GLFW to SDL. Controlify
> bundles its own SDL natives, and the two collide ("Xulu platform x64").
> PS4Control has no natives at all — it just calls the SDL the game already uses.

- Client-side only. Works alongside **Sodium**, **Lithium**, and
  **[mc-splitscreen](https://modrinth.com/mod/mc-splitscreen)**.
- Target: Minecraft **26.3**, Fabric Loader 0.20.1+, Fabric API.

## Default controls (PlayStation console-style)

| Input | Action |
|---|---|
| Left stick | Move |
| Right stick | Look (camera) |
| Cross | Jump / confirm in menus |
| Circle | Sneak / right-click in menus |
| Square | Inventory |
| Triangle | Drop item |
| R2 | Attack / mine (hold) |
| L2 | Use / place (hold) |
| R1 / L1 | Hotbar next / previous |
| L3 (hold) | Sprint |
| R3 | Toggle perspective (F5) |
| D-pad Up | Chat |
| D-pad Down | Pause / close menu |
| Left stick in menus | Move virtual cursor |

## Split-screen setup (mc-splitscreen)

mc-splitscreen runs **one Minecraft instance per player** (e.g. via PrismLauncher).
Give each instance a copy of this mod, then lock each instance to its own controller:

1. Launch instance for player 1, plug in controller 1, run `/ps4control list` —
   note the controller's GUID.
2. `/ps4control bind <guid>` — saves that GUID to that instance's
   `config/ps4control.json`.
3. Repeat for player 2's instance with the second controller.

Instances will then ignore each other's controllers. (Leave `lockToJoystickGuid`
empty for single-player use — it just grabs the first gamepad.)

## Settings GUI

Run `/ps4control` (or `/ps4control menu`) in-game, or bind **"Open PS4Control
menu"** in Options → Controls → Key Mappings → PS4Control. The GUI has:

- **Controller picker** — cycles Auto / each connected pad. With two controllers
  plugged in, pick the right one here (press buttons and watch the live readout
  to tell the pads apart)
- Look sensitivity, cursor speed and deadzone sliders
- Enable / invert-Y toggles
- Live input panel (sticks, buttons, triggers of the bound pad)

## Config (`config/ps4control.json`)

- `enabled` — master switch
- `lockToJoystickGuid` — split-screen controller locking (see above)
- `lookSensitivity` (default 18), `cursorSpeed`, `deadzone` (0.15), `invertY`
- `bindings` — remap any action to any button name:
  `cross, circle, square, triangle, l1, r1, l3, r3, back, start, guide, dpad_up/down/left/right`

## Commands (client-side)

- `/ps4control status` — controller + config summary
- `/ps4control list` — all connected gamepads with GUIDs
- `/ps4control bind <guid>` / `/ps4control auto` — lock / unlock controller
- `/ps4control reload` — reload config

## Notes

- Uses SDL state polling, not events (Minecraft flushes gamepad events from its
  SDL queue), so it can't interfere with the game's own input handling.
- Steam Input can double-map gamepads — if buttons act twice, disable Steam Input
  for Minecraft.

## Releasing a new version

Every push to `main` is built by CI ([Actions](https://github.com/MikhailStakhov/ps4control/actions)).
To publish a release to Modrinth + GitHub Releases in one step:

1. Bump `mod_version` in `gradle.properties`, update `CHANGELOG.md`
2. Commit, then tag and push:
   ```
   git tag v0.1.1
   git push origin main --tags
   ```
3. CI builds with JDK 25, uploads to Modrinth (needs the `MODRINTH_TOKEN` secret
   set in repo Settings → Secrets → Actions, and the Modrinth project ID in
   `modrinthId` in `gradle.properties`), and creates a GitHub Release with the jar.

For updating to a new Minecraft version, see [UPDATING.md](UPDATING.md).

- Controllers must be connected via Bluetooth/USB **before** GLFW init for the
  gamepad database to map them; reconnects are handled automatically.
- Steam Input can double-map gamepads — if buttons act twice, disable Steam Input
  for Minecraft or use DS4Windows *off* (raw HID is what GLFW wants).
