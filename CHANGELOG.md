# PS4Control 0.3.9

**Clicks are delivered through vanilla's own click pipeline again.**

Earlier releases stopped using MouseHandler.onButton (the path that made menu
clicks work in 0.3.3) in favor of calling screen.mouseClicked directly —
which silently misses vanilla plumbing around clicks. Cross and touchpad-tap
clicks now go through onButton again, at the visible warped cursor position.
