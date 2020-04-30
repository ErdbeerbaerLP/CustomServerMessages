package de.erdbeerbaerlp.customServerMessages;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements ICommand {

    public ReloadCommand() {

    }

    @Override
    public int compareTo(ICommand arg0) {
        return 0;
    }

	@Override
	public String getName() {
		return "messagereload";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/messagereload";
	}

	@Override
	public List<String> getAliases() {
		return new ArrayList<String>();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		CustomMessages.preInit();
		sender.sendMessage(new TextComponentString("\u00A72Reloaded Message config"));
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return sender.canUseCommand(4, "op");
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
			BlockPos targetPos) {
		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}

}
