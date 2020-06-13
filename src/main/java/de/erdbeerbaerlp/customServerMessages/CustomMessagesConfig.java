package de.erdbeerbaerlp.customServerMessages;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlComment;
import com.moandjiezana.toml.TomlIgnore;
import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class CustomMessagesConfig {
    private static final Random r = new Random();
    private static final File configFile = new File("./config/CustomMessages.toml");
    @TomlIgnore
    private static CustomMessagesConfig INSTANCE;

    static {
        INSTANCE = new CustomMessagesConfig();
        INSTANCE.loadConfig();
    }

    @TomlComment("Generic options")
    public General general = new General();
    @TomlComment({"These messages are visible to all players",
            "",
            "Any messages supports color codes",
            "More info about color codes here: http://bit.ly/mcformatting",
            "Use \\n for an new line, \u00a7k for unreadable text, \u00a7l for bold text, \u00a7m for strikethrough,",
            "\u00a7n for underlined text, \u00a7o for italics and \u00a7r to reset all formatting"})
    public Messages messages = new Messages();
    @TomlComment({"Useful to debug the mod",
            "These should be disabled after done using it"})
    public Dev dev = new Dev();


    public static CustomMessagesConfig instance() {
        return INSTANCE;
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            INSTANCE = new CustomMessagesConfig();
            INSTANCE.saveConfig();
            return;
        }
        INSTANCE = new Toml().read(configFile).to(CustomMessagesConfig.class);
        INSTANCE.saveConfig(); //Re-write the config so new values get added after updates
    }

    public void saveConfig() {
        try {
            if (!configFile.exists()) configFile.createNewFile();
            final TomlWriter w = new TomlWriter.Builder()
                    .indentValuesBy(2)
                    .indentTablesBy(4)
                    .padArrayDelimitersBy(3)
                    .build();
            w.write(this, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRandomMOTD() {
        return messages.customMOTDs[r.nextInt(messages.customMOTDs.length)].replace("\\n", "\n");
    }

    public static class General {
        @TomlComment({"Enable override of version while the Server is still starting?",
                "true: Shows the custom message and enables hover message",
                "false: Shows -1/-1 players online"})
        public boolean useStartVersion = true;
        @TomlComment("Enable custom MOTD handling")
        public boolean enableCustomMOTD = true;
        @TomlComment({"This will show custom text instead of the playercount.",
                "WARNING: This will also show, that the server is outdated, but players can still join!"})
        public boolean useCustomMOTDVersion = false;
        @TomlComment("Should the server log \"Disconnecting Player: Server is starting\"?")
        public boolean logStartDisconnects = false;
        @TomlComment({"Path to the server icon",
                "Image MUST be an PNG and the size MUST be 64x64 pixels"})
        public String serverIconPath = "./server-icon.png";
        @TomlComment({"Path to the server icon shown while starting",
                "Will instead use the normal icon when file does not exist",
                "Image MUST be an PNG and the size MUST be 64x64 pixels"})
        public String startServerIconPath = "./server-starting-icon.png";
    }

    public static class Messages {
        @TomlComment({"The Message that will be displayed instead of -1/-1 Players",
                "Supports estimated server start time using %time%"})
        public String startPlayerListText = "\u00A74Starting...";
        @TomlComment("Text that will be shown when you hover over the message you set in StartVersion")
        public String startPlayerListTextHover = "\u00a74Server is starting\n\u00a76Please wait until the server has started completely\n\u00a7cElse you will \u00a74not\u00a7c be able to join\n\u00a7eIf you think this is an error contact the server team\n\n\u00a7a\u00a7nLinks:\n\u00a7bSupport website: \u00a71http://example.com\n\u00a75Discord server: \u00a71http://discord.gg/example";
        @TomlComment({"Writes an custom MOTD while server is starting",
                "Only two lines will be displayed!",
                "Supports estimated server start time using %time%"})
        public String serverStartingMOTD = "\u00A74This Server is still Starting\n\u00A7cPlease Wait...  Estimated: %time%";
        @TomlComment({"Kick message that will be shown to players who want to connect to the starting server",
                "",
                "Vanilla: Server is still starting. Please wait before reconnecting",
                "Default: \u00a74This server is currently starting\n\u00a7cPlease wait..."})
        public String serverStartingKickMessage = "\u00a74This server is currently starting\n\u00a7cPlease wait...";
        @TomlComment({"The player join message",
                "",
                "Vanilla: \u00a7e%player% joined the game"})
        public String joinMessage = "\u00A76[\u00A72+\u00A76]\u00A77%player%";
        @TomlComment({"The player leave message",
                "",
                "Vanilla: \u00a7e%player% left the game"})
        public String leaveMessage = "\u00A76[\u00A7c-\u00A76]\u00A77%player%";
        @TomlComment({"The player leave message when the player times out",
                "",
                "Vanilla: \u00a7e%player% left the game"})
        public String timeoutLeaveMessage = "\u00A76[\u00A7c-\u00A76]\u00A77%player% timed out";
        @TomlComment({"The message you get when the server stops",
                "",
                "Vanilla: Server closed",
                "Default: \u00A7cServer has been stopped or is restarting\n\u00A77Try joining again later"})
        public String serverStoppedMessage = "\u00A7cServer has been stopped or is restarting\n\u00A77Try joining again later";
        @TomlComment({"Modify the motd as you like",
                "Multiple Lines in this config will randomize the MOTDs",
                "", "Supports 2 lines (Use \\n for new line)",
                "Placeholders:",
                "%online% - Online Players",
                "%max% - Maximum player count",
                "%time% - Time in the Overworld",
                "%time-colored% - Time in the Overworld, Green while day, Red while night"})
        public String[] customMOTDs = new String[]{"\u00A7a%online%\u00A76/\u00A7c%max%\u00A76 players playing on YOURSERVER\n\u00A73Join them now", "\u00A75Join our discord server:\n\u00A75discord.gg/example", "Another random MOTD\nReplace them in config/CustomMessages.cfg"};
        @TomlComment({"Text used for the custom version",
                "",
                "Placeholders:",
                "%online% - Online player count",
                "%max% - Maximum player count"})
        public String customMOTDPlayerListText = "\u00A7a%online%\u00A76/\u00A7c%max%\u00A76 online!";
        @TomlComment({"The message you see when hovering over the player count in the server list",
                "",
                "Placeholders:",
                "%online% - Online Players",
                "%max% - Maximum player count",
                "%playerlist% - A list of players like vanilla would display",
                "%gamemode% - The default gamemode of the server",
                "%time% - Time in the Overworld",
                "%time-colored% - Time in the Overworld, Green while day, Red while night"})
        public String customMOTDPlayerListHover = "\u00A76Welcome to YOURSERVER!\n\u00A73There are \u00A7a%online%\u00A73 players online.\nWorld time: %time-colored%\n\n\u00A7aOnline:\n\u00A72%playerlist%\n\u00A79\u00A7lHave Fun!";
        @TomlComment({"Message shown to players joining with newer Minecraft versions",
                "Vanilla: Outdated server! I'm still on  1.15.2"})
        public String outdatedServerKick = "Coming from the future? We are still using Minecraft 1.15.2";
        @TomlComment({"Message shown to players joining with older Minecraft versions",
                "Vanilla: Outdated client! Please use 1.15.2"})
        public String outdatedClientKick = "Your client is too old. Use 1.15.2";
        @TomlComment({"Kick message shown to spamming players",
                "Does not modify other mods's messages, only the vanilla one"})
        public String spamKick = "Please do not spam!";
        @TomlComment({"Idle-Timeout kick message",
                "Vanilla: You have been idle for too long!"})
        public String idleTimeoutKick = "\u00a7cYou have been AFK for too long!";
    }

    public static class Dev {
        @TomlComment({"The amount of seconds till the config should be automatically reloaded.",
                "Could cause lag",
                "Will start as soon as the config loaded once",
                "Can be used to edit the starting MOTD and version while server is starting",
                "Set to 0 to disable"})
        public int autoReloadConfig = 0;
        @TomlComment({"Will delay the server start by 9999 seconds",
                "Can be used with ReloadConfigAfter to modify your StartMOTD and Version"})
        public boolean delayServerStart = false;
    }

}
