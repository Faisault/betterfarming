package com.craftaro.epicfarming;

import com.craftaro.core.SongodaCore;
import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.commands.CommandManager;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.core.configuration.Config;
import com.craftaro.core.gui.GuiManager;
import com.craftaro.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.epicfarming.boost.BoostData;
import com.craftaro.epicfarming.boost.BoostManager;
import com.craftaro.epicfarming.commands.CommandBoost;
import com.craftaro.epicfarming.commands.CommandGiveFarmItem;
import com.craftaro.epicfarming.commands.CommandReload;
import com.craftaro.epicfarming.commands.CommandSettings;
import com.craftaro.epicfarming.commands.CommandTop;
import com.craftaro.epicfarming.database.migrations._1_InitialMigration;
import com.craftaro.epicfarming.database.migrations._2_FuelMigration;
import com.craftaro.epicfarming.database.migrations._3_StatsMigration;
import com.craftaro.epicfarming.holograms.HologramManager;
import com.craftaro.epicfarming.tasks.UpkeepTask;
import org.bukkit.Material;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import com.craftaro.epicfarming.farming.Farm;
import com.craftaro.epicfarming.farming.FarmManager;
import com.craftaro.epicfarming.farming.FarmType;
import com.craftaro.epicfarming.farming.levels.Level;
import com.craftaro.epicfarming.farming.levels.LevelManager;
import com.craftaro.epicfarming.farming.levels.modules.Module;
import com.craftaro.epicfarming.farming.levels.modules.ModuleAutoBreeding;
import com.craftaro.epicfarming.farming.levels.modules.ModuleAutoButcher;
import com.craftaro.epicfarming.farming.levels.modules.ModuleAutoCollect;
import com.craftaro.epicfarming.listeners.BlockListeners;
import com.craftaro.epicfarming.listeners.EntityListeners;
import com.craftaro.epicfarming.listeners.InteractListeners;
import com.craftaro.epicfarming.listeners.InventoryListeners;
import com.craftaro.epicfarming.listeners.MoistureListeners;
import com.craftaro.epicfarming.listeners.UnloadListeners;
import com.craftaro.epicfarming.settings.Settings;
import com.craftaro.epicfarming.storage.Storage;
import com.craftaro.epicfarming.storage.StorageRow;
import com.craftaro.epicfarming.storage.types.StorageYaml;
import com.craftaro.epicfarming.tasks.FarmTask;
import com.craftaro.epicfarming.tasks.GrowthTask;
import com.craftaro.epicfarming.tasks.HopperTask;
import com.craftaro.epicfarming.utils.DataHelper;
import com.craftaro.epicfarming.utils.EntityUtils;
import com.craftaro.epicfarming.utils.Methods;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class EpicFarming extends SongodaPlugin {
    private final Config levelsFile = new Config(this, "levels.yml");
    private final Config miningLevelsFile = new Config(this, "mining-levels.yml");

    private final GuiManager guiManager = new GuiManager(this);
    private FarmManager farmManager;
    private LevelManager levelManager;
    private LevelManager miningLevelManager;
    private HologramManager hologramManager;
    // Joueurs en train de choisir un conteneur à lier (UUID joueur -> ferme).
    private final Map<UUID, Farm> pendingLinks = new java.util.HashMap<>();
    private UpkeepTask upkeepTask;
    private CommandManager commandManager;
    private BoostManager boostManager;

    private GrowthTask growthTask;
    private FarmTask farmTask;

    private EntityUtils entityUtils;
    private static EpicFarming instance;

    @Override
    public void onPluginLoad() {
    }

    @Override
    public void onPluginDisable() {
        this.farmTask.cancel();
        this.growthTask.cancel();
        if (this.upkeepTask != null) {
            this.upkeepTask.cancel();
        }
        if (this.hologramManager != null) {
            this.hologramManager.removeAll();
        }

        saveToFile();
        for (Farm farm : this.farmManager.getFarms().values()) {
            if (farm.needsToBeSaved()) {
                DataHelper.updateItems(farm);
            }
            if (farm.isFuelDirty()) {
                DataHelper.updateStats(farm);
            }
        }
    }

    @Override
    public void onPluginEnable() {
        instance = this;
        // Run Songoda Updater
        SongodaCore.registerPlugin(this, 21, XMaterial.WHEAT);

        // Setup Config
        Settings.setupConfig();
        if (!new File(getDataFolder(), "locales" + File.separator + "fr_FR.lang").exists()) {
            saveResource("locales/fr_FR.lang", false);
        }
        this.setLocale(Settings.LANGUGE_MODE.getString(), false);

        this.entityUtils = new EntityUtils();

        // Register commands
        this.commandManager = new CommandManager(this);
        this.commandManager.addMainCommand("EFA")
                .addSubCommands(
                        new CommandBoost(this),
                        new CommandGiveFarmItem(this),
                        new CommandReload(this),
                        new CommandSettings(this),
                        new CommandTop(this)
                );

        // Database stuff.
        initDatabase(Arrays.asList(new _1_InitialMigration(), new _2_FuelMigration(), new _3_StatsMigration()));
        DataHelper.init(this);

        this.loadLevelManager();
        this.loadMiningLevelManager();

        this.hologramManager = new HologramManager();
        this.hologramManager.cleanupOrphans();

        this.farmManager = new FarmManager(this.levelManager);
        this.boostManager = new BoostManager();

        // Register Listeners
        this.guiManager.init();
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new EntityListeners(this), this);
        pluginManager.registerEvents(new BlockListeners(this), this);
        pluginManager.registerEvents(new InteractListeners(this), this);
        pluginManager.registerEvents(new UnloadListeners(this), this);
        pluginManager.registerEvents(new InventoryListeners(), this);
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_14)) {
            pluginManager.registerEvents(new MoistureListeners(this), this);
        }

        // Start tasks
        this.growthTask = new GrowthTask(this);
        this.farmTask = new FarmTask(this);
        this.upkeepTask = new UpkeepTask(this);

        Bukkit.getScheduler().runTaskLater(this, () -> HopperTask.startTask(this), 30L);

        // Start auto save
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            saveToFile();

            for (Farm farm : this.farmManager.getFarms().values()) {
                if (farm.needsToBeSaved()) {
                    DataHelper.updateItemsAsync(farm);
                }
                if (farm.isFuelDirty()) {
                    DataHelper.updateStatsAsync(farm);
                }
            }
        }, 6000, 6000);
    }

    @Override
    public void onDataLoad() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // Legacy data! Yay!
            File folder = getDataFolder();
            File dataFile = new File(folder, "data.yml");

            boolean converted = false;
            if (dataFile.exists()) {
                converted = true;
                Storage storage = new StorageYaml(this);
                if (storage.containsGroup("farms")) {
                    Bukkit.getConsoleSender().sendMessage("[" + getDescription().getName() + "] " + ChatColor.RED +
                            "Conversion des données en cours. N'éteins PAS le serveur." +
                            "BetterFarming n'est pas encore chargé, évite que les joueurs " +
                            "interagissent avec le plugin avant la fin de la conversion.");

                    List<Farm> farms = new ArrayList<>();
                    for (StorageRow row : storage.getRowsByGroup("farms")) {
                        Location location = Methods.deserializeLocation(row.getKey());
                        if (location == null) {
                            continue;
                        }

                        if (row.get("level").asInt() == 0) {
                            continue;
                        }

                        String placedByStr = row.get("placedby").asString();
                        UUID placedBy = placedByStr == null ? null : UUID.fromString(placedByStr);

                        List<ItemStack> items = row.get("contents").asItemStackList();
                        if (items == null) {
                            items = new ArrayList<>();
                        }

                        FarmType farmType = FarmType.BOTH;
                        String farmTypeStr = row.get("farmtype").asString();
                        if (farmTypeStr != null) {
                            farmType = FarmType.valueOf(farmTypeStr);
                        }

                        Farm farm = new Farm(location, this.levelManager.getLevel(row.get("level").asInt()), placedBy, farmType);
                        farm.setItems(items);

                        farms.add(farm);
                    }
                    DataHelper.createFarms(farms);
                }

                // Adding in Boosts
                if (storage.containsGroup("boosts")) {
                    for (StorageRow row : storage.getRowsByGroup("boosts")) {
                        if (row.get("uuid").asObject() == null) {
                            continue;
                        }

                        this.dataManager.save(new BoostData(
                                row.get("amount").asInt(),
                                Long.parseLong(row.getKey()),
                                UUID.fromString(row.get("uuid").asString())));
                    }
                }
                dataFile.delete();
            }

            if (converted) {
                Bukkit.getConsoleSender().sendMessage("[" + getDescription().getName() + "] " + ChatColor.GREEN + "Conversion terminée :)");
            }

            this.farmManager.addFarms(this.dataManager.loadBatch(Farm.class, "active_farms"));
            this.boostManager.addBoosts(this.dataManager.loadBatch(BoostData.class, "boosted_players"));
        });
    }

    @Override
    public void onConfigReload() {
        this.setLocale(Settings.LANGUGE_MODE.getString(), true);
        this.locale.reloadMessages();
        loadLevelManager();
        loadMiningLevelManager();
    }

    @Override
    public List<Config> getExtraConfig() {
        return Arrays.asList(this.levelsFile, this.miningLevelsFile);
    }

    private void loadLevelManager() {
        if (!new File(this.getDataFolder(), "levels.yml").exists()) {
            this.saveResource("levels.yml", false);
        }
        this.levelsFile.load();

        // Load an instance of LevelManager
        this.levelManager = new LevelManager();

        /*
         * Register Levels into LevelManager from configuration.
         */
        for (String levelName : this.levelsFile.getKeys(false)) {
            ConfigurationSection levels = this.levelsFile.getConfigurationSection(levelName);

            if (levels.get("Auto-Harvest") != null) {
                levels.set("Auto-Collect", levels.getBoolean("Auto-Harvest"));
                levels.set("Auto-Harvest", null);
            }

            int level = Integer.parseInt(levelName.split("-")[1]);
            int costExperience = levels.getInt("Cost-xp");
            int costEconomy = levels.getInt("Cost-eco");
            int radius = levels.getInt("Radius");
            double speedMultiplier = levels.getDouble("Speed-Multiplier");
            boolean autoCollect = levels.getBoolean("Auto-Collect");
            boolean autoReplant = levels.getBoolean("Auto-Replant");
            int pages = levels.getInt("Pages", 1);

            if (levels.get("Auto-Breeding") instanceof Boolean) {
                levels.set("Auto-Breeding", 15);
            }

            ArrayList<Module> modules = new ArrayList<>();

            for (String key : levels.getKeys(false)) {
                if (key.equals("Auto-Breeding") && levels.getInt("Auto-Breeding") != 0) {
                    modules.add(new ModuleAutoBreeding(this, levels.getInt("Auto-Breeding")));
                } else if (key.equals("Auto-Butcher") && levels.getInt("Auto-Butcher") != 0) {
                    modules.add(new ModuleAutoButcher(this, levels.getInt("Auto-Butcher")));
                } else if (key.equals("Auto-Collect")) {
                    modules.add(new ModuleAutoCollect(this));
                }
            }
            this.levelManager.addLevel(level, costExperience, costEconomy, speedMultiplier, radius, autoCollect, autoReplant, pages, modules);
        }
        this.levelsFile.saveChanges();
    }

    private void loadMiningLevelManager() {
        if (!new File(this.getDataFolder(), "mining-levels.yml").exists()) {
            this.saveResource("mining-levels.yml", false);
        }
        this.miningLevelsFile.load();

        this.miningLevelManager = new LevelManager();

        for (String levelName : this.miningLevelsFile.getKeys(false)) {
            ConfigurationSection section = this.miningLevelsFile.getConfigurationSection(levelName);
            if (section == null || !levelName.contains("-")) {
                continue;
            }

            int level = Integer.parseInt(levelName.split("-")[1]);
            Map<Material, Integer> ores = new LinkedHashMap<>();
            ConfigurationSection oresSection = section.getConfigurationSection("Ores");
            if (oresSection != null) {
                for (String oreName : oresSection.getKeys(false)) {
                    Material material = Material.matchMaterial(oreName);
                    int weight = oresSection.getInt(oreName);
                    if (material == null || weight <= 0) {
                        getLogger().warning("Minage " + levelName + " : minerai invalide ou poids nul ignoré -> " + oreName);
                        continue;
                    }
                    ores.put(material, weight);
                }
            }

            Level created = this.miningLevelManager.addLevel(level,
                    section.getInt("Cost-xp"),
                    section.getInt("Cost-eco"),
                    1, 0, false, false,
                    section.getInt("Pages", 1),
                    new ArrayList<>());
            created.setMining(section.getInt("Interval", 30), section.getInt("Amount", 1), ores);
        }
    }

    /*
     * Saves registered farms to file.
     */
    private void saveToFile() {
        if (this.levelManager != null) {
            for (Level level : this.levelManager.getLevels().values()) {
                for (Module module : level.getRegisteredModules()) {
                    module.saveDataToFile();
                }
            }
        }
    }

    public int getLevelFromItem(ItemStack item) {
        NBTItem nbtItem = new NBTItem(item);

        if (nbtItem.hasTag("level")) {
            return nbtItem.getInteger("level");
        }

        // Legacy trash.
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return 0;
        }
        if (item.getItemMeta().getDisplayName().contains(":")) {
            return NumberUtils.toInt(item.getItemMeta().getDisplayName().replace("§", "").split(":")[0], 0);
        }
        return 0;
    }

    /**
     * @return le bloc physique utilisé par les fermes de culture ou par les fermes minières.
     */
    public Material getFarmBlockMaterial(boolean mining) {
        if (mining) {
            Material material = Material.matchMaterial(Settings.MINING_BLOCK_MATERIAL.getString());
            return material != null ? material : Material.LODESTONE;
        }
        return Settings.FARM_BLOCK_MATERIAL.getMaterial(XMaterial.END_ROD).parseMaterial();
    }

    public boolean isFarmBlock(Material material) {
        return material == getFarmBlockMaterial(false) || material == getFarmBlockMaterial(true);
    }

    public boolean isMiningItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.hasTag("mining") && nbtItem.getBoolean("mining");
    }

    /**
     * Item d'une ferme existante : conserve le carburant restant (évite de recharger en reposant la ferme).
     */
    public ItemStack makeFarmItem(Farm farm) {
        ItemStack item = makeFarmItem(farm.getLevel());
        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setLong("fuel", farm.getFuelSeconds());
        return nbtItem.getItem();
    }

    public ItemStack makeFarmItem(Level level) {
        ItemStack item = new ItemStack(getFarmBlockMaterial(level.isMining()));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(TextUtils.formatText(Methods.formatName(level)));
        String line = getLocale().getMessage("general.nametag.lore").toText();
        if (!line.equals("")) {
            meta.setLore(Collections.singletonList(line));
        }
        item.setItemMeta(meta);

        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setInteger("level", level.getLevel());
        if (level.isMining()) {
            nbtItem.setBoolean("mining", true);
        }
        return nbtItem.getItem();
    }

    public FarmManager getFarmManager() {
        return this.farmManager;
    }

    public LevelManager getLevelManager() {
        return this.levelManager;
    }

    public LevelManager getLevelManager(boolean mining) {
        return mining ? this.miningLevelManager : this.levelManager;
    }

    public LevelManager getLevelManager(Farm farm) {
        return getLevelManager(farm.isMining());
    }

    public Map<UUID, Farm> getPendingLinks() {
        return this.pendingLinks;
    }

    public HologramManager getHologramManager() {
        return this.hologramManager;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public BoostManager getBoostManager() {
        return this.boostManager;
    }

    public GrowthTask getGrowthTask() {
        return this.growthTask;
    }

    public FarmTask getFarmTask() {
        return this.farmTask;
    }

    public GuiManager getGuiManager() {
        return this.guiManager;
    }

    public EntityUtils getEntityUtils() {
        return this.entityUtils;
    }

    public static EpicFarming getInstance() {
        return instance;
    }
}
