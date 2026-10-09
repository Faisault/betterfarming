package com.craftaro.epicfarming.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.craftaro.epicfarming.EpicFarming;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandReload extends AbstractCommand {
    private final EpicFarming plugin;

    public CommandReload(EpicFarming plugin) {
        super(CommandType.CONSOLE_OK, "reload");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        this.plugin.reloadConfig();
        this.plugin.getLocale().getMessage("&7Configuration et fichiers de langue rechargés.").sendPrefixedMessage(sender);
        return AbstractCommand.ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "epicfarming.reload";
    }

    @Override
    public String getSyntax() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recharge la configuration et les fichiers de langue.";
    }
}
