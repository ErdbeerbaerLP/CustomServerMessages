package de.erdbeerbaerlp.customServerMessages;

import com.mojang.logging.LogUtils;
import de.erdbeerbaerlp.customServerMessages.emulation.EmulatedServerConnectionListener;
import net.minecraft.ChatFormatting;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Mod(CustomMessageMod.MODID)
@EventBusSubscriber
public class CustomMessageMod {
    public static final String MODID = "customservermessages";
    public static boolean started = false;
    public static Logger LOGGER = LogUtils.getLogger();
    /**
     * File for storing the last time the server needed to start up
     */
    public static final File estimatedTimeFile = new File("./serverStartTime.txt");

    // Times for tracking server start time, set to 0 to prevent NPEs
    public static Instant serverLaunched = Instant.ofEpochMilli(0);
    public static Instant serverLaunchCompleted = Instant.ofEpochMilli(0);
    public static boolean launchTimeValid = true;
    /**
     * Gets an formatted time string for the estimated server start time
     *
     * @return Start time in format MM:SS
     */
    public static String getEstimatedStartTime() {
        try {
            if (!CustomMessageMod.estimatedTimeFile.exists()) {
                CustomMessageMod.estimatedTimeFile.createNewFile();
                final BufferedWriter w = new BufferedWriter(new FileWriter(CustomMessageMod.estimatedTimeFile));
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

    private static ArrayList<String> readFile() throws IOException {
        final BufferedReader r = new BufferedReader(new FileReader(estimatedTimeFile));
        final ArrayList<String> s = new ArrayList<>();
        r.lines().forEach(e -> {
            if (!e.isEmpty()) s.add(e);
        });
        r.close();
        return s;
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
        final long overworldTicks = ServerLifecycleHooks.getCurrentServer().overworld().getGameTime();
        final long ticksThisDay = overworldTicks % 24000;
        int hours = ((int) (ticksThisDay / 1000) + 6);
        if (hours >= 24)
            hours = hours - 24;
        int minutes = (int) ((ticksThisDay % 1000) * 3 / 50);
        String out = (hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes;
        if (colored) {
            out = (ServerLifecycleHooks.getCurrentServer().overworld().isDay() ? ChatFormatting.GREEN : ChatFormatting.RED) + out;
        }
        return out;
    }
    public CustomMessageMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ArrayList<String> playerTimeouts = new ArrayList<>();
    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent ev) {
        started = true;
        serverLaunchCompleted = Instant.now();
        if (launchTimeValid)
            try {
                if (!estimatedTimeFile.exists())
                    estimatedTimeFile.createNewFile();
                final ArrayList<String> s = readFile();
                if (s.size() > 4) s.clear();
                if (s.size() == 4) s.remove(0);
                s.add(ChronoUnit.MILLIS.between(serverLaunched, serverLaunchCompleted) + "");
                final BufferedWriter w = new BufferedWriter(new FileWriter(estimatedTimeFile));
                for (String l : s)
                    w.write(l + "\n");
                w.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private static EmulatedServerConnectionListener conn;
    static DedicatedServerProperties properties = null;

    public static void earlyStart() {
        Configuration.instance().loadConfig();
        final Path path = Paths.get("server.properties");
        final DedicatedServerSettings dedicatedserversettings = new DedicatedServerSettings(path);
        dedicatedserversettings.forceSave();
        properties = dedicatedserversettings.getProperties();
        conn = new EmulatedServerConnectionListener(properties);
        try {
            InetAddress inetaddr = null;
            if (!properties.serverIp.isEmpty())
                inetaddr = InetAddress.getByName(properties.serverIp);
            LOGGER.info("Starting Minecraft server on {}:{}", properties.serverIp.isEmpty()?"*":properties.serverIp , properties.serverPort);
            conn.startTcpServerListener(inetaddr, properties.serverPort);
        } catch (IOException ioexception) {
            LOGGER.warn("**** FAILED TO BIND TO PORT!");
            LOGGER.warn("The exception was: {}", (Object) ioexception.toString());
            LOGGER.warn("Perhaps a server is already running on that port?");
        }
    }

    public static void earlyStop() {
        conn.stop();
    }
}
