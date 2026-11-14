package com.ps4control;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.sdl.SDLGamepad;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * Settings screen: controller picker (with live input readout so you can tell
 * two identical DualShock pads apart), sensitivity/deadzone sliders, toggles.
 * Everything saves immediately — no Done-then-apply.
 */
public class Ps4ControlConfigScreen extends Screen {

    private static final int COLOR_LABEL = 0xFFA0A0A0;
    private static final int COLOR_LIVE = 0xFFC8FFC8;
    private static final int COLOR_ERROR = 0xFFFF6060;
    private static final int COLOR_PANEL = 0x90000000;

    // controller picker state: index into options (0 = Auto, 1..n = pads)
    private List<PadOption> padOptions = new ArrayList<>();
    private int selectedPadIndex;

    public Ps4ControlConfigScreen() {
        super(Component.literal("PS4Control Settings"));
    }

    @Override
    protected void init() {
        refreshPadOptions();

        int centerX = this.width / 2;
        int left = centerX - 155;
        int col2 = centerX + 5;
        int w = 150;
        int row = Math.max(50, this.height / 4 - 30);
        int gap = 24;
        Ps4Config cfg = Ps4Config.get();

        // Row 1: controller picker (full width)
        addRenderableWidget(Button.builder(currentPadLabel(), b -> {
                cyclePad();
                b.setMessage(currentPadLabel());
            })
            .bounds(left, row, w * 2 + 10, 20)
            .tooltip(Tooltip.create(Component.literal(
                "Which controller this instance uses. 'Auto' = first gamepad found. "
                + "Press buttons on a pad and watch the live readout below to identify it.")))
            .build());

        row += gap;
        // Row 2: toggles
        CycleButton<Boolean> enabledBtn = addRenderableWidget(CycleButton.onOffBuilder(cfg.enabled)
            .displayOnlyValue()
            .create(Component.literal("Enabled: "), (cb, value) -> { cfg.enabled = value; save(); }));
        enabledBtn.setX(left);
        enabledBtn.setY(row);
        enabledBtn.setWidth(w);

        CycleButton<Boolean> invertBtn = addRenderableWidget(CycleButton.onOffBuilder(cfg.invertY)
            .displayOnlyValue()
            .create(Component.literal("Invert Y: "), (cb, value) -> { cfg.invertY = value; save(); }));
        invertBtn.setX(col2);
        invertBtn.setY(row);
        invertBtn.setWidth(w);

        row += gap + 4;
        // Row 3: sliders
        addRenderableWidget(slider(left, row, w, "Sensitivity", cfg.lookSensitivity, 4.0, 40.0,
            v -> cfg.lookSensitivity = round1(v)));
        addRenderableWidget(slider(col2, row, w, "Cursor Speed", cfg.cursorSpeed, 150.0, 900.0,
            v -> cfg.cursorSpeed = Math.round(v)));

        row += gap;
        // Row 4: deadzone + debug HUD toggle
        addRenderableWidget(slider(left, row, w, "Deadzone %", cfg.deadzone * 100.0, 5.0, 40.0,
            v -> cfg.deadzone = round2(v / 100.0)));

        CycleButton<Boolean> debugBtn = addRenderableWidget(CycleButton.onOffBuilder(cfg.debugHud)
            .displayOnlyValue()
            .create(Component.literal("Debug HUD: "), (cb, value) -> { cfg.debugHud = value; save(); }));
        debugBtn.setX(col2);
        debugBtn.setY(row);
        debugBtn.setWidth(w);
        debugBtn.setTooltip(Tooltip.create(Component.literal(
            "Shows every raw button/axis the controller sends, top-left. Press the touchpad / PS button and watch which 'RAW' number lights up.")));

        // Done button
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(centerX - 100, this.height - 30, 200, 20)
            .build());
    }

    private static String onOff(boolean v) { return v ? "ON" : "OFF"; }

    private AbstractSliderButton slider(int x, int y, int w, String label, double value,
                                        double min, double max, DoubleConsumer setter) {
        double unit = Math.max(0, Math.min(1, (value - min) / (max - min)));
        return new AbstractSliderButton(x, y, w, 20, Component.literal(label + ": " + fmt(value)), unit) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(label + ": " + fmt(fromUnit(this.value, min, max))));
            }

            @Override
            protected void applyValue() {
                setter.accept(fromUnit(this.value, min, max));
                save();
            }
        };
    }

    private static double fromUnit(double u, double min, double max) {
        return min + u * (max - min);
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static String fmt(double v) {
        if (v == Math.floor(v) && Math.abs(v) < 10000) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    // ---------------- controller picker ----------------

    private record PadOption(int deviceId, String guid, String name) {}

    private void refreshPadOptions() {
        padOptions = new ArrayList<>();
        padOptions.add(new PadOption(0, "", "Auto (first gamepad)"));
        for (String line : ControllerManager.listGamepads()) {
            // lines look like: "id 3: 'Wireless Controller' [GUID abc...]"
            PadOption opt = parsePadLine(line);
            if (opt != null) padOptions.add(opt);
        }
        String lock = Ps4Config.get().lockToJoystickGuid;
        selectedPadIndex = 0;
        for (int i = 1; i < padOptions.size(); i++) {
            if (padOptions.get(i).guid().equals(lock)) {
                selectedPadIndex = i;
                break;
            }
        }
        if (!lock.isEmpty() && selectedPadIndex == 0) {
            // locked to a pad that isn't connected right now — keep it selectable
            padOptions.add(new PadOption(-1, lock, "Locked (not connected)"));
            selectedPadIndex = padOptions.size() - 1;
        }
    }

    private static PadOption parsePadLine(String line) {
        try {
            int idStart = line.indexOf("id ") + 3;
            int idEnd = line.indexOf(':', idStart);
            int guidStart = line.indexOf("GUID ") + 5;
            int nameStart = line.indexOf(": '") + 3;
            int nameEnd = line.lastIndexOf("' [");
            if (idEnd < 0 || guidStart < 5 || nameStart < 3 || nameEnd < 0) return null;
            int id = Integer.parseInt(line.substring(idStart, idEnd).trim());
            String guid = line.substring(guidStart).trim();
            String name = line.substring(nameStart, nameEnd);
            return new PadOption(id, guid, name);
        } catch (Exception e) {
            return null;
        }
    }

    private Component currentPadLabel() {
        return Component.literal("Controller: " + padOptions.get(selectedPadIndex).name());
    }

    private void cyclePad() {
        selectedPadIndex = (selectedPadIndex + 1) % padOptions.size();
        PadOption opt = padOptions.get(selectedPadIndex);
        Ps4Config.get().lockToJoystickGuid = opt.guid();
        save();
        ControllerManager.rebind();
    }

    private static void save() {
        Ps4Config.save();
    }

    // ---------------- live input readout ----------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        var font = this.minecraft.font;
        int panelTop = this.height - 130;
        int centerX = this.width / 2;
        graphics.fill(centerX - 160, panelTop, centerX + 160, this.height - 36, COLOR_PANEL);
        graphics.centeredText(font, "Live input — bound controller", centerX, panelTop + 4, COLOR_LABEL);

        long pad = ControllerManager.poll();
        if (pad == 0) {
            graphics.centeredText(font, "No controller connected", centerX, panelTop + 20, COLOR_ERROR);
            return;
        }

        int y = panelTop + 18;
        graphics.text(font, Component.literal(ControllerManager.boundDescription()), centerX - 152, y, COLOR_LIVE);
        y += 12;

        float lx = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX) / 32767f;
        float ly = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY) / 32767f;
        float rx = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX) / 32767f;
        float ry = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY) / 32767f;
        graphics.text(font, Component.literal(String.format("L-stick  X %+.2f  Y %+.2f", lx, ly)), centerX - 152, y, COLOR_LIVE);
        graphics.text(font, Component.literal(String.format("R-stick  X %+.2f  Y %+.2f", rx, ry)), centerX + 8, y, COLOR_LIVE);
        y += 12;

        StringBuilder pressed = new StringBuilder();
        String[] names = {"cross", "circle", "square", "triangle", "l1", "r1", "l3", "r3",
            "back", "start", "guide", "dpad_up", "dpad_down", "dpad_left", "dpad_right", "touchpad"};
        for (String n : names) {
            int code = Bindings.code(n);
            if (code >= 0 && SDLGamepad.SDL_GetGamepadButton(pad, code)) {
                if (pressed.length() > 0) pressed.append(' ');
                pressed.append(n);
            }
        }
        float l2 = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER) / 32767f;
        float r2 = SDLGamepad.SDL_GetGamepadAxis(pad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER) / 32767f;
        String line = "Buttons: " + (pressed.isEmpty() ? "(none)" : pressed.toString())
            + String.format("  L2 %+.2f  R2 %+.2f", l2, r2);
        graphics.text(font, Component.literal(line), centerX - 152, y, COLOR_LIVE);
    }
}
