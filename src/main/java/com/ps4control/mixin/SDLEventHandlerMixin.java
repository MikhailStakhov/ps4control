package com.ps4control.mixin;

import com.ps4control.InputHandler;
import com.mojang.blaze3d.platform.SDLEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the per-frame SDL event poll so controller state is refreshed and
 * applied smoothly (frame rate, not 20 Hz ticks) — essential for camera.
 */
@Mixin(SDLEventHandler.class)
public abstract class SDLEventHandlerMixin {

    @Inject(method = "pollEvents", at = @At("HEAD"))
    private void ps4control$onFrame(CallbackInfo ci) {
        InputHandler.onFrame();
    }
}
