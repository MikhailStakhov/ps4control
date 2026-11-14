# PS4Control 0.2.5

- **Removed the per-frame connection check entirely.** Some Windows driver
  paths report the gamepad connection state as "unknown" while connected,
  which re-triggered the every-frame rebind storm (GUI flicker, dead input).
  The mod now only checks the device list every 2 seconds.
- New `/ps4control debug` chat command toggles the Debug HUD (top-left in-game
  readout of every gamepad + raw button and axis) — no GUI needed.
