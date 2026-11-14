package com.ps4control;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLJoystick;

/**
 * On-screen readout of EVERY raw button the bound controller sends — including
 * buttons SDL's mapping doesn't turn into gamepad buttons (touchpad click,
 * PS/Guide on some driver paths). Toggled by "debugHud": true in config.
 * Purpose: identify which physical buttons actually reach the game so we can
 * bind them.
 */
public class DebugHud implements HudElement {

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!Ps4Config.get().debugHud) return;
        Minecraft mc = Minecraft.getInstance();
        var font = mc.font;
        if (font == null) return;

        long pad = ControllerManager.poll();
        int x = 4;
        int y = 4;
        int color = 0xFFC8FFC8;

        if (pad == 0) {
            graphics.text(font, "[PS4Control] no controller bound", x, y, 0xFFFF6060);
            return;
        }

        StringBuilder sb = new StringBuilder("GP down:");
        String[] names = {"S", "E", "W", "N", "back", "guide", "start", "L3", "R3",
            "L1", "R1", "dup", "ddown", "dleft", "dright", "misc1", "rp1", "lp1", "rp2", "lp2", "touch"};
        int[] codes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        for (int i = 0; i < codes.length; i++) {
            if (SDLGamepad.SDL_GetGamepadButton(pad, codes[i])) {
                sb.append(' ').append(names[i]);
            }
        }
        graphics.text(font, "[PS4Control] " + ControllerManager.boundDescription(), x, y, color);
        y += 10;
        graphics.text(font, sb.toString(), x, y, color);
        y += 10;

        // raw joystick layer — catches buttons with no gamepad mapping (touchpad, PS, paddles)
        long joy = SDLGamepad.SDL_GetGamepadJoystick(pad);
        if (joy != 0) {
            int n = SDLJoystick.SDL_GetNumJoystickButtons(joy);
            StringBuilder raw = new StringBuilder("RAW down (" + n + "):");
            for (int b = 0; b < n; b++) {
                if (SDLJoystick.SDL_GetJoystickButton(joy, b)) {
                    raw.append(' ').append(b);
                }
            }
            graphics.text(font, raw.toString(), x, y, 0xFFFFFF99);
            y += 10;
        }

        float lx = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX) / 32767f;
        float ly = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY) / 32767f;
        float rx = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX) / 32767f;
        float ry = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY) / 32767f;
        float l2 = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER) / 32767f;
        float r2 = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER) / 32767f;
        graphics.text(font, String.format("axes  LX %+.2f LY %+.2f RX %+.2f RY %+.2f L2 %+.2f R2 %+.2f",
            lx, ly, rx, ry, l2, r2), x, y, color);
    }
}
