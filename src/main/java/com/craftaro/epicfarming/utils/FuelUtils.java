package com.craftaro.epicfarming.utils;

import com.craftaro.epicfarming.settings.Settings;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Outils pour le carburant des fermes.
 */
public class FuelUtils {
    private FuelUtils() {
    }

    /**
     * @return map matériau -> secondes de carburant apportées par 1 item (lu dans la config).
     */
    public static Map<Material, Integer> loadFuelItems() {
        Map<Material, Integer> result = new HashMap<>();
        for (String entry : Settings.FUEL_ITEMS.getStringList()) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                continue;
            }
            Material material = Material.matchMaterial(parts[0].trim());
            if (material == null) {
                continue;
            }
            try {
                result.put(material, Math.max(1, Integer.parseInt(parts[1].trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    /**
     * Formate un temps de carburant : en heures (ex. "12,5 h") ou en minutes sous une heure.
     */
    public static String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0 min";
        }
        if (seconds < 3600) {
            return Math.max(1, seconds / 60) + " min";
        }
        return String.format(Locale.FRANCE, "%.1f h", seconds / 3600.0);
    }
}
