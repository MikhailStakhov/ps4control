# PS4Control 0.3.1

Gameplay fixes after controllers came online:

- **Camera speed fixed** — sensitivity was double-scaled (mod × vanilla), making
  the right stick crawl. Now full-tilt ≈ 160°/s at default sensitivity; adjust
  "Sensitivity" in the settings GUI to taste.
- **R2 (mine/attack) and L2 (use/place) now work** — triggers are SDL *axes*,
  not buttons; they were silently never registering.
- **Circle closes menus** console-style: right-click in the inventory as before,
  but if nothing under the cursor consumes it, Circle backs out of the screen.
  D-pad Down still closes instantly.
- Fixed an internal edge-detection slot collision between Cross and the triggers.
