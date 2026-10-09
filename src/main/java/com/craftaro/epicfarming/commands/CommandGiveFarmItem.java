package com.craftaro.epicfarming.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.craftaro.epicfarming.EpicFarming;
import com.craftaro.epicfarming.farming.levels.Level;
import com.craftaro.epicfarming.farming.levels.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandGiveFarmItem extends AbstractCommand {
    private final EpicFarming plugin;

    public CommandGiveFarmItem(EpicFarming plugin) {
        super(CommandType.CONSOLE_OK, "givefarmitem");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if (args.length == 1) {
            return ReturnType.SYNTAX_ERROR;
        }

        // /efa givefarmitem [joueur] [niveau] [mining]
        boolean mining = args.length >= 3 && args[2].equalsIgnoreCase("mining");
        LevelManager manager = this.plugin.getLevelManager(mining);
        Level level = manager.getLowestLevel();

        Player player;
        if (args.length != 0 && Bukkit.getPlayer(args[0]) == null) {
            this.plugin.getLocale().newMessage("&cCe joueur n'existe pas ou n'est pas en ligne.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        } else if (args.length == 0) {
            if (!(sender instanceof Player)) {
                this.plugin.getLocale().newMessage("&cTu dois être un joueur pour te donner une ferme à toi-même.").sendPrefixedMessage(sender);
                return ReturnType.FAILURE;
            }
            player = (Player) sender;
        } else {
            player = Bukkit.getPlayer(args[0]);
        }

        if (args.length >= 2) {
            int requested;
            try {
                requested = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                return ReturnType.SYNTAX_ERROR;
            }

            if (!manager.isLevel(requested)) {
                this.plugin.getLocale().newMessage("&cNiveau invalide... Les niveaux valides sont : &4"
                        + manager.getLowestLevel().getLevel() + "-"
                        + manager.getHighestLevel().getLevel() + "&c.").sendPrefixedMessage(sender);
                return ReturnType.FAILURE;
            }
            level = manager.getLevel(requested);
        }

        player.getInventory().addItem(this.plugin.makeFarmItem(level));
        this.plugin.getLocale().getMessage(mining ? "command.give.successmining" : "command.give.success")
                .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        } else if (args.length == 2) {
            return this.plugin.getLevelManager().getLevels().keySet().stream()
                    .map(String::valueOf).collect(Collectors.toList());
        } else if (args.length == 3) {
            return Collections.singletonList("mining");
        }
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "epicfarming.give";
    }

    @Override
    public String getSyntax() {
        return "givefarmitem [joueur] <niveau> [mining]";
    }

    @Override
    public String getDescription() {
        return "Donne une ferme (cultures, ou minière avec 'mining') à un joueur.";
    }
}
