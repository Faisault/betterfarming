package com.craftaro.epicfarming.listeners;

import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.epicfarming.EpicFarming;
import com.craftaro.epicfarming.farming.Farm;
import com.craftaro.epicfarming.tasks.UpkeepTask;
import com.craftaro.epicfarming.utils.DataHelper;
import com.craftaro.epicfarming.utils.FuelUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import com.craftaro.epicfarming.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractListeners implements Listener {
    private final EpicFarming plugin;

    public InteractListeners(EpicFarming plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }
        Location location = e.getClickedBlock().getLocation();

        // Liaison d'un conteneur : le joueur a cliqué sur "Lier" dans le menu de sa ferme.
        Farm pending = this.plugin.getPendingLinks().get(e.getPlayer().getUniqueId());
        if (pending != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getHand() == EquipmentSlot.HAND) {
            e.setCancelled(true);
            this.plugin.getPendingLinks().remove(e.getPlayer().getUniqueId());
            linkContainer(e.getPlayer(), pending, e.getClickedBlock());
            return;
        }

        if (e.getItem() != null && XMaterial.BONE_MEAL.isSimilar(e.getItem()) && this.plugin.getFarmManager().checkForFarm(location) != null) {
            e.setCancelled(true);
        }

        if (!this.plugin.isFarmBlock(e.getClickedBlock().getType())) {
            return;
        }

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Farm farm = this.plugin.getFarmManager().getFarm(location);
        if (farm == null) {
            return;
        }

        // Clic droit avec un item de carburant : on recharge la ferme au lieu d'ouvrir le menu.
        if (Settings.FUEL_ENABLED.getBoolean() && e.getItem() != null) {
            Integer seconds = FuelUtils.loadFuelItems().get(e.getItem().getType());
            if (seconds != null) {
                e.setCancelled(true);
                if (e.getHand() == EquipmentSlot.HAND) {
                    refuel(e.getPlayer(), farm, e.getItem(), seconds);
                }
                return;
            }
        }

        e.setCancelled(true);
        farm.view(e.getPlayer(), false);
    }

    private void linkContainer(Player player, Farm farm, org.bukkit.block.Block block) {
        if (this.plugin.getFarmManager().getFarm(farm.getLocation()) != farm) {
            return;
        }
        if (UpkeepTask.containerInventory(block) == null) {
            this.plugin.getLocale().getMessage("event.link.invalid").sendPrefixedMessage(player);
            return;
        }
        Location farmLoc = farm.getLocation();
        if (!block.getWorld().equals(farmLoc.getWorld())
                || block.getLocation().distance(farmLoc) > Settings.LINK_MAX_DISTANCE.getInt()) {
            this.plugin.getLocale().getMessage("event.link.toofar")
                    .processPlaceholder("distance", Settings.LINK_MAX_DISTANCE.getInt()).sendPrefixedMessage(player);
            return;
        }
        farm.setLinkedLocation(block.getLocation());
        DataHelper.updateStatsAsync(farm);
        this.plugin.getLocale().getMessage("event.link.success").sendPrefixedMessage(player);
    }

    private void refuel(Player player, Farm farm, ItemStack item, int seconds) {
        long added = farm.addFuel(seconds);
        if (added <= 0) {
            this.plugin.getLocale().getMessage("event.fuel.full").sendPrefixedMessage(player);
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        this.plugin.getLocale().getMessage("event.fuel.added")
                .processPlaceholder("time", FuelUtils.formatTime(farm.getFuelSeconds()))
                .sendPrefixedMessage(player);
    }
}
