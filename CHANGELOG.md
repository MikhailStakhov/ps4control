# PS4Control 0.2.1

Critical fixes for controller input not working at all:

- **Wireless controllers (Bluetooth) now work.** The connection-state check
  required the "wired" value, so a Bluetooth DualShock/DualSense caused the
  gamepad to be closed and reopened every frame — no input ever registered.
- **Menu navigation (virtual cursor + clicks) now works.** Synthesized mouse
  events were sent with a null window handle, which Minecraft ignores.
- Input loop is now exception-guarded (errors are rate-limited to the log
  instead of breaking the game).

For 0.2.0 GUI features (controller picker, sliders, live readout), see the 0.2.0 notes.
