package com.ps4control;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class Ps4ControlMod implements ClientModInitializer {

    public static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ps4control", "main"));

    /** Opens the settings GUI. Unbound by default — bind it in Options > Controls. */
    public static final KeyMapping MENU_KEY = KeyMappingHelper.registerKeyMapping(
        new KeyMapping("key.ps4control.menu", InputConstants.UNKNOWN.getValue(), CATEGORY));

    private static final Identifier DEBUG_HUD_ID = Identifier.fromNamespaceAndPath("ps4control", "debug");

    @Override
    public void onInitializeClient() {
        Ps4Config.load();
        ControllerManager.init();
        registerCommands();
        HudElementRegistry.addLast(DEBUG_HUD_ID, new DebugHud());
        LOG("PS4Control loaded (SDL gamepad backend). " + ControllerManager.describe());
    }

    public static void LOG(String msg) {
        System.out.println("[PS4Control] " + msg);
    }

    public static void openConfigScreen() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        mc.setScreenAndShow(new Ps4ControlConfigScreen());
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommands.literal("ps4control")
                .executes(ctx -> {
                    openConfigScreen();
                    return 1;
                })
                .then(ClientCommands.literal("menu").executes(ctx -> {
                    openConfigScreen();
                    return 1;
                }))
                .then(ClientCommands.literal("status").executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal(
                        "PS4Control: " + ControllerManager.describe()
                        + " | bound: " + ControllerManager.boundDescription()
                        + " | " + Ps4Config.get().summary()));
                    return 1;
                }))
                .then(ClientCommands.literal("list").executes(ctx -> {
                    for (String line : ControllerManager.listGamepads()) {
                        ctx.getSource().sendFeedback(Component.literal(line));
                    }
                    return 1;
                }))
                .then(ClientCommands.literal("bind")
                    .then(ClientCommands.argument("guid", StringArgumentType.string())
                    .executes(ctx -> {
                        String guid = StringArgumentType.getString(ctx, "guid");
                        Ps4Config.get().lockToJoystickGuid = guid;
                        Ps4Config.save();
                        ControllerManager.rebind();
                        ctx.getSource().sendFeedback(Component.literal("Lock set to GUID " + guid + ". Bound: " + ControllerManager.boundDescription()));
                        return 1;
                    })))
                .then(ClientCommands.literal("auto").executes(ctx -> {
                    Ps4Config.get().lockToJoystickGuid = "";
                    Ps4Config.save();
                    ControllerManager.rebind();
                    ctx.getSource().sendFeedback(Component.literal("Controller lock cleared; using first gamepad: " + ControllerManager.boundDescription()));
                    return 1;
                }))
                .then(ClientCommands.literal("debug").executes(ctx -> {
                    var cfg = Ps4Config.get();
                    cfg.debugHud = !cfg.debugHud;
                    Ps4Config.save();
                    ctx.getSource().sendFeedback(Component.literal(
                        "Debug HUD " + (cfg.debugHud ? "ON — look at the top-left corner of the screen" : "OFF")));
                    return 1;
                }))
                .then(ClientCommands.literal("reload").executes(ctx -> {
                    Ps4Config.load();
                    ctx.getSource().sendFeedback(Component.literal("Config reloaded: " + Ps4Config.get().summary()));
                    return 1;
                }))
        ));
    }
}
