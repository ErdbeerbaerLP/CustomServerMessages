package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.CustomMessages;
import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.network.NetworkSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.PropertyManager;
import net.minecraft.util.CryptManager;
import net.minecraftforge.fml.common.*;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.transformer.MixinProcessor;
import org.spongepowered.asm.mixin.transformer.Proxy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Mixin(value = Loader.class, remap = false)
public class MixinLoader {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        //Add mixin config...
        Mixins.addConfiguration("mixins.customservermsgs.json");

        Proxy mixinProxy = (Proxy) Launch.classLoader.getTransformers().stream().filter(transformer -> transformer instanceof Proxy).findFirst().get();
        try {
            //This will very likely break on the next major mixin release.
            Class mixinTransformerClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");

            Field transformerField = Proxy.class.getDeclaredField("transformer");
            transformerField.setAccessible(true);

            Object transformer = transformerField.get(mixinProxy);

            //Get MixinProcessor from MixinTransformer
            Field processorField = mixinTransformerClass.getDeclaredField("processor");
            processorField.setAccessible(true);
            Object processor = processorField.get(transformer);

            Method selectConfigsMethod = MixinProcessor.class.getDeclaredMethod("selectConfigs", MixinEnvironment.class);
            selectConfigsMethod.setAccessible(true);
            selectConfigsMethod.invoke(processor, MixinEnvironment.getCurrentEnvironment());

            Method prepareConfigsMethod = MixinProcessor.class.getDeclaredMethod("prepareConfigs", MixinEnvironment.class);
            prepareConfigsMethod.setAccessible(true);
            prepareConfigsMethod.invoke(processor, MixinEnvironment.getCurrentEnvironment());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Redirect(method = "loadMods", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;step(Ljava/lang/String;)V", ordinal = 1))
    private void load(ProgressManager.ProgressBar progressBar, String message) {
        CustomMessages.preInit();
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
        final Logger LOGGER = FMLLog.getLogger();
        final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        LOGGER.info("Loading properties");
        final PropertyManager settings;
        ObfuscationReflectionHelper.setPrivateValue(DedicatedServer.class, (DedicatedServer) server, settings = new PropertyManager(new File("server.properties")), "settings", "field_71340_o", "q");
        server.setOnlineMode(settings.getBooleanProperty("online-mode", true));
        server.setPreventProxyConnections(settings.getBooleanProperty("prevent-proxy-connections", false));
        server.setHostname(settings.getStringProperty("server-ip", ""));
        InetAddress inetaddress = null;

        if (!server.getServerHostname().isEmpty()) {
            try {
                inetaddress = InetAddress.getByName(server.getServerHostname());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        if (server.getServerPort() < 0) {
            server.setServerPort(settings.getIntProperty("server-port", 25565));
        }

        LOGGER.info("Generating keypair");
        server.setKeyPair(CryptManager.generateKeyPair());
        LOGGER.info("Starting Minecraft server on {}:{}", server.getServerHostname().isEmpty() ? "*" : server.getServerHostname(), Integer.valueOf(server.getServerPort()));

        try {
            server.getNetworkSystem().addLanEndpoint(inetaddress, server.getServerPort());
        } catch (IOException ioexception) {
            LOGGER.warn("**** FAILED TO BIND TO PORT!");
            LOGGER.warn("The exception was: {}", ioexception.toString());
            LOGGER.warn("Perhaps a server is already running on that port?");
            FMLCommonHandler.instance().exitJava(1, true);
        }

        if (!server.isServerInOnlineMode()) {
            LOGGER.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            LOGGER.warn("The server will make no attempt to authenticate usernames. Beware.");
            LOGGER.warn("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
            LOGGER.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
        }
        CustomServerMessagesMod.vanillaSystem = server.networkSystem;
        CustomServerMessagesMod.preServerThread = new CustomServerMessagesMod.PreServerThread(server.networkSystem = new NetworkSystem(server));
        CustomServerMessagesMod.preServerThread.start();

        Thread r = new Thread(() -> {
            if (CustomMessages.DEV_AUTO_RELOAD_CONFIG_SEC != 0)
                while (true) {
                    if (CustomMessages.DEV_AUTO_RELOAD_CONFIG_SEC != 0) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(CustomMessages.DEV_AUTO_RELOAD_CONFIG_SEC));
                            CustomMessages.preInit();
                        } catch (InterruptedException e) {
                            System.err.println("Error auto-reloading config: InterruptedException");
                        }
                    }
                }
        });
        r.setDaemon(true);
        r.setPriority(Thread.MAX_PRIORITY);
        r.start();
        if (CustomMessages.DEV_DELAY_SERVER) {
            try {
                TimeUnit.SECONDS.sleep(9999);
            } catch (InterruptedException e) {
                System.err.println("Got interrupted while delaying server");
            }
        }
        progressBar.step(message);
    }
}
