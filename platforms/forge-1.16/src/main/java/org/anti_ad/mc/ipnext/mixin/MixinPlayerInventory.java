package org.anti_ad.mc.ipnext.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.anti_ad.mc.ipnext.config.ModSettings;
import org.anti_ad.mc.ipnext.event.LockSlotsHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class MixinPlayerInventory {


    @Shadow @Final public NonNullList<ItemStack> mainInventory;

    @Inject(at = @At(value = "HEAD", target = "Lnet/minecraft/entity/player/PlayerInventory;getFirstEmptyStack()I"),
            method = "getFirstEmptyStack",
            cancellable = true)
    public void getEmptySlot(CallbackInfoReturnable<Integer> info) {
        if (!ModSettings.INSTANCE.getLOCKED_SLOTS_ALLOW_PICKUP_INTO_EMPTY().getValue()) {
            for (int i = 0; i < this.mainInventory.size(); ++i) {
                if (!LockSlotsHandler.INSTANCE.isSlotLocked(i)) {
                    if ((this.mainInventory.get(i)).isEmpty()) {
                        info.setReturnValue(i);
                        return;
                    }
                }
            }
            info.setReturnValue(-1);
        }
    }
}
