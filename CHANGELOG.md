# PS4Control 0.3.8

- **Touchpad tap = click.** Swipe to aim the cursor, tap the pad to click at
  the pointer — like a laptop trackpad. Two-finger tap = right click.
- **Cross = click at the cursor first** (the visible pointer), Enter-on-focused
  only as fallback when the click hits empty space.
- Settings menu moved from touchpad-click to the **Share button** ("back"),
  since touchpad taps are now clicks. Existing configs: set `"menu": "back"`
  in config/ps4control.json, or delete the file to regenerate.
