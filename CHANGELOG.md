# PS4Control 0.3.6

**Clicking now works — because you can finally SEE the cursor.**

The controller's virtual cursor moved internally, but the visible pointer on
screen stayed wherever your physical mouse last was. Cross clicks landed at
the invisible virtual position, so nothing seemed to happen. The real OS
cursor is now warped to follow the virtual one every frame:

- What you see IS what clicks — stick/touchpad moves the visible pointer
- Cursor centers on screen open (visibly now)
- Cross = click at the visible cursor; Circle = back; Options = pause menu
