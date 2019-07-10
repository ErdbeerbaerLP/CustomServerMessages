package de.erdbeerbaerlp.customServerMessages;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import net.minecraft.network.NetworkSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.PropertyManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid=CustomServerMessagesMod.MODID, version = CustomServerMessagesMod.VERSION, name = CustomServerMessagesMod.NAME, serverSideOnly = true, acceptableRemoteVersions = "*")
@EventBusSubscriber
public class CustomServerMessagesMod {
	public static final String MODID = "servermsgs";
	public static final String NAME = "Custom Server Messages";
	public static final String VERSION = "2.0.0";
	static boolean serverStarted = false;
	private static boolean preServer = true;
	private static final Logger LOGGER = FMLLog.log;
	static PropertyManager settings;
	private final NetworkSystem s = new CustomNetworkSystem();

	{
		CustomMessages.preInit();
		final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

		LOGGER.info("Loading properties");
		CustomServerMessagesMod.settings = new PropertyManager(new File("server.properties"));

		ObfuscationReflectionHelper.setPrivateValue(MinecraftServer.class, server, s, "networkSystem", "field_147144_o");
		InetAddress inetaddress = null;
		final String ip = CustomServerMessagesMod.settings.getStringProperty("server-ip", "");
		final int port = CustomServerMessagesMod.settings.getIntProperty("server-port", 25565);
		if (!ip.isEmpty())
		{
			try {
				inetaddress = InetAddress.getByName(ip);
			} catch (UnknownHostException e) {}
		}
		LOGGER.info("Starting Pre-Minecraft server on {}:{}", ip.isEmpty() ? "*" : ip, Integer.valueOf(port));

		try
		{
			s.addLanEndpoint(inetaddress, port);
		}
		catch (IOException ioexception)
		{
			LOGGER.warn("**** FAILED TO BIND TO PORT!");
			LOGGER.warn("The exception was: {}", (Object)ioexception.toString());
			LOGGER.warn("Perhaps a server is already running on that port?");
			preServer = false;
			FMLCommonHandler.instance().exitJava(-1, true);
		}
		new Thread() {
			{
				setName("Early Network Handler");
				setDaemon(true);
			}
			public void run() {
				while(preServer) {
					s.networkTick();
				}
			};
		}.start();
	}
	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent ev) {
		Thread r = new Thread() {
			@SuppressWarnings("static-access")
			public void run() {
				if(CustomMessages.DEV_AUTO_RELOAD_CONFIG_SEC != 0)
					while(true){
						if(CustomMessages.DEV_AUTO_RELOAD_CONFIG_SEC != 0){
							try {
								Thread.sleep(TimeUnit.SECONDS.toMillis(CustomMessages.DEV_AUTO_RELOAD_CONFIG_SEC));
								CustomMessages.preInit();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								System.err.println("Error auto-reloading config: InterruptedException");
							}
						}
					}
			}
		};
		r.setDaemon(true);
		r.setPriority(Thread.MAX_PRIORITY);
		r.start();
		preServer = false;
		s.terminateEndpoints();
	}
	@Mod.EventHandler
	public void init(FMLInitializationEvent ev) {

		if(CustomMessages.DEV_DELAY_SERVER){
			try {
				TimeUnit.SECONDS.sleep(9999);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.err.println("Got interrupted while delaying server");
			}
		}
	}
	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent ev) {

	}
	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event){
		event.registerServerCommand(new ReloadCommand());
		if(CustomMessages.HELP_LIST.length != 0) event.registerServerCommand(new HelpCommand());
	}
	@Mod.EventHandler
	public void serverStarted(FMLServerStartedEvent ev) {
		serverStarted = true;
	}
}
