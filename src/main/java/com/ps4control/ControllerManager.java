package com.ps4control;

import org.lwjgl.sdl.SDL_GUID;
import org.lwjgl.sdl.SDLGamepad;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers gamepads through SDL's GameController API — the exact library
 * Minecraft 26.x itself uses (bundled via LWJGL). SDL ships DualShock 4 /
 * DualSense mappings out of the box, and we add no native libraries of our
 * own, so there is no extra native crash surface (unlike Controlify's bundled
 * SDL, which caused the "Xulu platform x64" error).
 *
 * We poll device STATE each frame (SDL_GetGamepadButton/Axis) rather than
 * events, because Minecraft flushes gamepad events from the SDL event queue.
 */
public final class ControllerManager {

    private static long boundPad;
    private static int boundId;
    private static String boundGuid = "";
    private static String boundName = "";

    private ControllerManager() {}

    public static void init() {
        // Nothing to do at init — SDL is already initialized by the game.
        rebind();
    }

    /** Re-evaluates which gamepad this instance should use based on the config lock. */
    public static synchronized void rebind() {
        if (boundPad != 0) {
            SDLGamepad.SDL_CloseGamepad(boundPad);
            boundPad = 0;
        }
        boundGuid = "";
        boundName = "";
        boundId = 0;

        String lock = Ps4Config.get().lockToJoystickGuid;
        IntBuffer ids = SDLGamepad.SDL_GetGamepads();
        if (ids == null) {
            Ps4ControlMod.LOG("No gamepads connected (waiting)");
            return;
        }

        int firstId = 0;
        boolean haveFirst = false;
        for (int i = 0; i < ids.remaining(); i++) {
            int id = ids.get(i);
            String guid = guidOfId(id);
            if (!haveFirst) {
                firstId = id;
                haveFirst = true;
            }
            if (guid != null && !lock.isEmpty() && guid.equals(lock)) {
                boundId = id;
                boundGuid = guid;
                break;
            }
        }
        if (boundGuid.isEmpty() && haveFirst) {
            // no lock configured or lock didn't match: use the first gamepad
            boundId = firstId;
            boundGuid = guidOfId(firstId);
        }

        if (!boundGuid.isEmpty()) {
            boundName = safeName(boundId);
            boundPad = SDLGamepad.SDL_OpenGamepad(boundId);
            if (boundPad != 0) {
                Ps4ControlMod.LOG("Bound to gamepad '" + boundName + "' (id " + boundId + ", GUID " + boundGuid + ")");
            } else {
                Ps4ControlMod.LOG("Failed to open gamepad id " + boundId);
                boundGuid = "";
                boundName = "";
            }
        } else {
            Ps4ControlMod.LOG("No gamepad bound (waiting for a controller)");
        }
    }

    private static String safeName(int id) {
        try {
            String n = SDLGamepad.SDL_GetGamepadNameForID(id);
            return n == null ? "?" : n;
        } catch (Throwable t) {
            return "?";
        }
    }

    public static String guidOfId(int id) {
        try {
            SDL_GUID guid = SDLGamepad.SDL_GetGamepadGUIDForID(id, SDL_GUID.calloc());
            if (guid == null) return null;
            try {
                ByteBuffer data = guid.data();
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                StringBuilder sb = new StringBuilder(bytes.length * 2);
                for (byte b : bytes) sb.append(String.format("%02x", b));
                return sb.toString();
            } finally {
                guid.close();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /** Refreshes state; returns the open gamepad handle, or 0 (rebinding attempted). */
    public static synchronized long poll() {
        if (boundPad == 0 || SDLGamepad.SDL_GetGamepadConnectionState(boundPad) != 1) {
            rebind();
        }
        if (boundPad != 0) {
            SDLGamepad.SDL_UpdateGamepads();
        }
        return boundPad;
    }

    public static List<String> listGamepads() {
        List<String> out = new ArrayList<>();
        IntBuffer ids = SDLGamepad.SDL_GetGamepads();
        if (ids != null) {
            for (int i = 0; i < ids.remaining(); i++) {
                int id = ids.get(i);
                out.add("id " + id + ": " + safeName(id) + " [GUID " + guidOfId(id) + "]");
            }
        }
        if (out.isEmpty()) out.add("No gamepads connected.");
        return out;
    }

    public static String describe() {
        IntBuffer ids = SDLGamepad.SDL_GetGamepads();
        int count = ids == null ? 0 : ids.remaining();
        return count + " gamepad(s)";
    }

    public static String boundDescription() {
        if (boundPad == 0) return "none (waiting for controller)";
        return "'" + boundName + "' (id " + boundId + ", GUID " + boundGuid + ")";
    }
}
