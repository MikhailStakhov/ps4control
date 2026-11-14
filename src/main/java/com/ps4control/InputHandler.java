package com.ps4control;

import com.ps4control.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import org.lwjgl.sdl.SDLGamepad;

import java.util.HashSet;
import java.util.Set;

/**
 * Core input loop. Called once per frame from SDLEventHandlerMixin (before
 * vanilla polls the SDL event queue).
 * In-world: drives vanilla KeyMappings (movement/actions) and applies camera turns.
 * In menus: moves a virtual cursor and synthesizes mouse clicks.
 */
public final class InputHandler {

    private static final int BTN_COUNT = 26; // SDL3 has 26 gamepad buttons
    private static final boolean[] prevButtons = new boolean[BTN_COUNT];
    private static final Set<KeyMapping> heldByUs = new HashSet<>();

    private InputHandler() {}

    /** Called every frame. */
    public static void onFrame() {
        try {
            doFrame();
        } catch (Throwable t) {
            // never let controller input take the game down; log at most every 5 s
            long now = System.currentTimeMillis();
            if (now - lastErrorLog > 5000) {
                lastErrorLog = now;
                Ps4ControlMod.LOG("Input error: " + t);
            }
        }
    }

    private static long lastErrorLog;

    private static void doFrame() {
        Minecraft mc = Minecraft.getInstance();
        // settings-menu key works anywhere (menu or gameplay)
        if (Ps4ControlMod.MENU_KEY.consumeClick()) {
            Ps4ControlMod.openConfigScreen();
            return;
        }
        // gamepad button shortcut (default: touchpad) — works anywhere too
        long pad = ControllerManager.poll();
        if (pad != 0 && edge(pad, "menu")) {
            Ps4ControlMod.openConfigScreen();
            return;
        }
        Ps4Config cfg = Ps4Config.get();
        if (!cfg.enabled || mc.player == null && mc.gui.screen() == null) {
            return;
        }
        if (pad == 0) {
            releaseAll(mc);
            return;
        }

        if (mc.gui.screen() != null) {
            handleScreen(mc, cfg, pad);
        } else {
            handleGameplay(mc, cfg, pad);
        }
    }

    // ------------------------------------------------------------------ gameplay

