# PS4Control 0.3.0

**The big one: controllers were never detected at all.**

- Minecraft only initializes SDL's VIDEO subsystem — the GAMEPAD subsystem is
  never started, so SDL reported zero gamepads on every machine. The mod now
  initializes SDL_INIT_GAMEPAD + SDL_INIT_JOYSTICK itself at startup. This is
  the root cause of every "controller does nothing" report so far.
- Debug HUD now shows how many raw joysticks SDL sees even when no gamepad is
  bound (distinguishes "no devices" from "no mapping").
