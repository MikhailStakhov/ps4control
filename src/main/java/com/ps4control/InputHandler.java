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
        Minecraft mc = Minecraft.getInstance();
        // settings-menu key works anywhere (menu or gameplay)
        if (Ps4ControlMod.MENU_KEY.consumeClick()) {
            Ps4ControlMod.openConfigScreen();
            return;
        }
        Ps4Config cfg = Ps4Config.get();
        if (!cfg.enabled || mc.player == null && mc.gui.screen() == null) {
            return;
        }
        long pad = ControllerManager.poll();
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

        // --- camera: right stick
        double rx = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX, cfg.deadzone);
        double ry = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY, cfg.deadzone);
        if (rx != 0 || ry != 0) {
            double yaw = rx * cfg.lookSensitivity * 0.15;
            double pitch = (cfg.invertY ? -ry : ry) * cfg.lookSensitivity * 0.15;
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
        // left stick moves the virtual cursor via MouseHandler.onMove — the same
        // path a physical mouse takes, so vanilla GUI hover logic just works.
        double lx = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, cfg.deadzone);
        double ly = axis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY, cfg.deadzone);
        if (lx != 0 || ly != 0) {
            double dx = lx * cfg.cursorSpeed / 60.0;
            double dy = ly * cfg.cursorSpeed / 60.0;
            double nx = mc.mouseHandler.xpos() + dx;
            double ny = mc.mouseHandler.ypos() + dy;
            mc.mouseHandler.onMove(0, nx, ny, dx, dy);
        }

        // cross = left click, circle = right click (press on rising edge, release on falling)
        clickPair(mc, pad, "jump", 0);
        clickPair(mc, pad, "sneak", 1);

        // dpad_down closes most screens (convenience "back")
        if (edge(pad, "pause") && mc.gui.screen() != null) {
            mc.gui.screen().onClose();
        }
    }

    /** Emulates a mouse press/release pair for a gamepad action. */
    private static void clickPair(Minecraft mc, long pad, String action, int button) {
        int idx = index(action);
        boolean now = rawDown(pad, action);
        boolean was = prevButtons[idx];
        prevButtons[idx] = now;
        if (now && !was) {
            mc.mouseHandler.onButton(0, new MouseButtonInfo(button, 0), 1);
        } else if (!now && was) {
            mc.mouseHandler.onButton(0, new MouseButtonInfo(button, 0), 0);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** SDL axis values are S16; convert to [-1,1] with deadzone + rescale. */
    private static double axis(long pad, int axis, double deadzone) {
        float raw = SDLGamepad.SDL_GetGamepadAxis(pad, axis) / 32767.0f;
        if (Math.abs(raw) < deadzone) return 0;
        double sign = Math.signum(raw);
        return sign * (Math.abs(raw) - deadzone) / (1.0 - deadzone);
    }

    private static boolean rawDown(long pad, String action) {
        int code = Bindings.code(Ps4Config.get().bindings.getOrDefault(action, ""));
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
        int code = Bindings.code(Ps4Config.get().bindings.getOrDefault(action, ""));
        return Math.max(0, Math.min(BTN_COUNT - 1, code));
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
