package com.craftaro.epicfarming.settings;

import com.craftaro.core.compatibility.CompatibleMaterial;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.core.configuration.Config;
import com.craftaro.core.configuration.ConfigSetting;
import com.craftaro.epicfarming.EpicFarming;

import java.util.Arrays;

public class Settings {
    static final Config CONFIG = EpicFarming.getPlugin(EpicFarming.class).getCoreConfig();

    public static final ConfigSetting UPGRADE_WITH_XP = new ConfigSetting(CONFIG, "Main.Upgrade With XP", true,
            "Peut-on améliorer les fermes avec de l'XP ?");

    public static final ConfigSetting PARTICLE_TYPE = new ConfigSetting(CONFIG, "Main.Upgrade Particle Type", "SPELL_WITCH",
            "Particule affichée quand une ferme est améliorée.");

    public static final ConfigSetting FARM_TICK_SPEED = new ConfigSetting(CONFIG, "Main.Farm Tick Speed", 20,
            "Délai en ticks entre chaque cycle des fermes.");

    public static final ConfigSetting GROWTH_TICK_SPEED = new ConfigSetting(CONFIG, "Main.Growth Tick Speed", 70,
            "Délai en ticks entre chaque pousse des cultures.");

    public static final ConfigSetting FARM_BLOCK_MATERIAL = new ConfigSetting(CONFIG, "Main.Farm Block Material", ServerVersion.isServerVersionAtLeast(ServerVersion.V1_9) ? "END_ROD" : "TORCH",
            "Quel bloc sert d'item de ferme de culture ?");

    public static final ConfigSetting MINING_BLOCK_MATERIAL = new ConfigSetting(CONFIG, "Main.Mining Farm Block Material", "LODESTONE",
            "Quel bloc sert d'item de ferme minière ?");

    public static final ConfigSetting FUEL_ENABLED = new ConfigSetting(CONFIG, "Fuel.Enabled", true,
            "Les fermes consomment-elles du carburant ? Sans carburant, une ferme s'arrête.");

    public static final ConfigSetting FUEL_ITEMS = new ConfigSetting(CONFIG, "Fuel.Items", Arrays.asList(
            "COAL:3600", "CHARCOAL:3600", "COAL_BLOCK:32400", "BLAZE_ROD:10800"),
            "Items utilisables comme carburant, au format MATERIAL:secondes.",
            "Clic droit sur la ferme avec l'item en main pour la recharger.");

    public static final ConfigSetting FUEL_MAX_SECONDS = new ConfigSetting(CONFIG, "Fuel.Max Seconds", 259200,
            "Capacité maximale de carburant d'une ferme, en secondes (259200 = 72 h).");

    public static final ConfigSetting FUEL_START_SECONDS = new ConfigSetting(CONFIG, "Fuel.Start Seconds", 3600,
            "Carburant offert à une ferme fraîchement posée, en secondes.");

    public static final ConfigSetting HOLOGRAM_ENABLED = new ConfigSetting(CONFIG, "Hologram.Enabled", true,
            "Afficher le carburant restant au-dessus des fermes ?");

    public static final ConfigSetting HOLOGRAM_LINES = new ConfigSetting(CONFIG, "Hologram.Lines", Arrays.asList(
            "&6&lFerme %type% &8- &eNiv. %level%",
            "&7Propriétaire : &f%owner%",
            "&7Production : &a%produced%",
            "%fuel%",
            "%link%"),
            "Lignes de l'hologramme (de haut en bas). Une ligne vide est ignorée.",
            "Variables : %type% %level% %owner% %produced% %fuel% %link%");

    public static final ConfigSetting HOLOGRAM_LINK = new ConfigSetting(CONFIG, "Hologram.Link Format", "&7Stockage : &aLié",
            "Texte de %link% quand un conteneur est lié (vide sinon).");

    public static final ConfigSetting LINK_ENABLED = new ConfigSetting(CONFIG, "Link.Enabled", true,
            "Permettre de lier une ferme à un conteneur (trémie, coffre, baril, shulker) pour y envoyer sa production.");

    public static final ConfigSetting LINK_MAX_DISTANCE = new ConfigSetting(CONFIG, "Link.Max Distance", 128,
            "Distance maximale (en blocs, même monde) entre la ferme et le conteneur lié.");

    public static final ConfigSetting LINK_ADJACENT = new ConfigSetting(CONFIG, "Link.Adjacent Containers", true,
            "Envoyer aussi la production dans les conteneurs collés à la ferme.");

    public static final ConfigSetting LINK_TRANSFER_PER_RUN = new ConfigSetting(CONFIG, "Link.Items Per Second", 64,
            "Nombre maximal d'items transférés par seconde vers le conteneur lié.");

    public static final ConfigSetting CRIT_ENABLED = new ConfigSetting(CONFIG, "Progression.Critical Harvest.Enabled", true,
            "Récoltes critiques : chaque production a une chance d'être doublée.");

    public static final ConfigSetting CRIT_CHANCE = new ConfigSetting(CONFIG, "Progression.Critical Harvest.Chance Percent", 8,
            "Chance (en %) qu'une production soit doublée.");

    public static final ConfigSetting MILESTONES = new ConfigSetting(CONFIG, "Progression.Milestones", Arrays.asList(
            "1000;fuel;3600",
            "10000;fuel;10800",
            "50000;cmd;eco give %player% 25000",
            "250000;cmd;eco give %player% 150000"),
            "Paliers de production totale d'une ferme, au format seuil;type;valeur.",
            "Types : fuel (secondes de carburant offertes) ou cmd (commande console, %player% = propriétaire).");

