package com.craftaro.epicfarming.commands;

import com.craftaro.core.commands.AbstractCommand;
import com.craftaro.epicfarming.EpicFarming;
import com.craftaro.epicfarming.farming.Farm;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Classement des joueurs selon la production totale de leurs fermes.
 */
public class CommandTop extends AbstractCommand {
    private final EpicFarming plugin;

    public CommandTop(EpicFarming plugin) {
        super(CommandType.CONSOLE_OK, "top");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        Map<UUID, Long> totals = new HashMap<>();
        for (Farm farm : this.plugin.getFarmManager().getFarms().values()) {
            if (farm.getPlacedBy() != null) {
                totals.merge(farm.getPlacedBy(), farm.getTotalProduced(), Long::sum);
            }
        }

        List<Map.Entry<UUID, Long>> top = totals.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .collect(Collectors.toList());

        this.plugin.getLocale().newMessage("&6&lTop producteurs &7(toutes fermes confondues)").sendPrefixedMessage(sender);
        int rank = 1;
        for (Map.Entry<UUID, Long> entry : top) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            this.plugin.getLocale().newMessage("&e#" + rank + " &f" + (name == null ? "?" : name)
                    + " &8- &a" + String.format(Locale.FRANCE, "%,d", entry.getValue()) + " &7items").sendPrefixedMessage(sender);
            rank++;
        }
        if (top.isEmpty()) {
            this.plugin.getLocale().newMessage("&7Aucune ferme n'a encore produit.").sendPrefixedMessage(sender);
        }
        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "epicfarming.top";
    }

    @Override
    public String getSyntax() {
        return "top";
    }

    @Override
    public String getDescription() {
        return "Affiche le classement des meilleurs producteurs.";
    }
}
