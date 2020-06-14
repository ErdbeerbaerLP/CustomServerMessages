package de.erdbeerbaerlp.customServerMessages;

import net.minecraft.network.NetworkSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Mod(CustomServerMessagesMod.MODID)
@EventBusSubscriber
public class CustomServerMessagesMod {
    public static final String MODID = "servermsgs";
    public static final String NAME = "Custom Server Messages";
    public static final String VERSION = "4.0.1";
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
    public static EarlyServerThread earlyNetworkSystem;
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
			final ArrayList<String> s = readFile();
			final ArrayList<Long> l = new ArrayList<>();
			for (String st : s)
				l.add(Long.parseLong(st));
			final long millis = average(l.toArray(new Long[0]));
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

    public CustomServerMessagesMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static long average(Long... longs) {
        final long len = longs.length + 1;
        long sum = 0;
        for (long l : longs) {
            sum = sum + l;
        }
		return sum / len;
	}

	public static String getOverworldTime(boolean colored) {
        final long overworldTicks = ServerLifecycleHooks.getCurrentServer().getWorld(DimensionType.OVERWORLD).getGameTime();
        final long ticksThisDay = overworldTicks % 24000;
        int hours = ((int) (ticksThisDay / 1000) + 6);
        if (hours >= 24)
            hours = hours - 24;
        int minutes = (int) ((ticksThisDay % 1000) * 3 / 50);
        String out = (hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes;
        if (colored) {
            out = (ServerLifecycleHooks.getCurrentServer().getWorld(DimensionType.OVERWORLD).isDaytime() ? TextFormatting.GREEN : TextFormatting.RED) + out;
        }
        return out;
    }

    private static ArrayList<String> readFile() throws IOException {
        final BufferedReader r = new BufferedReader(new FileReader(estimatedTimeFile));
        final ArrayList<String> s = new ArrayList<>();
        r.lines().forEach(e -> {
            if (!e.isEmpty()) s.add(e);
        });
        r.close();
        return s;
    }

    @SubscribeEvent
    public void serverStarted(FMLServerStartedEvent ev) {
        serverStarted = true;
        ObfuscationReflectionHelper.setPrivateValue(MinecraftServer.class, ServerLifecycleHooks.getCurrentServer(), vanillaSystem, "field_147144_o");
        serverLaunchCompleted = Instant.now();
        if (launchTimeValid)
            try {
                if (!CustomServerMessagesMod.estimatedTimeFile.exists())
                    CustomServerMessagesMod.estimatedTimeFile.createNewFile();
                final ArrayList<String> s = readFile();
                if (s.size() > 4) s.clear();
                if (s.size() == 4) s.remove(0);
                s.add(ChronoUnit.MILLIS.between(serverLaunched, serverLaunchCompleted) + "");
                final BufferedWriter w = new BufferedWriter(new FileWriter(CustomServerMessagesMod.estimatedTimeFile));
                for (String l : s)
                    w.write(l + "\n");
                w.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static class EarlyServerThread extends Thread {
        private NetworkSystem sys;

        public EarlyServerThread(NetworkSystem sys) {
            this.sys = sys;
            setName("Early Network Handler");
            setDaemon(true);
            setPriority(MAX_PRIORITY);
        }

        public void run() {
            while (preServer) {
				sys.tick();
			}
		}
	}
}