    private static void handleGameplay(Minecraft mc, Ps4Config cfg, long pad) {
        // --- left stick movement -> vanilla key mappings
        double lx = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, cfg.deadzone);
        double ly = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY, cfg.deadzone);
        key(mc.options.keyUp, ly < -0.25);
        key(mc.options.keyDown, ly > 0.25);
        key(mc.options.keyLeft, lx < -0.25);
        key(mc.options.keyRight, lx > 0.25);

        // --- buttons
        button(pad, "jump", mc.options.keyJump, true);
        button(pad, "sneak", mc.options.keyShift, true);
        button(pad, "sprint", mc.options.keySprint, true);
        button(pad, "inventory", mc.options.keyInventory, false);
        button(pad, "drop", mc.options.keyDrop, false);
        button(pad, "attack", mc.options.keyAttack, true);
        button(pad, "use", mc.options.keyUse, true);
        button(pad, "chat", mc.options.keyChat, false);
        button(pad, "perspective", mc.options.keyTogglePerspective, false);

        if (edge(pad, "hotbarNext")) changeSlot(mc, 1);
        if (edge(pad, "hotbarPrev")) changeSlot(mc, -1);
        if (edge(pad, "pause")) mc.pauseGame(true);

        // --- camera: right stick. Entity.turn applies its own 0.15 scaling,
        // so pass the raw sensitivity — do NOT pre-scale.
        double rx = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX, cfg.deadzone);
        double ry = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY, cfg.deadzone);
        if (rx != 0 || ry != 0) {
            double yaw = rx * cfg.lookSensitivity;
            double pitch = (cfg.invertY ? -ry : ry) * cfg.lookSensitivity;
            mc.player.turn(yaw, pitch);
        }
    }

    private static void changeSlot(Minecraft mc, int dir) {
        if (mc.player == null || mc.player.getInventory() == null) return;
        int next = Math.floorMod(mc.player.getInventory().getSelectedSlot() + dir, 9);
        mc.player.getInventory().setSelectedSlot(next);
        if (mc.player.connection != null) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(next));
        }
    }

    // ------------------------------------------------------------------ screens

    private static void handleScreen(Minecraft mc, Ps4Config cfg, long pad) {
        var screen = mc.gui.screen();
        if (screen == null) return;
        long window = mc.getWindow().handle();

        // --- Bedrock-style focus navigation: D-pad and left-stick flicks move
        // focus (vanilla arrow-key traversal), Cross activates, Circle backs out.
        if (edge(pad, "dpad_up") || flick(pad, 0)) {
            screen.keyPressed(new net.minecraft.client.input.KeyEvent(
                com.mojang.blaze3d.platform.InputConstants.KEY_UP, 0, 0));
        }
        if (edge(pad, "dpad_down") || flick(pad, 1)) {
            screen.keyPressed(new net.minecraft.client.input.KeyEvent(
                com.mojang.blaze3d.platform.InputConstants.KEY_DOWN, 0, 0));
        }
        if (edge(pad, "dpad_left") || flick(pad, 2)) {
            screen.keyPressed(new net.minecraft.client.input.KeyEvent(
                com.mojang.blaze3d.platform.InputConstants.KEY_LEFT, 0, 0));
        }
        if (edge(pad, "dpad_right") || flick(pad, 3)) {
            screen.keyPressed(new net.minecraft.client.input.KeyEvent(
                com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT, 0, 0));
        }

        // --- virtual cursor (fallback for clicking specific things like item slots)
        double lx = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, cfg.deadzone);
        double ly = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY, cfg.deadzone);
        if (lx != 0 || ly != 0) {
            double dx = lx * cfg.cursorSpeed / 60.0;
            double dy = ly * cfg.cursorSpeed / 60.0;
            double nx = clamp(mc.mouseHandler.xpos() + dx, 0, mc.getWindow().getWidth());
            double ny = clamp(mc.mouseHandler.ypos() + dy, 0, mc.getWindow().getHeight());
            mc.mouseHandler.onMove(window, nx, ny, 0, 0);
            double sx = net.minecraft.client.MouseHandler.getScaledXPos(mc.getWindow(), nx);
            double sy = net.minecraft.client.MouseHandler.getScaledYPos(mc.getWindow(), ny);
            screen.mouseMoved(sx, sy);
        }

        // Cross = left click; if nothing was under the cursor, activate the
        // focused control (Enter-equivalent) — Bedrock-style confirm
        crossClickOrActivate(mc, screen, pad);
        // Circle = right click, or back if nothing consumed it
        circleClickOrBack(mc, screen, pad);
    }

    // left-stick flick detection for discrete focus movement (0=up,1=down,2=left,3=right)
    private static final boolean[] flickPrev = new boolean[4];

    private static boolean flick(long pad, int dir) {
        double lx = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, 0.15);
        double ly = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY, 0.15);
        boolean pressed = switch (dir) {
            case 0 -> ly < -0.75;
            case 1 -> ly > 0.75;
            case 2 -> lx < -0.75;
            default -> lx > 0.75;
        };
        boolean rising = pressed && !flickPrev[dir];
        flickPrev[dir] = pressed;
        return rising;
    }

    /**
     * Circle: right-click on press; if NO gui element consumed the click
     * (mouseClicked returned false), treat Circle as "back" and close the
     * screen on release — console-style menu behavior.
     */
    private static void circleClickOrBack(Minecraft mc, net.minecraft.client.gui.screens.Screen screen,
                                          long pad) {
        int idx = index("sneak");
        boolean now = rawDown(pad, "sneak");
        boolean was = prevButtons[idx];
        prevButtons[idx] = now;
        if (now == was) return;

        double sx = net.minecraft.client.MouseHandler.getScaledXPos(mc.getWindow(), mc.mouseHandler.xpos());
        double sy = net.minecraft.client.MouseHandler.getScaledYPos(mc.getWindow(), mc.mouseHandler.ypos());
        var info = new MouseButtonInfo(1, 0);
        var event = new net.minecraft.client.input.MouseButtonEvent(sx, sy, info);
        if (now) {
            circleConsumed = screen.mouseClicked(event, false);
        } else {
            screen.mouseReleased(event);
            if (!circleConsumed) {
                screen.onClose();
            }
            circleConsumed = true;
        }
    }

    private static boolean circleConsumed = true;

    /**
     * Cross: left click at the cursor; if no element consumed the click,
     * send Enter to the screen so the FOCUSED control activates — console-style.
     */
    private static void crossClickOrActivate(Minecraft mc, net.minecraft.client.gui.screens.Screen screen,
                                             long pad) {
        int idx = index("jump");
        boolean now = rawDown(pad, "jump");
        boolean was = prevButtons[idx];
        prevButtons[idx] = now;
        if (now == was) return;

        double sx = net.minecraft.client.MouseHandler.getScaledXPos(mc.getWindow(), mc.mouseHandler.xpos());
        double sy = net.minecraft.client.MouseHandler.getScaledYPos(mc.getWindow(), mc.mouseHandler.ypos());
        var info = new MouseButtonInfo(0, 0);
        var event = new net.minecraft.client.input.MouseButtonEvent(sx, sy, info);
        if (now) {
            boolean consumed = screen.mouseClicked(event, false);
            if (!consumed) {
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(
                    com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
            }
        } else {
            screen.mouseReleased(event);
        }
    }

    /** Emulates a mouse press/release pair, delivered directly to the screen. */
    private static void clickScreen(Minecraft mc, net.minecraft.client.gui.screens.Screen screen,
                                    long pad, String action, int button) {
        int idx = index(action);
        boolean now = rawDown(pad, action);
        boolean was = prevButtons[idx];
        prevButtons[idx] = now;
        if (now == was) return;

        double sx = net.minecraft.client.MouseHandler.getScaledXPos(mc.getWindow(), mc.mouseHandler.xpos());
        double sy = net.minecraft.client.MouseHandler.getScaledYPos(mc.getWindow(), mc.mouseHandler.ypos());
        var info = new MouseButtonInfo(button, 0);
        var event = new net.minecraft.client.input.MouseButtonEvent(sx, sy, info);
        if (now) {
            screen.mouseClicked(event, false);
        } else {
            screen.mouseReleased(event);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** SDL axis values are S16; convert to [-1,1] with deadzone + rescale. */
    private static double axis(long pad, int axis, double deadzone) {
        float raw = SDLGamepad.SDL_GetGamepadAxis(pad, axis) / 32767.0f;
        if (Math.abs(raw) < deadzone) return 0;
        double sign = Math.signum(raw);
        return sign * (Math.abs(raw) - deadzone) / (1.0 - deadzone);
    }

    private static boolean rawDown(long pad, String action) {
        String binding = Ps4Config.get().bindings.getOrDefault(action, "");
        // triggers are AXES in SDL, not buttons
        if (binding.equals("l2")) {
            return SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER) > 5000;
        }
        if (binding.equals("r2")) {
            return SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER) > 5000;
        }
        int code = Bindings.code(binding);
        return code >= 0 && SDLGamepad.SDL_GetGamepadButton(pad, code);
    }

    /** Rising edge; updates prev state. */
    private static boolean edge(long pad, String action) {
        int idx = index(action);
        boolean now = rawDown(pad, action);
        boolean was = prevButtons[idx];
        prevButtons[idx] = now;
        return now && !was;
    }

    private static int index(String action) {
        String binding = Ps4Config.get().bindings.getOrDefault(action, "");
        if (binding.equals("l2")) return BTN_COUNT - 2; // triggers aren't buttons; own slots
        if (binding.equals("r2")) return BTN_COUNT - 1;
        int code = Bindings.code(binding);
        return Math.max(0, Math.min(BTN_COUNT - 3, code));
    }

    /** Sets a key down while held; releases when we were the one holding it. */
    private static void button(long pad, String action, KeyMapping mapping, boolean holdCapable) {
        boolean pressed = rawDown(pad, action);
        KeyMappingAccessor accessor = (KeyMappingAccessor) mapping;
        if (pressed) {
            if (!wasHeld(action)) {
                KeyMapping.click(accessor.ps4control$key());
            }
            KeyMapping.set(accessor.ps4control$key(), true);
            heldByUs.add(mapping);
        } else if (wasHeld(action)) {
            KeyMapping.set(accessor.ps4control$key(), false);
            heldByUs.remove(mapping);
        }
        prevButtons[index(action)] = pressed;
    }

    private static boolean wasHeld(String action) {
        return prevButtons[index(action)];
    }

    /** Movement keys: only release if we engaged them (keyboard never fights us). */
    private static void key(KeyMapping mapping, boolean pressed) {
        KeyMappingAccessor accessor = (KeyMappingAccessor) mapping;
        if (pressed) {
            KeyMapping.set(accessor.ps4control$key(), true);
            heldByUs.add(mapping);
        } else if (heldByUs.remove(mapping)) {
            KeyMapping.set(accessor.ps4control$key(), false);
        }
    }

    private static void releaseAll(Minecraft mc) {
        for (KeyMapping mapping : new HashSet<>(heldByUs)) {
            KeyMapping.set(((KeyMappingAccessor) mapping).ps4control$key(), false);
        }
        heldByUs.clear();
    }
}
