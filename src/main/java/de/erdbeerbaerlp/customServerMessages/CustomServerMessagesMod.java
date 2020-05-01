package de.erdbeerbaerlp.customServerMessages;

import net.minecraft.network.NetworkSystem;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Mod(modid = CustomServerMessagesMod.MODID, version = CustomServerMessagesMod.VERSION, name = CustomServerMessagesMod.NAME, serverSideOnly = true, acceptableRemoteVersions = "*")
@EventBusSubscriber
public class CustomServerMessagesMod {
	public static final String MODID = "servermsgs";
	public static final String NAME = "Custom Server Messages";
	public static final String VERSION = "3.0.0";
	/**
	 * File for storing the last time the server needed to start up
	 */
	public static final File estimatedTimeFile = new File("./serverStartTime.txt");
	public static boolean serverStarted = false;
	/**
	 * Setting to false will terminate the preServerThread
	 */
	public static boolean preServer = true;
	/**
	 * Network System stored for restoring to allow player joining
	 */
	public static NetworkSystem vanillaSystem = null;
	/**
	 * Thread for allowing network messages early
	 */
	public static PreServerThread preServerThread;
	// Times for tracking server start time, set to 0 to prevent NPEs
	public static Instant serverLaunched = Instant.ofEpochMilli(0);
	public static Instant serverLaunchCompleted = Instant.ofEpochMilli(0);
	public static boolean launchTimeValid = true;
	/**
	 * List of timed out player names
	 */
	public static ArrayList<String> playersTimedOut = new ArrayList<>();

	/**
	 * Gets an formatted time string for the estimated server start time
	 *
	 * @return Start time in format MM:SS
	 */
	public static String getEstimatedStartTime() {
		try {
			if (!CustomServerMessagesMod.estimatedTimeFile.exists()) {
				CustomServerMessagesMod.estimatedTimeFile.createNewFile();
				final BufferedWriter w = new BufferedWriter(new FileWriter(CustomServerMessagesMod.estimatedTimeFile));
				w.write("0");
				w.close();
			}
			final BufferedReader r = new BufferedReader(new FileReader(estimatedTimeFile));
			final long millis = Long.parseLong(r.readLine());
			r.close();
			final Instant now = Instant.now();
			final Instant instant = serverLaunched.plusMillis(millis);
			final Duration d = Duration.between(now, instant);
			if (d.isNegative()) return "00:00";
			int seconds = (int) d.getSeconds();
			int minutes = 0;
			while (seconds >= 60) {
				seconds = seconds - 60;
				minutes++;
			}
			return (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
		}
		return "??:??";
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new ReloadCommand());
		if (CustomMessages.HELP_LIST.length != 0) event.registerServerCommand(new HelpCommand());
	}

	public static String getOverworldTime(boolean colored) {
		final long overworldTicks = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0).getWorldTime();
		final long ticksThisDay = overworldTicks % 24000;
		int hours = ((int) (ticksThisDay / 1000) + 6);
		if (hours >= 24)
			hours = hours - 24;
		int minutes = (int) ((ticksThisDay % 1000) * 3 / 50);
		String out = (hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes;
		if (colored) {
			out = (FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0).isDaytime() ? TextFormatting.GREEN : TextFormatting.RED) + out;
		}
		return out;
	}

	@Mod.EventHandler
	public void serverStarted(FMLServerStartedEvent ev) {
		serverStarted = true;
		FMLCommonHandler.instance().getMinecraftServerInstance().networkSystem = vanillaSystem;
		serverLaunchCompleted = Instant.now();
		if (launchTimeValid)
			try {
				if (!CustomServerMessagesMod.estimatedTimeFile.exists())
					CustomServerMessagesMod.estimatedTimeFile.createNewFile();
				final BufferedWriter w = new BufferedWriter(new FileWriter(CustomServerMessagesMod.estimatedTimeFile));
				w.write((ChronoUnit.MILLIS.between(serverLaunched, serverLaunchCompleted)) + "");
				w.close();
			} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class PreServerThread extends Thread {
		private NetworkSystem sys;

		public PreServerThread(NetworkSystem sys) {
			this.sys = sys;
			setName("Early Network Handler");
			setDaemon(true);
			setPriority(MAX_PRIORITY);
		}

		public void run() {
			while (preServer) {
				sys.networkTick();
			}
		}
	}
}
