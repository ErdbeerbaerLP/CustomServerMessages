package de.erdbeerbaerlp.customServerMessages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Loader;

public class CustomMessages {

	private static Configuration config = null;
	//CATEGORIES
	public static final String CATEGORY_MESSAGES = "Messages";
	public static final String CATEGORY_LOG = "Log";
	public static final String CATEGORY_DEV = "Developer options";
	
	
	//MESSAGES
	public static String START_VERSION;
	public static boolean USE_VERSION;
	public static String START_MOTD;
	public static String START_KICK_MSG;
	public static String JOIN_MSG;
	public static String LEAVE_MSG;
	public static String LEAVE_MSG_TIMEOUT;
	public static String STOP_MSG;
	public static String START_VERSION_HOVER;
	public static String[] HELP_LIST;
	
	//LOG
	public static boolean LOG_START_DISCONNECT;
	public static boolean LOG_CONFIG_RELOAD = true;
	
	
	//DEV
	public static int DEV_AUTO_RELOAD_CONFIG_SEC;
	public static boolean DEV_DELAY_SERVER;
	
	
	
	
	
	
	public static void preInit(){
		File configFile = new File(Loader.instance().getConfigDir(), "/CustomMessages.cfg");
		config = new Configuration(configFile);
		syncFromFiles();
		
	}
	
	public static Configuration getConfig(){
		return config;
	}
	
	public static void syncFromFiles(){
		syncConfig(true, true);
	}
	
	public static void syncFromGui(){
		syncConfig(false, true);
	}
	
	public static void syncFromFields(){
		syncConfig(false, false);
	}
	
