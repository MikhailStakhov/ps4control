package com.ps4control;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Config stored at config/ps4control.json.
 * lockToJoystickGuid is the key setting for split-screen: each game instance
 * (e.g. PrismLauncher instance for player 2) should lock to a different
 * controller GUID so instances don't fight over the same gamepad.
 */
public class Ps4Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Ps4Config instance;

    public boolean enabled = true;

    /** Empty = bind to first gamepad found. Otherwise a GLFW joystick GUID (use /ps4control list + bind). */
    public String lockToJoystickGuid = "";

    public double lookSensitivity = 18.0;
    public double cursorSpeed = 450.0;
    public double deadzone = 0.15;
    public boolean invertY = false;

    /** action name -> GLFW button name (see Bindings class for the vocabulary). */
    public Map<String, String> bindings = defaultBindings();

    public static Map<String, String> defaultBindings() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("jump", "cross");
        m.put("sneak", "circle");
        m.put("inventory", "square");
        m.put("drop", "triangle");
        m.put("attack", "r2");
        m.put("use", "l2");
        m.put("hotbarNext", "r1");
        m.put("hotbarPrev", "l1");
        m.put("sprint", "l3");
        m.put("perspective", "r3");
        m.put("chat", "dpad_up");
        m.put("pause", "dpad_down");
        return m;
    }

    public static Ps4Config get() {
        if (instance == null) load();
        return instance;
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("ps4control.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try {
                instance = GSON.fromJson(Files.readString(p), Ps4Config.class);
            } catch (Exception e) {
                System.err.println("[PS4Control] Failed to read config, using defaults: " + e);
            }
        }
        if (instance == null) instance = new Ps4Config();
        if (instance.bindings == null) instance.bindings = defaultBindings();
        instance.bindings.putAll(defaultBindings()); // fill in any new actions
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(instance));
        } catch (IOException e) {
            System.err.println("[PS4Control] Failed to save config: " + e);
        }
    }

    public String summary() {
        return "enabled=" + enabled
            + ", lock=" + (lockToJoystickGuid.isEmpty() ? "<auto>" : lockToJoystickGuid)
            + ", sensitivity=" + lookSensitivity
            + ", deadzone=" + deadzone;
    }
}
