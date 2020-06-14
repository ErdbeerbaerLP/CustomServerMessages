package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraftforge.fml.StartupQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StartupQuery.class, remap = false)
public class MixinStartupQuery {
    @Inject(method = "execute", at = @At("HEAD"))
    private void query(CallbackInfo ci) {
        CustomServerMessagesMod.launchTimeValid = false;
    }
}
