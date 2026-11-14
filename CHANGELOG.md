# PS4Control 0.3.4

Menu controls simplified to pure Bedrock mapping:

- **Circle = Back, unconditionally.** It is no longer a right-click — it sends
  Escape through vanilla's close handling, which finally makes leaving the
  inventory work (container screens consume clicks on empty space, so the old
  "unconsumed click = back" trick could never fire there).
- **Options (Start) shows the real pause menu** (setScreen(PauseScreen)),
  not just the frozen world — works on multiplayer servers too.
- **Cursor is now a real mouse-style pointer**: left stick drives a smoothed,
  accelerating velocity (no more per-frame jerk); top speed raised to 900 px/s
  (Cursor Speed slider, up to 2000).
- Touchpad swipe still available alongside.
