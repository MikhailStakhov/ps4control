package com.ps4control;

import org.lwjgl.sdl.SDLGamepad;

import java.util.HashMap;
import java.util.Map;

/**
 * Translates friendly button names (used in config/ps4control.json) to SDL3
 * gamepad button codes. PlayStation button letters map to SDL positions:
 * cross=SOUTH, circle=EAST, square=WEST, triangle=NORTH.
 */
public final class Bindings {

    private static final Map<String, Integer> NAMES = new HashMap<>();
    private static final Map<Integer, String> CODES = new HashMap<>();

    static {
        add("cross", SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH);
        add("circle", SDLGamepad.SDL_GAMEPAD_BUTTON_EAST);
        add("square", SDLGamepad.SDL_GAMEPAD_BUTTON_WEST);
        add("triangle", SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH);
        add("l1", SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER);
        add("r1", SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER);
        add("back", SDLGamepad.SDL_GAMEPAD_BUTTON_BACK);
        add("start", SDLGamepad.SDL_GAMEPAD_BUTTON_START);
        add("guide", SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE);
        add("l3", SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK);
        add("r3", SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK);
        add("touchpad", SDLGamepad.SDL_GAMEPAD_BUTTON_TOUCHPAD);
        add("dpad_up", SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP);
        add("dpad_right", SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT);
        add("dpad_down", SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN);
        add("dpad_left", SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT);
    }

    private static void add(String name, int code) {
        NAMES.put(name, code);
        CODES.put(code, name);
    }

    public static int code(String name) {
        Integer i = NAMES.get(name);
        return i == null ? -1 : i;
    }

    public static String name(int code) {
        String s = CODES.get(code);
        return s == null ? "?" : s;
    }

    private Bindings() {}
}
