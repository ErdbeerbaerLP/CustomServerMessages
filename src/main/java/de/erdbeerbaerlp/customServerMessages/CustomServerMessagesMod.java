package de.erdbeerbaerlp.customServerMessages;

import java.util.concurrent.TimeUnit;

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

@Mod(modid=CustomServerMessagesMod.MODID, version = CustomServerMessagesMod.VERSION, name = CustomServerMessagesMod.NAME, serverSideOnly = true, acceptableRemoteVersions = "*")
@EventBusSubscriber
public class CustomServerMessagesMod {
	public static final String MODID = "servermsgs";
	public static final String NAME = "Custom Server Messages";
	public static final String VERSION = "2.0.0";
	protected static boolean serverStarted;
//  Keeping that backup here, just in case
	private NetworkSystem defSys;
	@Mod.EventHandler
	public void prePreInit(FMLConstructionEvent ev) {
		CustomMessages.preInit();
		defSys = ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, FMLCommonHandler.instance().getMinecraftServerInstance(), "networkSystem");
		
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
						}else break;
					}
			}
		};
		r.setDaemon(true);
		r.setPriority(Thread.MAX_PRIORITY);
		r.start();
//		new Thread() {
//			public void run() {
//				while(!serverStarted) {
//					try {
//						final ServerStatusResponse statusResp = FMLCommonHandler.instance().getMinecraftServerInstance().getServerStatusResponse();
//						Players statusPlayers = new Players(-1, -1);
//						GameProfile[] playersIn = new GameProfile[]{new GameProfile(UUID.randomUUID(), CustomMessages.START_VERSION_HOVER)};
//						statusPlayers.setPlayers(playersIn);
//						statusResp.setVersion(new Version(CustomMessages.USE_VERSION?CustomMessages.START_VERSION:MinecraftForge.MC_VERSION, CustomMessages.USE_VERSION?Integer.MAX_VALUE:version));
//						statusResp.setPlayers(statusPlayers);
//						statusResp.setServerDescription(new TextComponentString(CustomMessages.START_MOTD));
//						((DedicatedServer)FMLServerHandler.instance().getServer()).allowPlayerLogins = true;
//					}catch (Exception e) {}
//				}
//			};
//		}.start();
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
	@Mod.EventHandler
	public void serverStarted(FMLServerStartedEvent ev) {
		serverStarted = true;
//		ObfuscationReflectionHelper.setPrivateValue(MinecraftServer.class, FMLCommonHandler.instance().getMinecraftServerInstance(), defSys, "networkSystem");
	}
}
