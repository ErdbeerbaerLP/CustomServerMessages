package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public class MixinMCServer {
    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;applyServerIconToResponse(Lnet/minecraft/network/ServerStatusResponse;)V", ordinal = 0))
    private void run(MinecraftServer minecraftServer, ServerStatusResponse response) {
        CustomServerMessagesMod.preServer = false;
        minecraftServer.applyServerIconToResponse(response);
    }
}
