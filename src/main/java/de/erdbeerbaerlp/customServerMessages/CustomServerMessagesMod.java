package de.erdbeerbaerlp.customServerMessages;

import java.util.concurrent.TimeUnit;

import com.google.common.eventbus.Subscribe;

import net.minecraft.network.NetworkSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
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
	protected static boolean serverStarted;
	//  Keeping that backup here, just in case
	@Mod.EventHandler
	public void prePreInit(FMLConstructionEvent ev) {
		CustomMessages.preInit();
		final NetworkSystem s = new CustomNetworkSystem();
		ObfuscationReflectionHelper.setPrivateValue(MinecraftServer.class, FMLCommonHandler.instance().getMinecraftServerInstance(), s, "networkSystem");

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
								this.sleep(TimeUnit.SECONDS.toMillis(CustomMessages.DEV_AUTO_RELOAD_CONFIG_SEC));
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
	}
	@Mod.EventHandler
	public void init(FMLInitializationEvent ev) {
		try {
			Thread.sleep(TimeUnit.SECONDS.toMillis(10));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent ev) {

	}
	@Subscribe
	public void onServerStarting(FMLServerStartingEvent event){
		event.registerServerCommand(new ReloadCommand());
		if(CustomMessages.HELP_LIST.length != 0) event.registerServerCommand(new HelpCommand());
	}
	@Mod.EventHandler
	public void serverStarted(FMLServerStartedEvent ev) {
		serverStarted = true;
	}
}