    public static final ConfigSetting HOLOGRAM_FORMAT = new ConfigSetting(CONFIG, "Hologram.Format", "&6Carburant : &e%time%",
            "Texte de l'hologramme. %time% = temps restant en heures.");

    public static final ConfigSetting HOLOGRAM_EMPTY = new ConfigSetting(CONFIG, "Hologram.Empty Format", "&cSans carburant",
            "Texte de l'hologramme quand la ferme n'a plus de carburant.");

    public static final ConfigSetting NON_COMMAND_FARMS = new ConfigSetting(CONFIG, "Main.Allow Non Command Issued Farm Items", false,
            "Les blocs de ferme posés sans commande fonctionnent-ils comme des fermes ?");

    public static final ConfigSetting ANIMATE = new ConfigSetting(CONFIG, "Main.Animate", true,
            "Animer l'item produit au-dessus de la ferme ?");

    public static final ConfigSetting DISABLE_AUTO_TIL_LAND = new ConfigSetting(CONFIG, "Main.Disable Auto Til Land", false,
            "Désactiver le labour automatique autour des fermes ?");

    public static final ConfigSetting BREAKABLE_BLOCKS = new ConfigSetting(CONFIG, "Main.Breakable Blocks", Arrays.asList(
            "TALL_GRASS", "GRASS", "BLUE_ORCHID", "AZURE_BLUET",
            "ALLIUM", "POPPY", "DANDELION", "SNOW",
            "CORNFLOWER", "FERN", "LARGE_FERN", "SUNFLOWER",
            "ROSE_BUSH", "OXEYE_DAISY", "LILY_OF_THE_VALLEY", "PEONY",
            "LILAC", "WITHER_ROSE", "PINK_TULIP", "RED_TULIP",
            "WHITE_TULIP", "ORANGE_TULIP", "NETHER_SPROUTS", "GLOW_LICHEN"),
            "Blocs que les fermes peuvent casser en labourant.");

    public static final ConfigSetting XP_ICON = new ConfigSetting(CONFIG, "Interfaces.XP Icon", "EXPERIENCE_BOTTLE");

    public static final ConfigSetting GLASS_TYPE_1 = new ConfigSetting(CONFIG, "Interfaces.Glass Type 1", "GRAY_STAINED_GLASS_PANE");
    public static final ConfigSetting GLASS_TYPE_2 = new ConfigSetting(CONFIG, "Interfaces.Glass Type 2", "BLUE_STAINED_GLASS_PANE");
    public static final ConfigSetting GLASS_TYPE_3 = new ConfigSetting(CONFIG, "Interfaces.Glass Type 3", "LIGHT_BLUE_STAINED_GLASS_PANE");

    public static final ConfigSetting LANGUGE_MODE = new ConfigSetting(CONFIG, "System.Language Mode", "fr_FR",
            "Fichier de langue utilisé (fr_FR, en_US).",
            "D'autres fichiers de langue se trouvent dans le dossier du plugin.");

    public static final ConfigSetting MYSQL_ENABLED = new ConfigSetting(CONFIG, "MySQL.Enabled", false, "Mettre 'true' pour utiliser MySQL au lieu de SQLite.");
    public static final ConfigSetting MYSQL_HOSTNAME = new ConfigSetting(CONFIG, "MySQL.Hostname", "localhost");
    public static final ConfigSetting MYSQL_PORT = new ConfigSetting(CONFIG, "MySQL.Port", 3306);
    public static final ConfigSetting MYSQL_DATABASE = new ConfigSetting(CONFIG, "MySQL.Database", "your-database");
    public static final ConfigSetting MYSQL_USERNAME = new ConfigSetting(CONFIG, "MySQL.Username", "user");
    public static final ConfigSetting MYSQL_PASSWORD = new ConfigSetting(CONFIG, "MySQL.Password", "pass");
    public static final ConfigSetting MYSQL_USE_SSL = new ConfigSetting(CONFIG, "MySQL.Use SSL", false);
    public static final ConfigSetting MYSQL_POOL_SIZE = new ConfigSetting(CONFIG, "MySQL.Pool Size", 3, "Determines the number of connections the pool is using. Increase this value if you are getting timeout errors when more players online.");

    public static void setupConfig() {
        CONFIG.load();
        CONFIG.setAutoremove(true).setAutosave(true);

        // convert glass pane settings
        int color;
        if ((color = GLASS_TYPE_1.getInt(-1)) != -1) {
            CONFIG.set(GLASS_TYPE_1.getKey(), CompatibleMaterial.getGlassPaneForColor(color).name());
        }
        if ((color = GLASS_TYPE_2.getInt(-1)) != -1) {
            CONFIG.set(GLASS_TYPE_2.getKey(), CompatibleMaterial.getGlassPaneForColor(color).name());
        }
        if ((color = GLASS_TYPE_3.getInt(-1)) != -1) {
            CONFIG.set(GLASS_TYPE_3.getKey(), CompatibleMaterial.getGlassPaneForColor(color).name());
        }

        if (Settings.FARM_TICK_SPEED.getInt() == 70) {
            CONFIG.set(FARM_TICK_SPEED.getKey(), 20);
        }
        CONFIG.saveChanges();
    }
}
