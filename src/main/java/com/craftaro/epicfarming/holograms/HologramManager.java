package com.craftaro.epicfarming.holograms;

import com.craftaro.epicfarming.EpicFarming;
import com.craftaro.epicfarming.farming.Farm;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hologrammes multi-lignes (un ArmorStand invisible par ligne) affichés au-dessus des fermes.
 * Aucune dépendance externe. Les stands ne sont pas persistants : ils disparaissent avec le serveur.
 */
public class HologramManager {
    public static final String TAG = "betterfarming_holo";
    private static final double LINE_SPACING = 0.26;

    private final Map<UUID, List<ArmorStand>> stands = new HashMap<>();
    private final Map<UUID, List<String>> lastLines = new HashMap<>();

    /**
     * À appeler uniquement depuis le thread principal. {@code lines} va du haut vers le bas.
     */
    public void update(Farm farm, List<String> lines) {
        UUID id = farm.getFarmUUID();

        if (lines == null || lines.isEmpty()) {
            remove(farm);
            return;
        }
        Location base = farm.getLocation();
        if (base.getWorld() == null || !farm.isInLoadedChunk()) {
            return;
        }

        List<ArmorStand> current = this.stands.get(id);
        boolean rebuild = current == null || current.size() != lines.size();
        if (!rebuild) {
            for (ArmorStand stand : current) {
                if (!stand.isValid()) {
                    rebuild = true;
                    break;
                }
            }
        }

        if (rebuild) {
            remove(farm);
            current = new ArrayList<>();
            int n = lines.size();
            for (int i = 0; i < n; i++) {
                Location loc = base.clone().add(0.5, 1.0 + LINE_SPACING * (n - 1 - i), 0.5);
                ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class, as -> {
                    as.setVisible(false);
                    as.setMarker(true);
                    as.setGravity(false);
                    as.setInvulnerable(true);
                    as.setSmall(true);
                    as.setCustomNameVisible(true);
                    as.setPersistent(false);
                    as.addScoreboardTag(TAG);
                });
                current.add(stand);
            }
            this.stands.put(id, current);
            this.lastLines.remove(id);
        }

        List<String> previous = this.lastLines.get(id);
        for (int i = 0; i < lines.size(); i++) {
            if (previous == null || !lines.get(i).equals(previous.get(i))) {
                current.get(i).setCustomName(lines.get(i));
            }
        }
        this.lastLines.put(id, new ArrayList<>(lines));
    }

    public void remove(Farm farm) {
        UUID id = farm.getFarmUUID();
        List<ArmorStand> list = this.stands.remove(id);
        this.lastLines.remove(id);
        if (list != null) {
            for (ArmorStand stand : list) {
                stand.remove();
            }
        }
    }

    public void removeAll() {
        for (List<ArmorStand> list : this.stands.values()) {
            for (ArmorStand stand : list) {
                stand.remove();
            }
        }
        this.stands.clear();
        this.lastLines.clear();
    }

    /**
     * Supprime les stands orphelins (ex. après un /reload) dans les chunks chargés.
     */
    public void cleanupOrphans() {
        for (org.bukkit.World world : EpicFarming.getInstance().getServer().getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ArmorStand.class)) {
                if (entity.getScoreboardTags().contains(TAG)) {
                    entity.remove();
                }
            }
        }
    }
}
