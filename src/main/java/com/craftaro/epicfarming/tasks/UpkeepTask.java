package com.craftaro.epicfarming.tasks;

import com.craftaro.epicfarming.EpicFarming;
import com.craftaro.epicfarming.boost.BoostData;
import com.craftaro.epicfarming.farming.Farm;
import com.craftaro.epicfarming.farming.levels.Level;
import com.craftaro.epicfarming.settings.Settings;
import com.craftaro.epicfarming.utils.FuelUtils;
import com.craftaro.epicfarming.utils.Methods;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tâche synchrone qui gère, pour toutes les fermes :
 * - la consommation du carburant,
 * - l'hologramme du carburant restant,
 * - la production des fermes minières.
 */
public class UpkeepTask extends BukkitRunnable {
    private static final BlockFace[] ADJACENT_FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    private final EpicFarming plugin;
    private final int secondsPerRun;
    private final Map<UUID, String> ownerNames = new HashMap<>();

    public UpkeepTask(EpicFarming plugin) {
        this.plugin = plugin;
        int period = Math.max(1, Settings.FARM_TICK_SPEED.getInt());
        this.secondsPerRun = Math.max(1, period / 20);
        runTaskTimer(plugin, 20L, period);
    }

    @Override
    public void run() {
        boolean fuelEnabled = Settings.FUEL_ENABLED.getBoolean();
        boolean hologramEnabled = Settings.HOLOGRAM_ENABLED.getBoolean();
        boolean linkEnabled = Settings.LINK_ENABLED.getBoolean() || Settings.LINK_ADJACENT.getBoolean();

        for (Farm farm : new ArrayList<>(this.plugin.getFarmManager().getFarms().values())) {
            try {
                if (!farm.isInLoadedChunk()) {
                    continue;
                }

                if (farm.isMining() && farm.hasFuel()) {
                    runMining(farm);
                }

                if (linkEnabled) {
                    transfer(farm);
                }

                if (fuelEnabled) {
                    farm.consumeFuel(this.secondsPerRun);
                }

                if (hologramEnabled) {
                    this.plugin.getHologramManager().update(farm, hologramLines(farm));
                } else {
                    this.plugin.getHologramManager().remove(farm);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private List<String> hologramLines(Farm farm) {
        String fuel = "";
        if (Settings.FUEL_ENABLED.getBoolean()) {
            fuel = farm.getFuelSeconds() > 0
                    ? Settings.HOLOGRAM_FORMAT.getString().replace("%time%", FuelUtils.formatTime(farm.getFuelSeconds()))
                    : Settings.HOLOGRAM_EMPTY.getString();
        }
        String link = farm.getLinkedLocation() != null ? Settings.HOLOGRAM_LINK.getString() : "";
        String owner = ownerName(farm);
        String produced = String.format(Locale.FRANCE, "%,d", farm.getTotalProduced());

        List<String> lines = new ArrayList<>();
        for (String raw : Settings.HOLOGRAM_LINES.getStringList()) {
            String line = raw
                    .replace("%type%", farm.getFarmType().translate())
                    .replace("%level%", String.valueOf(farm.getLevel().getLevel()))
                    .replace("%owner%", owner)
                    .replace("%produced%", produced)
                    .replace("%fuel%", fuel)
                    .replace("%link%", link);
            if (!ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line)).trim().isEmpty()) {
                lines.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }
        return lines;
    }

    private String ownerName(Farm farm) {
        if (farm.getPlacedBy() == null) {
            return "?";
        }
        return this.ownerNames.computeIfAbsent(farm.getPlacedBy(), uuid -> {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            return name == null ? "?" : name;
        });
    }

    /**
     * Envoie la production vers le conteneur lié et/ou les conteneurs collés à la ferme.
     */
    private void transfer(Farm farm) {
        if (farm.getItems().isEmpty()) {
            return;
        }

        List<Inventory> targets = new ArrayList<>();

        Location linked = farm.getLinkedLocation();
        if (linked != null && Settings.LINK_ENABLED.getBoolean() && linked.getWorld() != null
                && linked.getWorld().isChunkLoaded(linked.getBlockX() >> 4, linked.getBlockZ() >> 4)) {
            Inventory inventory = containerInventory(linked.getBlock());
            if (inventory != null) {
                targets.add(inventory);
            } else {
                // Le conteneur a été détruit : on délie la ferme.
                farm.setLinkedLocation(null);
            }
        }

        if (Settings.LINK_ADJACENT.getBoolean()) {
            for (BlockFace face : ADJACENT_FACES) {
                Inventory inventory = containerInventory(farm.getLocation().getBlock().getRelative(face));
                if (inventory != null) {
                    targets.add(inventory);
                }
            }
        }

        if (targets.isEmpty()) {
            return;
        }

        int budget = Settings.LINK_TRANSFER_PER_RUN.getInt() * this.secondsPerRun;
        Set<Material> blocked = new HashSet<>();
        Material boneMeal = XMaterial.BONE_MEAL.parseMaterial();

        for (ItemStack item : farm.getItems().toArray(new ItemStack[0])) {
            if (budget <= 0) {
                break;
            }
            if (item.getType() == boneMeal || blocked.contains(item.getType())) {
                continue;
            }

            int amount = Math.min(item.getAmount(), budget);
            ItemStack toMove = item.clone();
            toMove.setAmount(amount);

            for (Inventory target : targets) {
                Map<Integer, ItemStack> left = target.addItem(toMove);
                if (left.isEmpty()) {
                    toMove = null;
                    break;
                }
                toMove = left.values().iterator().next();
            }

            int moved = amount - (toMove == null ? 0 : toMove.getAmount());
            if (moved > 0) {
                farm.removeMaterial(item.getType(), moved);
                budget -= moved;
            }
            if (moved < amount) {
                blocked.add(item.getType());
            }
        }
    }

    public static Inventory containerInventory(Block block) {
        BlockState state = block.getState();
        if (state instanceof Hopper || state instanceof Chest || state instanceof Barrel || state instanceof ShulkerBox) {
            return ((InventoryHolder) state).getInventory();
        }
        return null;
    }

    private void runMining(Farm farm) {
        Level level = farm.getLevel();
        if (level == null || !level.isMining()) {
            return;
        }

        farm.setMiningProgress(farm.getMiningProgress() + this.secondsPerRun);
        if (farm.getMiningProgress() < level.getMiningIntervalSeconds()) {
            return;
        }

        Map<Material, Integer> ores = level.getMiningOres();
        int totalWeight = ores.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            return;
        }

        // Un boost du joueur multiplie la production.
        int multiplier = 1;
        BoostData boost = this.plugin.getBoostManager().getBoost(farm.getPlacedBy());
        if (boost != null && boost.getEndTime() > System.currentTimeMillis()) {
            multiplier = Math.max(1, boost.getMultiplier());
        }

        Map<Material, Integer> produced = new LinkedHashMap<>();
        int rolls = level.getMiningAmount() * multiplier;
        for (int i = 0; i < rolls; i++) {
            int pick = ThreadLocalRandom.current().nextInt(totalWeight);
            for (Map.Entry<Material, Integer> ore : ores.entrySet()) {
                pick -= ore.getValue();
                if (pick < 0) {
                    produced.merge(ore.getKey(), 1, Integer::sum);
                    break;
                }
            }
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : produced.entrySet()) {
            int remaining = entry.getValue();
            int max = entry.getKey().getMaxStackSize();
            while (remaining > 0) {
                int amount = Math.min(remaining, max);
                stacks.add(new ItemStack(entry.getKey(), amount));
                remaining -= amount;
            }
        }

        // Stockage plein : la ferme attend qu'on vide son inventaire, sans perdre de production.
        for (ItemStack stack : stacks) {
            if (!farm.willFit(stack)) {
                return;
            }
        }

        for (ItemStack stack : stacks) {
            farm.addItem(stack);
        }
        farm.setMiningProgress(0);

        if (!stacks.isEmpty()) {
            Methods.animate(farm.getLocation(), stacks.get(0));
        }
    }
}
