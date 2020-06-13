package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraft.network.NetworkSystem;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public class MixinMCServer {
    @Redirect(method = "updateTimeLightAndEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkSystem;tick()V", ordinal = 0))
    private void run(NetworkSystem networkSystem) {
        if (CustomServerMessagesMod.preServer)
            CustomServerMessagesMod.preServer = false;
        networkSystem.tick();
    }
}
