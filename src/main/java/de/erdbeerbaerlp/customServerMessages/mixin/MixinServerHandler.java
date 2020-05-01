package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraftforge.fml.common.StartupQuery;
import net.minecraftforge.fml.server.FMLServerHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FMLServerHandler.class, remap = false)
public class MixinServerHandler {
    @Inject(method = "queryUser", at = @At("HEAD"))
    private void query(StartupQuery query, CallbackInfo ci) {
        CustomServerMessagesMod.launchTimeValid = false;
    }
}
