package de.erdbeerbaerlp.customServerMessages.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.rcon.IServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.datafix.DataFixer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.net.InetAddress;
import java.net.Proxy;
import java.security.KeyPair;

@Mixin(DedicatedServer.class)
public abstract class MixinDedServer extends MinecraftServer implements IServer {
    @Shadow
    @Final
    private static Logger LOGGER;

    public MixinDedServer(File anvilFileIn, Proxy proxyIn, DataFixer dataFixerIn, YggdrasilAuthenticationService authServiceIn, MinecraftSessionService sessionServiceIn, GameProfileRepository profileRepoIn, PlayerProfileCache profileCacheIn) {
        super(anvilFileIn, proxyIn, dataFixerIn, authServiceIn, sessionServiceIn, profileRepoIn, profileCacheIn);
    }

    // Removing already handled stuff...

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;setKeyPair(Ljava/security/KeyPair;)V", ordinal = 0))
    private void keypair(DedicatedServer dedicatedServer, KeyPair keyPair) {
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkSystem;addLanEndpoint(Ljava/net/InetAddress;I)V", ordinal = 0))
    private void addEndpoint(NetworkSystem networkSystem, InetAddress address, int port) {
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;setServerPort(I)V", ordinal = 0))
    private void setPort(DedicatedServer dedicatedServer, int port) {
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", ordinal = 0, remap = false))
    private void logServerStart(Logger logger, String message, Object p0, Object p1) {
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;isServerInOnlineMode()Z", ordinal = 0))
    private boolean isOnlineMode(DedicatedServer dedicatedServer) {
        return true;
    }

}
