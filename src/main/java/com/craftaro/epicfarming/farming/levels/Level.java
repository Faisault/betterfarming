package com.craftaro.epicfarming.farming.levels;

import com.craftaro.epicfarming.EpicFarming;
import com.craftaro.epicfarming.farming.levels.modules.Module;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Level {

    private final ArrayList<Module> registeredModules;
    private final List<String> description = new ArrayList<>();
    private final int level;
    private final int costExperience;
    private final int costEconomy;
    private final int radius;
    private final int pages;
    private final double speedMultiplier;
    private final boolean autoReplant;

    // Données des fermes minières (inutilisées pour les fermes de culture).
    private boolean mining = false;
    private int miningIntervalSeconds = 30;
    private int miningAmount = 1;
    private final Map<Material, Integer> miningOres = new LinkedHashMap<>();

    Level(int level, int costExperience, int costEconomy, double speedMultiplier, int radius, boolean autoReplant, int pages, ArrayList<Module> registeredModules) {
        this.level = level;
        this.costExperience = costExperience;
        this.costEconomy = costEconomy;
        this.speedMultiplier = speedMultiplier;
        this.radius = radius;
        this.autoReplant = autoReplant;
        this.pages = pages;
        this.registeredModules = registeredModules;

        buildDescription();
    }

    public void buildDescription() {
        EpicFarming instance = EpicFarming.getPlugin(EpicFarming.class);
        this.description.clear();

        if (this.mining) {
            this.description.add(instance.getLocale().getMessage("interface.button.mininginterval")
                    .processPlaceholder("seconds", this.miningIntervalSeconds).toText());
            this.description.add(instance.getLocale().getMessage("interface.button.miningamount")
                    .processPlaceholder("amount", this.miningAmount).toText());

            int total = this.miningOres.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<Material, Integer> ore : this.miningOres.entrySet()) {
                double percent = total == 0 ? 0 : (ore.getValue() * 100.0) / total;
                this.description.add(instance.getLocale().getMessage("interface.button.miningore")
                        .processPlaceholder("ore", ore.getKey().name().toLowerCase().replace('_', ' '))
                        .processPlaceholder("chance", String.format("%.1f", percent)).toText());
            }

            if (this.pages > 1) {
                this.description.add(instance.getLocale().getMessage("interface.button.pages")
                        .processPlaceholder("amount", this.pages).toText());
            }
            return;
        }

        this.description.add(instance.getLocale().getMessage("interface.button.radius")
                .processPlaceholder("radius", this.radius).toText());

        this.description.add(instance.getLocale().getMessage("interface.button.speed")
                .processPlaceholder("speed", this.speedMultiplier).toText());


        if (this.autoReplant) {
            this.description.add(instance.getLocale().getMessage("interface.button.autoreplant")
                    .processPlaceholder("status",
                            instance.getLocale().getMessage("general.interface.unlocked")
                                    .toText()).toText());
        }

        if (this.pages > 1) {
            this.description.add(instance.getLocale().getMessage("interface.button.pages")
                    .processPlaceholder("amount", this.pages).toText());
        }

        for (Module module : this.registeredModules) {
            this.description.add(module.getDescription());
        }
    }

    public List<String> getDescription() {
        return new ArrayList<>(this.description);
    }

    /**
     * Transforme ce niveau en niveau de ferme minière.
     */
    public void setMining(int intervalSeconds, int amount, Map<Material, Integer> ores) {
        this.mining = true;
        this.miningIntervalSeconds = Math.max(1, intervalSeconds);
        this.miningAmount = Math.max(1, amount);
        this.miningOres.clear();
        this.miningOres.putAll(ores);
        buildDescription();
    }

    public boolean isMining() {
        return this.mining;
    }

    public int getMiningIntervalSeconds() {
        return this.miningIntervalSeconds;
    }

    public int getMiningAmount() {
        return this.miningAmount;
    }

    public Map<Material, Integer> getMiningOres() {
        return new LinkedHashMap<>(this.miningOres);
    }

    public int getLevel() {
        return this.level;
    }

    public int getRadius() {
        return this.radius;
    }

    public boolean isAutoReplant() {
        return this.autoReplant;
    }

    public int getPages() {
        return this.pages;
    }

    public double getSpeedMultiplier() {
        return this.speedMultiplier;
    }

    public int getCostExperience() {
        return this.costExperience;
    }

    public int getCostEconomy() {
        return this.costEconomy;
    }

    public ArrayList<Module> getRegisteredModules() {
        return new ArrayList<>(this.registeredModules);
    }

    public void addModule(Module module) {
        this.registeredModules.add(module);
        buildDescription();
    }

    public Module getModule(String name) {
        return this.registeredModules == null ? null :
                this.registeredModules.stream().filter(module -> module.getName().equals(name)).findFirst().orElse(null);
    }
}
