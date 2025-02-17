package org.anti_ad.mc.ipnext.mixin;

import net.minecraft.client.render.GameRenderer;
import org.anti_ad.mc.ipnext.gui.inject.ScreenEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinGameRenderer
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;" +
            "render(IIF)V"), method = "render")
    public void preScreenRender(float f, long l, boolean bl, CallbackInfo ci) {
        ScreenEventHandler.INSTANCE.preRender();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;" +
            "render(IIF)V", shift = At.Shift.AFTER), method = "render")
    public void postScreenRender(float f, long l, boolean bl, CallbackInfo ci) {
        ScreenEventHandler.INSTANCE.postRender();
    }
}