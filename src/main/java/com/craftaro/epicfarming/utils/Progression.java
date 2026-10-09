package com.craftaro.epicfarming.utils;

import com.craftaro.epicfarming.EpicFarming;
import com.craftaro.epicfarming.farming.Farm;
import com.craftaro.epicfarming.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Paliers de production : récompenses automatiques quand une ferme franchit un seuil.
 */
public class Progression {
    private Progression() {
    }

    public static class Milestone {
        public final long threshold;
        public final String type;
        public final String value;

        Milestone(long threshold, String type, String value) {
            this.threshold = threshold;
            this.type = type;
            this.value = value;
        }
    }

    public static List<Milestone> load() {
        List<Milestone> result = new ArrayList<>();
        for (String entry : Settings.MILESTONES.getStringList()) {
            String[] parts = entry.split(";", 3);
            if (parts.length != 3) {
                continue;
            }
            try {
                result.add(new Milestone(Long.parseLong(parts[0].trim()), parts[1].trim().toLowerCase(Locale.ROOT), parts[2].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        result.sort((a, b) -> Long.compare(a.threshold, b.threshold));
        return result;
    }

    /**
     * @return le prochain seuil à atteindre, ou -1 si tous sont atteints.
     */
    public static long nextThreshold(long total) {
        for (Milestone milestone : load()) {
            if (milestone.threshold > total) {
                return milestone.threshold;
            }
        }
        return -1;
    }

    /**
     * Appelé quand la production totale d'une ferme passe de {@code before} à {@code after}.
     */
    public static void check(Farm farm, long before, long after) {
        for (Milestone milestone : load()) {
            if (before < milestone.threshold && after >= milestone.threshold) {
                Bukkit.getScheduler().runTask(EpicFarming.getInstance(), () -> reward(farm, milestone));
            }
        }
    }

    private static void reward(Farm farm, Milestone milestone) {
        OfflinePlayer owner = farm.getPlacedBy() == null ? null : Bukkit.getOfflinePlayer(farm.getPlacedBy());

        if (milestone.type.equals("fuel")) {
            try {
                farm.addFuel(Long.parseLong(milestone.value));
            } catch (NumberFormatException ignored) {
            }
        } else if (milestone.type.equals("cmd") && owner != null && owner.getName() != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), milestone.value.replace("%player%", owner.getName()));
        }

        if (owner != null && owner.isOnline()) {
            Player player = owner.getPlayer();
            if (player != null) {
                EpicFarming.getInstance().getLocale().getMessage("event.milestone.reached")
                        .processPlaceholder("amount", String.format(Locale.FRANCE, "%,d", milestone.threshold))
                        .sendPrefixedMessage(player);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            }
        }
    }
}
