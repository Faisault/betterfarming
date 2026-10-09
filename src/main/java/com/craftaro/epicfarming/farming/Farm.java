package com.craftaro.epicfarming.farming;

import com.craftaro.core.compatibility.CompatibleMaterial;
import com.craftaro.core.compatibility.CompatibleParticleHandler;
import com.craftaro.core.database.Data;
import com.craftaro.core.database.SerializedLocation;
import com.craftaro.third_party.com.cryptomorin.xseries.XBlock;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.craftaro.epicfarming.EpicFarming;
import com.craftaro.epicfarming.farming.levels.Level;
import com.craftaro.epicfarming.farming.levels.LevelManager;
import com.craftaro.epicfarming.gui.OverviewGui;
import com.craftaro.epicfarming.settings.Settings;
import com.craftaro.epicfarming.utils.Progression;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Farm implements Data {
    // This is the unique identifier for this farm.
    // It is reset on every plugin load.
    private final UUID uniqueId = UUID.randomUUID();

    // Id for database usage.
    private final int id;

    private boolean needsToBeSaved = false;

    private static final Random random = new Random();
    private final List<Block> cachedCrops = new ArrayList<>();
    private final List<ItemStack> items = new ArrayList<>();
    private Location location;
    private Level level;
    private OverviewGui opened = null;
    private final UUID placedBy;
    private UUID viewing = null;
    private long lastCached = 0;

    private FarmType farmType;

    // Carburant restant (secondes) et progression du minage en cours.
    private long fuelSeconds = 0;
    private boolean fuelDirty = false;
    private int miningProgress = 0;

    // Progression : production totale et conteneur lié.
    private long totalProduced = 0;
    private Location linked = null;

    private final Map<String, Object> moduleCache = new HashMap<>();

    /**
     * Default constructor for database usage.
     */
    public Farm() {
        this.id = 0;
        this.location = null;
        this.level = null;
        this.placedBy = null;
        this.farmType = null;
    }

    /**
     * This is the constructor for a new farm.
     * It is used when a player places a new farm.
     *
     * @param location The location of the farm.
     * @param level    The level of the farm.
     * @param placedBy The player who placed the farm.
     * @param farmType The type of farm.
     */
    public Farm(Location location, Level level, UUID placedBy, FarmType farmType) {
        this(EpicFarming.getPlugin(EpicFarming.class).getDataManager().getNextId("active_farms"), location, level, placedBy, farmType);
    }

    /**
     * This is the constructor for a new farm.
     * It is used when we load a farm from the database.
     *
     * @param id       The id of the farm.
     * @param location The location of the farm.
     * @param level    The level of the farm.
     * @param placedBy The player who placed the farm.
     * @param farmType The type of farm.
     */
    public Farm(int id, Location location, Level level, UUID placedBy, FarmType farmType) {
        this.id = id;
        this.location = location;
        this.level = level;
        this.placedBy = placedBy;
        this.farmType = farmType;
    }

    public void view(Player player, boolean force) {
        if (!player.hasPermission("epicfarming.view") && !force) {
            return;
        }

        if (this.opened != null && !force) {
            return;
        }

        this.opened = new OverviewGui(this, player);

        EpicFarming.getInstance().getGuiManager().showGUI(player, this.opened);
    }

    public void forceMenuClose() {
        if (this.opened == null) {
            return;
        }

        this.opened.close();
    }

    public void upgrade(UpgradeType type, Player player) {
        EpicFarming instance = EpicFarming.getInstance();
        if (type != UpgradeType.EXPERIENCE) {
            return;
        }

        if (instance.getLevelManager(this).getLevels().containsKey(this.level.getLevel() + 1)) {
            Level level = instance.getLevelManager(this).getLevel(this.level.getLevel() + 1);
            int cost = level.getCostExperience();

            if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    player.setLevel(player.getLevel() - cost);
                }
                upgradeFinal(level, player);
            } else {
                instance.getLocale().getMessage("event.upgrade.cannotafford").sendPrefixedMessage(player);
            }
        }
    }

    private void upgradeFinal(Level level, Player player) {
        EpicFarming instance = EpicFarming.getInstance();
        this.level = level;
        instance.getDataManager().save(this);
        if (instance.getLevelManager(this).getHighestLevel() != level) {
            instance.getLocale().getMessage("event.upgrade.success")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);
        } else {
            instance.getLocale().getMessage("event.upgrade.successmaxed")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);
        }
        tillLand();
        Location loc = this.location.clone().add(.5, .5, .5);
        CompatibleParticleHandler.spawnParticles(Settings.PARTICLE_TYPE.getString(), loc, 200, .5, .5, .5);

        if (instance.getLevelManager(this).getHighestLevel() != level) {
            XSound.ENTITY_PLAYER_LEVELUP.play(player, .6f, 15);
        } else {
            XSound.ENTITY_PLAYER_LEVELUP.play(player, 2, 25);

            XSound.BLOCK_NOTE_BLOCK_CHIME.play(player, 2, 25);
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> XSound.BLOCK_NOTE_BLOCK_CHIME.play(player, 1.2f, 35), 5);
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> XSound.BLOCK_NOTE_BLOCK_CHIME.play(player, 1.8f, 35), 10);
        }
    }

    public boolean tillLand() {
        if (isMining() || Settings.DISABLE_AUTO_TIL_LAND.getBoolean()) {
            return true;
        }
        Block block = this.location.getBlock();
        int radius = this.level.getRadius();
        int bx = block.getX();
        int by = block.getY();
        int bz = block.getZ();
        for (int fx = -radius; fx <= radius; fx++) {
            for (int fy = -2; fy <= 1; fy++) {
                for (int fz = -radius; fz <= radius; fz++) {
                    Block b2 = block.getWorld().getBlockAt(bx + fx, by + fy, bz + fz);

                    // Check if the block is a grass block or dirt
                    if (b2.getType() == XMaterial.GRASS_BLOCK.parseMaterial() || b2.getType() == Material.DIRT) {
                        // Check if the block above is junk (grass or a flower)
                        Block blockAbove = b2.getRelative(BlockFace.UP);
                        if (isJunk(blockAbove.getType())) {
                            Bukkit.getScheduler().runTaskLater(EpicFarming.getInstance(), () -> {
                                XBlock.setType(b2, XMaterial.FARMLAND);
                                Collection<ItemStack> drops = blockAbove.getDrops();
                                if (!drops.isEmpty()) {
                                    blockAbove.breakNaturally(drops.iterator().next());
                                } else {
                                    blockAbove.breakNaturally();
                                }
                                XSound.BLOCK_GRASS_BREAK.play(b2.getLocation(), 10, 15);
                            }, random.nextInt(30) + 1);
                        } else if (blockAbove.getType() == Material.AIR) {
                            Bukkit.getScheduler().runTaskLater(EpicFarming.getInstance(), () -> {
                                XBlock.setType(b2, XMaterial.FARMLAND);
                                XSound.BLOCK_GRASS_BREAK.play(b2.getLocation(), 10, 15);
                            }, random.nextInt(30) + 1);
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isJunk(Material material) {
        XMaterial xMaterial = XMaterial.matchXMaterial(material);
        if (xMaterial == null) {
            return false;
        }

        switch (xMaterial) {
            case SHORT_GRASS:
            case TALL_GRASS:
            case FERN:
            case LARGE_FERN:
            case DANDELION:
            case POPPY:
            case BLUE_ORCHID:
            case ALLIUM:
            case AZURE_BLUET:
            case RED_TULIP:
            case ORANGE_TULIP:
            case WHITE_TULIP:
            case PINK_TULIP:
            case OXEYE_DAISY:
            case CORNFLOWER:
            case LILY_OF_THE_VALLEY:
            case WITHER_ROSE:
            case SUNFLOWER:
            case LILAC:
            case ROSE_BUSH:
            case PEONY:
                return true;
            default:
                return false;
        }
    }

    //Don't use getUniqueId here as it conflicts with the Data interface.
    public UUID getFarmUUID() {
        return this.uniqueId;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("farm_type", this.farmType.name());
        map.put("level", this.level.getLevel());
        map.put("placed_by", this.placedBy.toString());
        map.put("fuel_seconds", this.fuelSeconds);
        map.put("total_produced", this.totalProduced);
        map.put("linked_world", this.linked == null || this.linked.getWorld() == null ? "" : this.linked.getWorld().getName());
        map.put("linked_x", this.linked == null ? 0 : this.linked.getBlockX());
        map.put("linked_y", this.linked == null ? 0 : this.linked.getBlockY());
        map.put("linked_z", this.linked == null ? 0 : this.linked.getBlockZ());
        map.putAll(SerializedLocation.of(this.location));
        return map;
    }

    @Override
    public Data deserialize(Map<String, Object> map) {
        int id = (int) map.get("id");
        FarmType farmType = FarmType.valueOf((String) map.get("farm_type"));
        EpicFarming plugin = EpicFarming.getPlugin(EpicFarming.class);
        LevelManager manager = plugin.getLevelManager(farmType == FarmType.MINING);
        Level level = manager.getLevel((int) map.get("level"));
        if (level == null) {
            level = manager.getLowestLevel();
        }
        UUID placedBy = UUID.fromString((String) map.get("placed_by"));
        Location location = SerializedLocation.of(map);
        Farm farm = new Farm(id, location, level, placedBy, farmType);
        Object fuel = map.get("fuel_seconds");
        if (fuel instanceof Number) {
            farm.fuelSeconds = ((Number) fuel).longValue();
        }
        Object produced = map.get("total_produced");
        if (produced instanceof Number) {
            farm.totalProduced = ((Number) produced).longValue();
        }
        Object linkedWorld = map.get("linked_world");
        if (linkedWorld instanceof String && !((String) linkedWorld).isEmpty() && Bukkit.getWorld((String) linkedWorld) != null) {
            farm.linked = new Location(Bukkit.getWorld((String) linkedWorld),
                    ((Number) map.get("linked_x")).intValue(),
                    ((Number) map.get("linked_y")).intValue(),
                    ((Number) map.get("linked_z")).intValue());
        }
        return farm;
    }

    @Override
    public String getTableName() {
        return "active_farms";
    }

    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    /**
     * Ajoute une production à la ferme : applique la chance de récolte critique,
     * compte la production et déclenche les paliers.
     */
    public void addItem(ItemStack toAdd) {
        if (toAdd == null || toAdd.getType() == Material.AIR) {
            return;
        }

        ItemStack stack = toAdd;
        boolean critical = Settings.CRIT_ENABLED.getBoolean()
                && java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < Settings.CRIT_CHANCE.getInt();
        if (critical) {
            stack = toAdd.clone();
            stack.setAmount(Math.min(stack.getMaxStackSize(), toAdd.getAmount() * 2));
        }

        addItemInternal(stack);

        long before = this.totalProduced;
        this.totalProduced += stack.getAmount();
        this.fuelDirty = true;
        Progression.check(this, before, this.totalProduced);

        if (critical) {
            criticalEffects();
        }
    }

    private void criticalEffects() {
        Bukkit.getScheduler().runTask(EpicFarming.getInstance(), () -> {
            Location loc = this.location.clone().add(.5, 1.2, .5);
            CompatibleParticleHandler.spawnParticles(Settings.PARTICLE_TYPE.getString(), loc, 25, .4, .4, .4);
            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(loc, 1f, 1.5f);

            Player owner = this.placedBy == null ? null : Bukkit.getPlayer(this.placedBy);
            if (owner != null && owner.getWorld().equals(this.location.getWorld())
                    && owner.getLocation().distanceSquared(this.location) < 32 * 32) {
                owner.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(
                                EpicFarming.getInstance().getLocale().getMessage("event.crit.bonus").toText()));
            }
        });
    }

    // Should be used in sync.
    private void addItemInternal(ItemStack toAdd) {
        this.needsToBeSaved = true;
        synchronized (this.items) {
            for (ItemStack item : new ArrayList<>(this.items)) {
                if (item.getType() != toAdd.getType()
                        || item.getAmount() + toAdd.getAmount() > item.getMaxStackSize()) {
                    continue;
                }
                item.setAmount(item.getAmount() + toAdd.getAmount());
                if (this.opened != null) {
                    this.opened.updateInventory();
                }
                return;
            }
            this.items.add(toAdd);
            if (this.opened != null) {
                this.opened.updateInventory();
            }
        }
    }

    public void removeMaterial(Material material, int amount) {
        this.needsToBeSaved = true;
        synchronized (this.items) {
            for (ItemStack item : getItems().toArray(new ItemStack[0])) {
                if (material == item.getType()) {
                    item.setAmount(item.getAmount() - amount);

                    if (item.getAmount() <= 0) {
                        this.items.remove(item);
                    }
                    if (this.opened != null) {
                        this.opened.updateInventory();
                    }
                    return;
                }
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean willFit(ItemStack item) {
        synchronized (this.items) {
            if (this.items.size() < 27 * this.level.getPages()) {
                return true;
            }

            for (ItemStack stack : this.items) {
                if (stack.isSimilar(item) && stack.getAmount() < stack.getMaxStackSize()) {
                    return true;
                }
            }
        }

        return false;
    }

    public void setItems(List<ItemStack> items) {
        this.needsToBeSaved = true;
        synchronized (this.items) {
            this.items.clear();
            this.items.addAll(items);

            if (this.opened != null) {
                this.opened.updateInventory();
            }
        }
    }

    public UUID getViewing() {
        return this.viewing;
    }

    public void setViewing(UUID viewing) {
        this.viewing = viewing;
    }

    public void addCachedCrop(Block block) {
        this.cachedCrops.add(block);
    }

    public void removeCachedCrop(Block block) {
        this.cachedCrops.remove(block);
    }

    public List<Block> getCachedCrops() {
        return new ArrayList<>(this.cachedCrops);
    }

    public void clearCache() {
        this.cachedCrops.clear();
    }

    public long getLastCached() {
        return this.lastCached;
    }

    public void setLastCached(long lastCached) {
        this.lastCached = lastCached;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isInLoadedChunk() {
        return this.location != null && this.location.getWorld() != null && this.location.getWorld().isChunkLoaded(((int) this.location.getX()) >> 4, ((int) this.location.getZ()) >> 4);
    }

    public Location getLocation() {
        return this.location.clone();
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public UUID getPlacedBy() {
        return this.placedBy;
    }

    public Level getLevel() {
        return this.level;
    }

    public void close() {
        this.opened = null;
    }

    public Object getDataFromModuleCache(String key) {
        return this.moduleCache.getOrDefault(key, null);
    }

    public void addDataToModuleCache(String key, Object data) {
        this.moduleCache.put(key, data);
    }

    public boolean isDataCachedInModuleCache(String key) {
        return this.moduleCache.containsKey(key);
    }

    public void removeDataFromModuleCache(String key) {
        this.moduleCache.remove(key);
    }

    public void clearModuleCache() {
        this.moduleCache.clear();
    }

    public FarmType getFarmType() {
        return this.farmType;
    }

    public void toggleFarmType() {
        if (this.farmType == FarmType.MINING) {
            return;
        }
        switch (this.farmType) {
            case CROPS:
                this.farmType = FarmType.LIVESTOCK;
                break;
            case LIVESTOCK:
                this.farmType = FarmType.BOTH;
                break;
            case BOTH:
                this.farmType = FarmType.CROPS;
                break;
            default:
                break;
        }
        EpicFarming.getInstance().getDataManager().save(this);
    }

    public void setFarmType(FarmType farmType) {
        this.farmType = farmType;
        EpicFarming.getInstance().getDataManager().save(this);
    }

    public boolean isMining() {
        return this.farmType == FarmType.MINING;
    }

    /**
     * Une ferme sans système de carburant (désactivé dans la config) fonctionne toujours.
     */
    public boolean hasFuel() {
        return !Settings.FUEL_ENABLED.getBoolean() || this.fuelSeconds > 0;
    }

    public long getFuelSeconds() {
        return this.fuelSeconds;
    }

    public void setFuelSeconds(long fuelSeconds) {
        this.fuelSeconds = Math.max(0, fuelSeconds);
        this.fuelDirty = true;
    }

    /**
     * Ajoute du carburant (plafonné). @return les secondes réellement ajoutées.
     */
    public long addFuel(long seconds) {
        long max = Settings.FUEL_MAX_SECONDS.getInt();
        long before = this.fuelSeconds;
        setFuelSeconds(Math.min(max, this.fuelSeconds + seconds));
        return this.fuelSeconds - before;
    }

    public void consumeFuel(long seconds) {
        if (this.fuelSeconds <= 0) {
            return;
        }
        setFuelSeconds(this.fuelSeconds - seconds);
    }

    public boolean isFuelDirty() {
        return this.fuelDirty;
    }

    public void setFuelDirty(boolean fuelDirty) {
        this.fuelDirty = fuelDirty;
    }

    public long getTotalProduced() {
        return this.totalProduced;
    }

    public Location getLinkedLocation() {
        return this.linked == null ? null : this.linked.clone();
    }

    public void setLinkedLocation(Location linked) {
        this.linked = linked == null ? null : linked.clone();
        this.fuelDirty = true;
    }

    public int getMiningProgress() {
        return this.miningProgress;
    }

    public void setMiningProgress(int miningProgress) {
        this.miningProgress = miningProgress;
    }

    public boolean needsToBeSaved() {
        return this.needsToBeSaved;
    }

    public void setNeedsToBeSaved(boolean needsToBeSaved) {
        this.needsToBeSaved = needsToBeSaved;
    }
}
