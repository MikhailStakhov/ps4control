package com.ps4control;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

public class Ps4ControlMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Ps4Config.load();
        ControllerManager.init();
        registerCommands();
        LOG("PS4Control loaded (SDL gamepad backend). " + ControllerManager.describe());
    }

    public static void LOG(String msg) {
        System.out.println("[PS4Control] " + msg);
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommands.literal("ps4control")
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
                    .then(ClientCommands.argument("guid", com.mojang.brigadier.arguments.StringArgumentType.string())
                    .executes(ctx -> {
                        String guid = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "guid");
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
                .then(ClientCommands.literal("reload").executes(ctx -> {
                    Ps4Config.load();
                    ctx.getSource().sendFeedback(Component.literal("Config reloaded: " + Ps4Config.get().summary()));
                    return 1;
                }))
        ));
    }
}
