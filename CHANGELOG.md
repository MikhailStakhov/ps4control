# PS4Control 0.1.0

Initial release.

- PS4 (DualShock 4) / PS5 (DualSense) controller support for Minecraft 26.3 (Fabric)
- Uses SDL's GameController API via LWJGL — the same SDL the game itself loads,
  zero bundled natives (fixes the "Xulu platform x64" crash class from mods that
  ship their own SDL, like Controlify)
- Console-style default layout, fully remappable via config/ps4control.json
- Virtual cursor for menus (left stick + Cross/Circle)
- Split-screen friendly: per-instance controller locking
  (`/ps4control list`, `/ps4control bind <guid>`)
- Client-side only; compatible with Sodium, Lithium, mc-splitscreen
