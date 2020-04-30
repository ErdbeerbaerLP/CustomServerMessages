package de.erdbeerbaerlp.customServerMessages;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HelpCommand extends CommandBase {
    private final Random rand = new Random();

    /**
     * Gets the name of the command
     */
    public String getName() {
        return "help";
    }

    /**
     * Return the required permission level for this command.
     */
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
    /**
     * Gets the usage string for the command.
     */
    public String getUsage(ICommandSender sender)
    {
        return "commands.help.usage";
    }

    /**
     * Get a list of aliases for this command. <b>Never return null!</b>
     */
    public List<String> getAliases()
    {
        return Arrays.<String>asList("?");
    }

    /**
     * Callback for when the command is executed
     */
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
    	try {
    		sender.sendMessage(new TextComponentString((CustomMessages.HELP_LIST[this.rand.nextInt(CustomMessages.HELP_LIST.length) % CustomMessages.HELP_LIST.length]).replace("\\n", "\n")));
    	}catch(Exception e) {
    		sender.sendMessage(new TextComponentString("Missingno.").setStyle(new Style().setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new TextComponentString("Could not find any help messages :/")))));
    	}
    	}
    /**
     * Get a list of options for when the user presses the TAB key
     */
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
            return Collections.<String>emptyList();
    }
}