	private static void syncConfig(boolean loadFromConfigFile, boolean readFieldsFromConfig){
		if(loadFromConfigFile)
			config.load();
		if(config.hasCategory("ServerMessages")){
			System.err.println("Converting config file from version 1.0.2 to "+CustomServerMessagesMod.VERSION+"...");
			String old = "ServerMessages";
			config.moveProperty(old, "ServerStart MOTD", CATEGORY_MESSAGES);
			config.moveProperty(old, "UseStartVersion", CATEGORY_MESSAGES);
			config.moveProperty(old, "StartVersion", CATEGORY_MESSAGES);
			config.moveProperty(old, "serverStartingKick", CATEGORY_MESSAGES);
			config.moveProperty(old, "Join Message", CATEGORY_MESSAGES);
			config.moveProperty(old, "Leave Message", CATEGORY_MESSAGES);
			config.moveProperty(old, "Stop Message", CATEGORY_MESSAGES);
			config.removeCategory(config.getCategory(old));
			old = null;
			System.out.println("Conversion done!");
		}
		config.setCategoryComment(CATEGORY_MESSAGES, "Theese messages should be visible to all players\n\nAny messages supports color codes\nMore infos about color codes here: http://bit.ly/mcformatting\nUse \\n for an new line, \u00a7k for unreadable text, \u00a7l for bold text, \u00a7m for strikethrough,\n\u00a7n for underlined text, \u00a7o for italics and \u00a7r to reset all formatting");
		config.setCategoryComment(CATEGORY_LOG, "Stuff that will be logged, enable/disable or modify them here");
		config.setCategoryComment(CATEGORY_DEV, "Useful to debug the mod\nThese should be set to disabled after done using it");
		//VISIBLE MESSAGES
		Property serverStartingMotd = config.get(CATEGORY_MESSAGES, "ServerStart MOTD", "\u00A74This Server is still Starting\\n\u00A7cPlease Wait...");
		serverStartingMotd.setComment("Writes an custom MOTD while server is starting\nOnly two lines will be displayed!");
		
		Property useStartVersion = config.get(CATEGORY_MESSAGES, "UseStartVersion", true);
		useStartVersion.setComment("Enable override of version?\ntrue: Shows the custom message and enables hover message\nfalse: Shows -1/-1 players online -> Hover message does not work here");
		
		Property startVersion = config.get(CATEGORY_MESSAGES, "StartVersion", "\u00A74Starting...");
		startVersion.setComment("The Message that will be displayed instead of -1/-1 Players");
		
		Property startVersionHover = config.get(CATEGORY_MESSAGES, "StartVersion Hover", "\u00a74Server is starting\\n\u00a76Please wait \u00a72until the server has started completely\\n\u00a7cElse you will \u00a74not\u00a7c be able to join\\n\u00a7eIf you think this is an error contact the server team\\n\\n\u00a7a\u00a7nLinks:\\n\u00a7bSupport website: \u00a71http://example.com\\n\u00a75Discord server: \u00a71http://discord.gg/example");
		startVersionHover.setComment("Text that will be shows when you hover over the message you set in StartVersion");
		
		Property serverStartingKick = config.get(CATEGORY_MESSAGES, "serverStartingKick", "\u00a74This server is currently starting\\n\u00a7cPlease wait...");
		serverStartingKick.setComment("Kick message that will be shown \nto players who want to connect to the starting server\n\nVanilla: Server is still starting. Please wait before reconnecting\nDefault: "+serverStartingKick.getDefault());
		
		Property joinMsg = config.get(CATEGORY_MESSAGES, "Join Message", "\u00A76[\u00A72+\u00A76]\u00A77%player%");
		joinMsg.setComment("Replaces \"PLAYER joined the game\"\n\nVanilla: \u00a7e%player% joined the game");
		
		Property leaveMsg = config.get(CATEGORY_MESSAGES, "Leave Message", "\u00A76[\u00A7c-\u00A76]\u00A77%player%");
		leaveMsg.setComment("Replaces \"PLAYER left the game\"\n\nVanilla: \u00a7e%player% left the game");
		
		Property leaveTimeoutMsg = config.get(CATEGORY_MESSAGES, "Timeout Message", "\u00A76[\u00A7c-\u00A76]\u00A77%player% timed out");
		leaveTimeoutMsg.setComment("Replaces \"PLAYER left the game\" when player timed out\n\nVanilla: \u00a7e%player% left the game");
		
		Property stopMsg = config.get(CATEGORY_MESSAGES,"Stop Message", "\u00A7cServer has been stopped or is restarting\\n\u00A77Try joining again later");
		stopMsg.setComment("The message you get when the server stops\n\nVanilla: Server closed\nDefault: "+stopMsg.getDefault());
		
		Property helpMsg = config.get(CATEGORY_MESSAGES, "Help Messages", new String[] {});
		helpMsg.setComment("A list of custom help messages to show instead of the vanilla /help command\nWhen there are multiple they get randomized\n\nLeave empty to disable\nDoes NOT work with /messagereload");
		
		
		
		
		
		
		//LOG MESSAGES

		Property logStartingDisconnect = config.get(CATEGORY_LOG, "Log Starting Disconnect", false);
		logStartingDisconnect.setComment("Should the server log \"Disconnecting Player: Server is starting\"?");
		
		Property logConfigReload = config.get(CATEGORY_LOG, "Log Config Reloads", false);
		logConfigReload.setComment("Should server log an config reload?");
		
		
		
		
		//Developer options
		Property reloadConfigAfter = config.get(CATEGORY_DEV, "ReloadCfgAfter", 0);
		reloadConfigAfter.setComment("The amount of seconds till the config should be automatically reloaded.\nCould cause lag\nWill start as soon as the config loaded once\nCan be used to edit the starting MOTD and version while server is starting\nSet to 0 to disable");
		
		Property delayServerStart = config.get(CATEGORY_DEV, "DelayServerStart", false);
		delayServerStart.setComment("Will delay the server start by 9999 seconds\nCan be used with ReloadConfigAfter to modify your StartMOTD and Version");
		
		
		
		//DEV
		List<String> orderDev = new ArrayList<String>();
		orderDev.add(reloadConfigAfter.getName());
		orderDev.add(delayServerStart.getName());
		config.setCategoryPropertyOrder(CATEGORY_DEV, orderDev);
		
		
		
		//MESSAGES
		List<String> propOrderMsgs = new ArrayList<String>();
		propOrderMsgs.add(serverStartingMotd.getName());
		propOrderMsgs.add(serverStartingKick.getName());
		propOrderMsgs.add(useStartVersion.getName());
		propOrderMsgs.add(startVersion.getName());
		propOrderMsgs.add(startVersionHover.getName());
		propOrderMsgs.add(joinMsg.getName());
		propOrderMsgs.add(leaveMsg.getName());
		propOrderMsgs.add(leaveTimeoutMsg.getName());
		propOrderMsgs.add(stopMsg.getName());
		propOrderMsgs.add(helpMsg.getName());
		config.setCategoryPropertyOrder(CATEGORY_MESSAGES, propOrderMsgs);
		
		
		
		
		//LOG
		List<String> orderLogs = new ArrayList<String>();
		orderLogs.add(logStartingDisconnect.getName());
		orderLogs.add(logConfigReload.getName());
		config.setCategoryPropertyOrder(CATEGORY_LOG, orderLogs);
		

		
		if(readFieldsFromConfig){
			
			//MESSAGES
			START_KICK_MSG = serverStartingKick.getString().replace("\\n", "\n");
			START_MOTD = serverStartingMotd.getString().replace("\\n", "\n");
			USE_VERSION = useStartVersion.getBoolean();
			START_VERSION = startVersion.getString();
			JOIN_MSG = joinMsg.getString().replace("\\n", "\n");
			LEAVE_MSG = leaveMsg.getString().replace("\\n", "\n");
			LEAVE_MSG_TIMEOUT = leaveTimeoutMsg.getString().replace("\\n", "\n");
			STOP_MSG = stopMsg.getString().replace("\\n", "\n");
			START_VERSION_HOVER = startVersionHover.getString().replace("\\n", "\n");
			HELP_LIST = helpMsg.getStringList();
			
			//LOG
			LOG_START_DISCONNECT = logStartingDisconnect.getBoolean();
			LOG_CONFIG_RELOAD = logConfigReload.getBoolean();
			
			//DEV
			DEV_AUTO_RELOAD_CONFIG_SEC = reloadConfigAfter.getInt();
			DEV_DELAY_SERVER = delayServerStart.getBoolean();
		}
		
		if(config.hasChanged())
			config.save();
	}
	
}
