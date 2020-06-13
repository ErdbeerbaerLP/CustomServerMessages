package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraftforge.fml.StartupQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(value = StartupQuery.class, remap = false)
public class MixinStartupQuery {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void query(String header, String text, String action, AtomicBoolean result, CallbackInfo ci) {
        CustomServerMessagesMod.launchTimeValid = false;
    }
}
