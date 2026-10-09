package com.craftaro.epicfarming.gui;

import com.craftaro.core.chat.AdventureUtils;
import com.craftaro.core.gui.CustomizableGui;
import com.craftaro.core.gui.GuiUtils;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.core.utils.NumberUtils;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.core.utils.TimeUtils;
import com.craftaro.epicfarming.EpicFarming;
import com.craftaro.epicfarming.boost.BoostData;
import com.craftaro.epicfarming.farming.Farm;
import com.craftaro.epicfarming.farming.FarmType;
import com.craftaro.epicfarming.farming.UpgradeType;
import com.craftaro.epicfarming.farming.levels.Level;
import com.craftaro.epicfarming.farming.levels.modules.Module;
import com.craftaro.epicfarming.settings.Settings;
import com.craftaro.epicfarming.utils.DataHelper;
import com.craftaro.epicfarming.utils.FuelUtils;
import com.craftaro.epicfarming.utils.Progression;
import com.craftaro.epicfarming.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class OverviewGui extends CustomizableGui {
    private final EpicFarming plugin;
    private final Farm farm;
    private final Level level;
    private final Player player;

    private int task;

    public OverviewGui(Farm farm, Player player) {
        super(EpicFarming.getPlugin(EpicFarming.class), "overview");
        this.plugin = EpicFarming.getPlugin(EpicFarming.class);

        this.farm = farm;
        this.level = farm.getLevel();
        this.player = player;
        this.setRows(6);
        this.setTitle(Methods.formatName(this.level));
        this.setAcceptsItems(true);
        this.setUnlockedRange(3, 0, 5, 8);

        ItemStack glass1 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_1.getMaterial());
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial());
        ItemStack glass3 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_3.getMaterial());

        this.setDefaultItem(null);

        mirrorFill("mirrorfill_1", 0, 0, false, true, glass2);
        mirrorFill("mirrorfill_2", 0, 1, false, true, glass2);
        mirrorFill("mirrorfill_3", 0, 2, false, true, glass3);
        mirrorFill("mirrorfill_4", 1, 0, false, true, glass2);
        mirrorFill("mirrorfill_5", 1, 1, false, true, glass3);
        mirrorFill("mirrorfill_6", 2, 0, false, true, glass2);
        mirrorFill("mirrorfill_7", 2, 1, false, true, glass2);
        mirrorFill("mirrorfill_8", 2, 2, false, true, glass3);

        mirrorFill("mirrorfill_9", 0, 3, false, true, glass1);
        mirrorFill("mirrorfill_10", 0, 4, false, false, glass1);
        mirrorFill("mirrorfill_11", 1, 3, false, true, glass1);
        mirrorFill("mirrorfill_12", 1, 2, false, true, glass1);
        mirrorFill("mirrorfill_13", 2, 3, false, true, glass1);
        mirrorFill("mirrorfill_14", 2, 4, false, false, glass1);

        // enable page events
        if (this.level.getPages() > 1) {
            setPages(this.level.getPages());
            setPrevPage(2, 0, GuiUtils.createButtonItem(XMaterial.ARROW, this.plugin.getLocale().getMessage("general.interface.previous").getMessage()));
            setNextPage(2, 8, GuiUtils.createButtonItem(XMaterial.ARROW, this.plugin.getLocale().getMessage("general.interface.next").getMessage()));
            setOnPage((event) -> updateInventory());
        }

        // events
        this.setOnOpen((event) -> updateInventory());
        this.setDefaultAction((event) ->
                Bukkit.getScheduler().runTaskLater(this.plugin, this::updateFarm, 0L));
        runTask();
        this.setOnClose((event) -> {
            updateFarm();
            farm.close();
            Bukkit.getScheduler().cancelTask(this.task);
        });

        showPage();
    }

    private void showPage() {
        Level nextLevel = this.plugin.getLevelManager(this.farm).getHighestLevel().getLevel() > this.level.getLevel() ? this.plugin.getLevelManager(this.farm).getLevel(this.level.getLevel() + 1) : null;

        List<String> farmLore = this.level.getDescription();
        if (Settings.FUEL_ENABLED.getBoolean()) {
            farmLore.add(this.farm.getFuelSeconds() > 0
                    ? this.plugin.getLocale().getMessage("interface.button.fuel")
                    .processPlaceholder("time", FuelUtils.formatTime(this.farm.getFuelSeconds())).toText()
                    : this.plugin.getLocale().getMessage("interface.button.nofuel").toText());
        }
        farmLore.add("");
        if (nextLevel == null) {
            farmLore.add(this.plugin.getLocale().getMessage("event.upgrade.maxed").toText());
        } else {
            farmLore.add(this.plugin.getLocale().getMessage("interface.button.level")
                    .processPlaceholder("level", nextLevel.getLevel()).toText());
            farmLore.addAll(nextLevel.getDescription());
        }

        BoostData boostData = this.plugin.getBoostManager().getBoost(this.farm.getPlacedBy());
        if (boostData != null) {
            String[] parts = this.plugin.getLocale().getMessage("interface.button.boostedstats")
                    .processPlaceholder("amount", Integer.toString(boostData.getMultiplier()))
                    .processPlaceholder("time", TimeUtils.makeReadable(boostData.getEndTime() - System.currentTimeMillis()))
                    .toText().split("\\|");
            farmLore.add("");
            for (String line : parts) {
                farmLore.add(AdventureUtils.formatLegacy(line));
            }
        }

        setItem("farm", 13, GuiUtils.createButtonItem(XMaterial.matchXMaterial(this.plugin.getFarmBlockMaterial(this.farm.isMining())),
                this.plugin.getLocale().getMessage(this.level.isMining() ? "general.nametag.miningfarm" : "general.nametag.farm")
                        .processPlaceholder("level", this.level.getLevel()).toText(),
                farmLore));

        if (this.player != null && Settings.UPGRADE_WITH_XP.getBoolean() && this.player.hasPermission("EpicFarming.Upgrade.XP")) {

            setButton("xp", 11, GuiUtils.createButtonItem(Settings.XP_ICON.getMaterial(XMaterial.EXPERIENCE_BOTTLE),
                            this.plugin.getLocale().getMessage("interface.button.upgradewithxp").getMessage(),
                            nextLevel != null
                                    ? this.plugin.getLocale().getMessage("interface.button.upgradewithxplore")
                                    .processPlaceholder("cost", nextLevel.getCostExperience()).getMessage()
                                    : this.plugin.getLocale().getMessage("event.upgrade.maxed").getMessage()),
                    event -> {
                        this.farm.upgrade(UpgradeType.EXPERIENCE, this.player);
                        onClose(this.guiManager, this.player);
                        this.farm.view(this.player, true);
                    });
        }

        if (Settings.LINK_ENABLED.getBoolean() && this.player != null) {
            Location linked = this.farm.getLinkedLocation();
            List<String> linkLore = new ArrayList<>();
            linkLore.add(linked == null
                    ? this.plugin.getLocale().getMessage("interface.button.linknone").toText()
                    : this.plugin.getLocale().getMessage("interface.button.linkstatus")
                    .processPlaceholder("location", linked.getBlockX() + " " + linked.getBlockY() + " " + linked.getBlockZ()).toText());
            linkLore.add(this.plugin.getLocale().getMessage("interface.button.linkclick").toText());

            setButton("link", 1, GuiUtils.createButtonItem(XMaterial.HOPPER,
                    this.plugin.getLocale().getMessage("interface.button.link").toText(), linkLore), event -> {
                if (event.clickType.isShiftClick()) {
                    this.farm.setLinkedLocation(null);
                    DataHelper.updateStatsAsync(this.farm);
                    showPage();
                    return;
                }
                this.plugin.getPendingLinks().put(this.player.getUniqueId(), this.farm);
                this.player.closeInventory();
                this.plugin.getLocale().getMessage("event.link.start").sendPrefixedMessage(this.player);
            });
        }

        List<String> statsLore = new ArrayList<>();
        statsLore.add(this.plugin.getLocale().getMessage("interface.button.statsproduced")
                .processPlaceholder("amount", String.format(Locale.FRANCE, "%,d", this.farm.getTotalProduced())).toText());
        long nextMilestone = Progression.nextThreshold(this.farm.getTotalProduced());
        statsLore.add(nextMilestone < 0
                ? this.plugin.getLocale().getMessage("interface.button.statsmax").toText()
                : this.plugin.getLocale().getMessage("interface.button.statsnext")
                .processPlaceholder("amount", String.format(Locale.FRANCE, "%,d", nextMilestone)).toText());
        setItem("stats", 7, GuiUtils.createButtonItem(XMaterial.NETHER_STAR,
                this.plugin.getLocale().getMessage("interface.button.stats").toText(), statsLore));

        Material farmTypeMaterial = XMaterial.WHEAT.parseMaterial();
        if (this.farm.getFarmType() == FarmType.LIVESTOCK) {
            farmTypeMaterial = XMaterial.BEEF.parseMaterial();
        } else if (this.farm.getFarmType() == FarmType.BOTH) {
            farmTypeMaterial = XMaterial.GOLD_NUGGET.parseMaterial();
        }

        ItemStack farmType = new ItemStack(farmTypeMaterial, 1);
        ItemMeta farmTypeMeta = farmType.getItemMeta();
        farmTypeMeta.setDisplayName(this.plugin.getLocale().getMessage("interface.button.farmtype")
                .processPlaceholder("type", this.farm.getFarmType().translate())
                .toText());
        farmTypeMeta.setLore(Collections.singletonList(this.plugin.getLocale().getMessage("interface.button.farmtypelore")
                .toText()));
        farmType.setItemMeta(farmTypeMeta);

        Map<Integer, Integer[]> layouts = new HashMap<>();
        layouts.put(1, new Integer[]{22});
        layouts.put(2, new Integer[]{22, 4});
        layouts.put(3, new Integer[]{22, 3, 5});
        layouts.put(4, new Integer[]{23, 3, 5, 21});
        layouts.put(5, new Integer[]{23, 3, 5, 21, 22});
        layouts.put(6, new Integer[]{23, 3, 4, 5, 21, 22});
        layouts.put(7, new Integer[]{23, 3, 4, 5, 21, 22, 12});
        layouts.put(8, new Integer[]{23, 3, 4, 5, 21, 22, 12, 14});
        layouts.put(9, new Integer[]{23, 3, 4, 5, 21, 22, 12, 14, 20});
        layouts.put(10, new Integer[]{23, 3, 4, 5, 21, 22, 12, 14, 20, 24});

        List<Module> modules = this.level.getRegisteredModules().stream().filter(module ->
                module.getGUIButton(this.farm) != null).collect(Collectors.toList());

        int amount = modules.size();

        if (amount > 0) {
            amount++;
        }

        Integer[] layout = layouts.get(amount);

        for (int i = 0; i < amount; i++) {
            int slot = layout[i];
            if (i == 0 && this.level.getRegisteredModules().stream().map(Module::getName).anyMatch(s -> s.equals("AutoCollect"))) {
                if (this.level.getRegisteredModules().stream().map(Module::getName).anyMatch(s -> s.equals("AutoButcher")
                        || s.equals("AutoBreeding"))) {
                    setButton("toggle", slot, farmType,
                            (event) -> {
                                this.farm.toggleFarmType();
                                if (this.farm.getFarmType() != FarmType.LIVESTOCK) {
                                    this.farm.tillLand();
                                }
                                showPage();
                            });
                }
            } else {
                if (modules.isEmpty()) {
                    break;
                }

                Module module = modules.get(0);
                modules.remove(module);
                setButton("module_" + module.getName().toLowerCase(), slot, module.getGUIButton(this.farm),
                        (event) -> module.runButtonPress(this.player, this.farm, event.clickType));
            }
        }
    }

    private void runTask() {
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
            updateFarm();
            showPage();
        }, 2L, 1L);
    }

    public void updateInventory() {
        List<ItemStack> items = this.farm.getItems();

        int j = (this.page - 1) * 27;
        for (int i = 27; i <= 54; ++i) {
            if (items.size() <= j) {
                setItem(i, null);
            } else {
                setItem(i, items.get(j));
            }

            ++j;
        }
    }

    private void updateFarm() {
        List<ItemStack> items = new ArrayList<>();
        int start = 27 * (this.page - 1);
        int j = 27;
        for (int i = 0; i <= 27 * this.pages; i++) {
            if (i >= start && i < start + 27) {
                ItemStack item = getItem(j);
                j++;
                if (item != null && item.getType() != Material.AIR) {
                    items.add(item);
                }
            } else {
                if (i >= this.farm.getItems().size()) {
                    continue;
                }
                items.add(this.farm.getItems().get(i));
            }
        }
        this.farm.setItems(items);
    }
}
