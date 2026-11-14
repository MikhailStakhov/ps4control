# PS4Control 0.2.4

Rewritten menu navigation — this should make GUIs usable in BOTH split-screen
instances:

- Virtual cursor movement and clicks are now delivered **directly** to the open
  screen with correct scaled coordinates, instead of going through Minecraft's
  accumulated-mouse-movement pipeline, which vanilla gates behind
  "is this window focused?" — the unfocused instance in a two-instance
  split-screen setup previously received no GUI input at all.
- Cursor is clamped to the window; Cross = left click, Circle = right click,
  D-pad Down closes the screen.
