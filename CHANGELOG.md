# PS4Control 0.3.7

**Fixes "the white selected thing doesn't react to any button":**

- Synthetic key events filled only vanilla's scancode slot. Focus navigation
  reads the scancode (so the highlight moved), but button activation checks the
  keycode slot — which was zero, so Enter-on-focused-widget silently failed.
  Both slots are now filled: Cross activates the focused server/world/button
  (Bedrock-style), falling back to a cursor click when nothing is focused.
- Cross priority changed: focused control first, cursor click second — matches
  how console UIs confirm selections.
