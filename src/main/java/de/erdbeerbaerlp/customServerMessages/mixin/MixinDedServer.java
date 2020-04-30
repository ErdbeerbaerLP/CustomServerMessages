package de.erdbeerbaerlp.customServerMessages.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.rcon.IServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.datafix.DataFixer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "init", at = @At("HEAD"))
    private void init(CallbackInfoReturnable<Boolean> cir) {
        //getNetworkSystem().terminateEndpoints();
        //ObfuscationReflectionHelper.setPrivateValue(MinecraftServer.class,this,new NetworkSystem(this),"networkSystem","p","field_147144_o");

    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;handleServerAboutToStart(Lnet/minecraft/server/MinecraftServer;)Z"))
    private boolean init2(FMLCommonHandler fmlCommonHandler, MinecraftServer server) {
        return fmlCommonHandler.handleServerAboutToStart(server);
    }


    // Removing already handled stuff...
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkSystem;addLanEndpoint(Ljava/net/InetAddress;I)V", ordinal = 0))
    private void addEndpoint(NetworkSystem networkSystem, InetAddress address, int port) {
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;setKeyPair(Ljava/security/KeyPair;)V", ordinal = 0))
    private void keypair(DedicatedServer dedicatedServer, KeyPair keyPair) {
        CustomServerMessagesMod.preServer = false;
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;setServerPort(I)V", ordinal = 0))
    private void setPort(DedicatedServer dedicatedServer, int port) {
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", ordinal = 0))
    private void logServerStart(Logger logger, String message, Object p0, Object p1) {
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;)V", ordinal = 0))
    private void logInfo(Logger logger, String message) {
        if (!message.equals("Generating keypair")) logger.info(message);
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;isServerInOnlineMode()Z", ordinal = 0))
    private boolean isOnlineMode(DedicatedServer dedicatedServer) {
        return true;
    }

}
