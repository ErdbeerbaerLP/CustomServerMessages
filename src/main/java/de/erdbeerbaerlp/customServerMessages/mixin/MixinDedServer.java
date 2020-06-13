package de.erdbeerbaerlp.customServerMessages.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import de.erdbeerbaerlp.customServerMessages.CustomMessagesConfig;
import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraft.command.Commands;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.rcon.IServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerPropertiesProvider;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.ServerProperties;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.CryptManager;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.fml.server.ServerModLoader;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Mixin(DedicatedServer.class)
public abstract class MixinDedServer extends MinecraftServer implements IServer {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private ServerPropertiesProvider settings;

    public MixinDedServer(File p_i50590_1_, Proxy p_i50590_2_, DataFixer dataFixerIn, Commands p_i50590_4_, YggdrasilAuthenticationService p_i50590_5_, MinecraftSessionService p_i50590_6_, GameProfileRepository p_i50590_7_, PlayerProfileCache p_i50590_8_, IChunkStatusListenerFactory p_i50590_9_, String p_i50590_10_) {
        super(p_i50590_1_, p_i50590_2_, dataFixerIn, p_i50590_4_, p_i50590_5_, p_i50590_6_, p_i50590_7_, p_i50590_8_, p_i50590_9_, p_i50590_10_);
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/server/ServerModLoader;begin(Lnet/minecraft/server/dedicated/DedicatedServer;)V"), remap = false)
    private void begin(final DedicatedServer dedicatedServer) {
        CustomServerMessagesMod.serverLaunched = Instant.now();
        try {
            if (!CustomServerMessagesMod.estimatedTimeFile.exists()) {
                CustomServerMessagesMod.estimatedTimeFile.createNewFile();
                final BufferedWriter w = new BufferedWriter(new FileWriter(CustomServerMessagesMod.estimatedTimeFile));
                w.write("0");
                w.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Loading properties");
        settings.func_219033_a(properties -> ServerProperties.create(Paths.get("server.properties")));
        ServerProperties serverproperties = this.settings.getProperties();
        dedicatedServer.setOnlineMode(serverproperties.onlineMode);
        dedicatedServer.setPreventProxyConnections(serverproperties.preventProxyConnections);
        dedicatedServer.setHostname(serverproperties.serverIp);
        InetAddress inetaddress = null;
        if (!dedicatedServer.getServerHostname().isEmpty()) {
            try {
                inetaddress = InetAddress.getByName(dedicatedServer.getServerHostname());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        if (dedicatedServer.getServerPort() < 0) {
            dedicatedServer.setServerPort(serverproperties.serverPort);
        }

        LOGGER.info("Generating keypair");
        dedicatedServer.setKeyPair(CryptManager.generateKeyPair());
        LOGGER.info("Starting Minecraft server on {}:{}", dedicatedServer.getServerHostname().isEmpty() ? "*" : dedicatedServer.getServerHostname(), Integer.valueOf(dedicatedServer.getServerPort()));

        try {
            dedicatedServer.getNetworkSystem().addEndpoint(inetaddress, dedicatedServer.getServerPort());
        } catch (IOException ioexception) {
            LOGGER.warn("**** FAILED TO BIND TO PORT!");
            LOGGER.warn("The exception was: {}", ioexception.toString());
            LOGGER.warn("Perhaps a server is already running on that port?");
            ServerLifecycleHooks.handleExit(1);
        }

        if (!dedicatedServer.isServerInOnlineMode()) {
            LOGGER.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            LOGGER.warn("The server will make no attempt to authenticate usernames. Beware.");
            LOGGER.warn("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
            LOGGER.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
        }
        //Creating an new instance of the NetworkSystem fixes an small bug
        CustomServerMessagesMod.vanillaSystem = dedicatedServer.getNetworkSystem();
        ObfuscationReflectionHelper.setPrivateValue(MinecraftServer.class, dedicatedServer, new NetworkSystem(dedicatedServer), "field_147144_o");

        CustomServerMessagesMod.earlyNetworkSystem = new CustomServerMessagesMod.EarlyServerThread(dedicatedServer.getNetworkSystem());
        CustomServerMessagesMod.earlyNetworkSystem.start();

        Thread r = new Thread(() -> {
            if (CustomMessagesConfig.instance().dev.autoReloadConfig != 0)
                while (true) {
                    if (CustomMessagesConfig.instance().dev.autoReloadConfig != 0) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(CustomMessagesConfig.instance().dev.autoReloadConfig));
                            CustomMessagesConfig.instance().loadConfig();
                        } catch (InterruptedException e) {
                            System.err.println("Error auto-reloading config: InterruptedException");
                        }
                    }
                }
        });
        r.setDaemon(true);
        r.setPriority(Thread.MAX_PRIORITY);
        r.start();
        if (CustomMessagesConfig.instance().dev.delayServerStart) {
            try {
                TimeUnit.SECONDS.sleep(9999);
            } catch (InterruptedException e) {
                System.err.println("Got interrupted while delaying server");
            }
        }
        ServerModLoader.begin(dedicatedServer);
    }

    // Removing already handled stuff...
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;setKeyPair(Ljava/security/KeyPair;)V", ordinal = 0))
    private void keypair(DedicatedServer dedicatedServer, KeyPair keyPair) {
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkSystem;addEndpoint(Ljava/net/InetAddress;I)V", ordinal = 0))
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
