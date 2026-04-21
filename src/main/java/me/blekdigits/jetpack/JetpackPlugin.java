package me.blekdigits.jetpack;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import me.blekdigits.listeners.JetpackMenuListener;
import me.blekdigits.listeners.PlayerListener;
import me.blekdigits.utils.Utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JetpackPlugin extends JavaPlugin {

    private ItemStack fuelItem;
    private ItemStack jetpackItem;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    public static NamespacedKey JETPACK_KEY;
    public static NamespacedKey SPEED_LEVEL_KEY;
    public static NamespacedKey EFFICIENCY_LEVEL_KEY;
    public static NamespacedKey DURABILITY_LEVEL_KEY;
    private JetpackService jetpackService;

    @Override
    public void onEnable() {
        JETPACK_KEY = new NamespacedKey(this, "is_jetpack");
        SPEED_LEVEL_KEY = new NamespacedKey(this, "jetpack_speed_level");
        EFFICIENCY_LEVEL_KEY = new NamespacedKey(this, "jetpack_efficiency_level");
        DURABILITY_LEVEL_KEY = new NamespacedKey(this, "jetpack_durability_level");
        jetpackService = new JetpackService(this);

        saveDefaultConfig();
        loadPluginData();
        loadMessages();
        
        if (getCommand("jetpack") != null) {
            getCommand("jetpack").setExecutor(new JetpackCommand(this));
        }
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new JetpackMenuListener(this), this);
        
        getLogger().info("Jetpack Plugin enabled!");
    }

    public void loadMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false); 
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // List of all required keys and their default values
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("prefix", "{#FFaa00}[Jetpack] &r");
        defaults.put("usage", "{#FF5555}Usage: /jetpack <give|set|reload>");
        defaults.put("no_permission", "{#FF5555}You don't have permission to do that.");
        defaults.put("not_player", "{#FF5555}Only players can use this command.");
        defaults.put("player_not_found", "{#FF5555}Player not found.");
        defaults.put("invalid_number", "{#FF5555}Amount must be a valid number.");
        defaults.put("invalid_type", "{#FF5555}Invalid type. Use 'fuel' or 'jetpack'.");
        defaults.put("give_fuel", "{#55FF55}You received {amount} jetpack fuel!");
        defaults.put("give_jetpack", "{#55FF55}You received a jetpack!");
        defaults.put("set_fuel", "{#55FF55}Current item set as Jetpack Fuel!");
        defaults.put("set_jetpack", "{#55FF55}Current item set as the Jetpack!");
        defaults.put("reloaded", "{#55FF55}Configuration and messages reloaded!");
        defaults.put("flight_enabled", "{#55FF55}Jetpack activated! Double jump to fly.");
        defaults.put("flight_disabled", "{#FF5555}Jetpack deactivated.");
        defaults.put("out_of_fuel", "{#FF5555}You are out of fuel!");
        defaults.put("jetpack_removed", "{#FF5555}Jetpack removed! Flight disabled.");
        defaults.put("jetpack_broken", "{#FF5555}&l[!] &cYour jetpack has completely burned out and broken!");
        defaults.put("cannot_repair_jetpack", "{#FFAA00}&l[!] &eThis jetpack is too complex to be repaired or enchanted.");
        defaults.put("cannot_grind", "{#FFAA00}&eYou cannot use a grindstone on experimental jetpack technology.");
        defaults.put("no_upgrade_permission", "{#FF5555}You do not have permission to upgrade jetpacks.");
        defaults.put("no_repair_permission", "{#FF5555}You do not have permission to repair jetpacks.");
        defaults.put("menu_title", "{#55AAFF}Jetpack Upgrade & Repair");
        defaults.put("menu_speed", "{#55AAFF}Upgrade Speed");
        defaults.put("menu_efficiency", "{#55FF55}Upgrade Efficiency");
        defaults.put("menu_durability", "{#FFAA00}Upgrade Durability");
        defaults.put("menu_repair", "{#AAAAFF}Repair Armor");
        defaults.put("upgrade_success", "{#55FF55}Upgraded {type} to level {level}!");
        defaults.put("upgrade_maxed", "{#FFAA00}{type} is already at max level.");
        defaults.put("upgrade_missing_material", "{#FF5555}You need {cost}.");
        defaults.put("repair_success", "{#55FF55}Jetpack repaired by {amount} durability.");
        defaults.put("repair_not_damaged", "{#FFAA00}Your jetpack does not need repair.");
        defaults.put("repair_not_damageable", "{#FF5555}This jetpack cannot be repaired.");
        defaults.put("menu_invalid_item", "{#FF5555}Hold the same jetpack in your main hand to use this menu.");
        defaults.put("menu_next_level", "&7Next: &fLvl {level}");
        defaults.put("menu_cost", "&7Cost: &f{cost}");
        defaults.put("menu_current_value", "&7Current: &f{value}");
        defaults.put("menu_maxed", "&aMAX LEVEL");

        boolean modified = false;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String current = messagesConfig.getString(entry.getKey());
            if (current == null || !current.equals(entry.getValue())) {
                messagesConfig.set(entry.getKey(), entry.getValue());
                modified = true;
            }
        }

        if (modified) {
            try {
                messagesConfig.save(messagesFile);
            } catch (IOException e) {
                getLogger().severe("Could not sync messages.yml!");
            }
        }
    }

    public void loadPluginData() {
        // Reloads the file from disk into memory
        reloadConfig(); 

        // 1. Try to load Fuel
        if (getConfig().contains("fuel-item")) {
            fuelItem = getConfig().getItemStack("fuel-item");
        } else {
            fuelItem = new ItemStack(Material.COAL);
            getConfig().set("fuel-item", fuelItem);
        }

        // 2. Try to load Jetpack
        if (getConfig().contains("jetpack-item")) {
            jetpackItem = getConfig().getItemStack("jetpack-item");
        } else {
            jetpackItem = createDefaultJetpack();
            getConfig().set("jetpack-item", jetpackItem);
        }

        markAsJetpack(jetpackItem);
        applyJetpackLore(jetpackItem);
        getConfig().set("jetpack-item", jetpackItem);

        if (!getConfig().contains("fuel-per-durability")) {
            getConfig().set("fuel-per-durability", 5);
        }

        if (!getConfig().contains("particle")) {
            getConfig().set("particle", "FLAME");
        }

        if (!getConfig().contains("fuel-burn-interval")) {
            getConfig().set("fuel-burn-interval", 1.0);
        }

        if (!getConfig().contains("base-fly-speed")) {
            getConfig().set("base-fly-speed", 0.1);
        }

        if (!getConfig().contains("unrepairable")) {
            getConfig().set("unrepairable", true);
        }

        if (!getConfig().contains("upgrades.speed.tiers.1.value")) {
            getConfig().set("upgrades.speed.tiers.1.value", 0.12);
            getConfig().set("upgrades.speed.tiers.1.cost.provider", "MATERIAL");
            getConfig().set("upgrades.speed.tiers.1.cost.material", "IRON_INGOT");
            getConfig().set("upgrades.speed.tiers.1.cost.amount", 8);
            getConfig().set("upgrades.speed.tiers.2.value", 0.15);
            getConfig().set("upgrades.speed.tiers.2.cost.provider", "MATERIAL");
            getConfig().set("upgrades.speed.tiers.2.cost.material", "GOLD_INGOT");
            getConfig().set("upgrades.speed.tiers.2.cost.amount", 10);
            getConfig().set("upgrades.speed.tiers.3.value", 0.18);
            getConfig().set("upgrades.speed.tiers.3.cost.provider", "MATERIAL");
            getConfig().set("upgrades.speed.tiers.3.cost.material", "DIAMOND");
            getConfig().set("upgrades.speed.tiers.3.cost.amount", 6);
        }

        if (!getConfig().contains("upgrades.efficiency.tiers.1.value")) {
            getConfig().set("upgrades.efficiency.tiers.1.value", 1.2);
            getConfig().set("upgrades.efficiency.tiers.1.cost.provider", "MATERIAL");
            getConfig().set("upgrades.efficiency.tiers.1.cost.material", "REDSTONE");
            getConfig().set("upgrades.efficiency.tiers.1.cost.amount", 16);
            getConfig().set("upgrades.efficiency.tiers.2.value", 1.4);
            getConfig().set("upgrades.efficiency.tiers.2.cost.provider", "MATERIAL");
            getConfig().set("upgrades.efficiency.tiers.2.cost.material", "LAPIS_LAZULI");
            getConfig().set("upgrades.efficiency.tiers.2.cost.amount", 20);
            getConfig().set("upgrades.efficiency.tiers.3.value", 1.6);
            getConfig().set("upgrades.efficiency.tiers.3.cost.provider", "MATERIAL");
            getConfig().set("upgrades.efficiency.tiers.3.cost.material", "EMERALD");
            getConfig().set("upgrades.efficiency.tiers.3.cost.amount", 8);
        }

        if (!getConfig().contains("upgrades.durability.tiers.1.value")) {
            getConfig().set("upgrades.durability.tiers.1.value", 1.25);
            getConfig().set("upgrades.durability.tiers.1.cost.provider", "MATERIAL");
            getConfig().set("upgrades.durability.tiers.1.cost.material", "IRON_BLOCK");
            getConfig().set("upgrades.durability.tiers.1.cost.amount", 4);
            getConfig().set("upgrades.durability.tiers.2.value", 1.5);
            getConfig().set("upgrades.durability.tiers.2.cost.provider", "MATERIAL");
            getConfig().set("upgrades.durability.tiers.2.cost.material", "DIAMOND_BLOCK");
            getConfig().set("upgrades.durability.tiers.2.cost.amount", 2);
            getConfig().set("upgrades.durability.tiers.3.value", 2.0);
            getConfig().set("upgrades.durability.tiers.3.cost.provider", "MATERIAL");
            getConfig().set("upgrades.durability.tiers.3.cost.material", "NETHERITE_INGOT");
            getConfig().set("upgrades.durability.tiers.3.cost.amount", 1);
        }

        if (!getConfig().contains("repair.mode")) {
            getConfig().set("repair.mode", "PERCENT");
            getConfig().set("repair.amount", 15.0);
            getConfig().set("repair.cost.provider", "MATERIAL");
            getConfig().set("repair.cost.material", "IRON_INGOT");
            getConfig().set("repair.cost.amount", 6);
        }

        for (JetpackService.UpgradeType type : JetpackService.UpgradeType.values()) {
            String basePath = "upgrades." + type.getPath() + ".tiers";
            if (getConfig().isConfigurationSection(basePath)) {
                for (String key : getConfig().getConfigurationSection(basePath).getKeys(false)) {
                    String costPath = basePath + "." + key + ".cost";
                    if (!getConfig().contains(costPath + ".provider")) {
                        getConfig().set(costPath + ".provider", "MATERIAL");
                    }
                }
            }
        }

        if (!getConfig().contains("repair.cost.provider")) {
            getConfig().set("repair.cost.provider", "MATERIAL");
        }

        if (!getConfig().contains("jetpack-lore")) {
            List<String> defaultLore = new ArrayList<>();
            defaultLore.add("&7Fuel: &f{fuel_name}");
            defaultLore.add("&7Speed Level: &f{speed_level} &8(&7{speed_value}&8)");
            defaultLore.add("&7Efficiency Level: &f{efficiency_level} &8(&7x{efficiency_value}&8)");
            defaultLore.add("&7Durability Level: &f{durability_level} &8(&7x{durability_value}&8)");
            defaultLore.add("&8Shift + Right Click while holding to upgrade.");
            getConfig().set("jetpack-lore", defaultLore);
        }

        saveConfig();
    }

    private ItemStack createDefaultJetpack() {
        ItemStack item = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.colorize("&aJetpack"));
            meta.getPersistentDataContainer().set(JETPACK_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(SPEED_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            meta.getPersistentDataContainer().set(EFFICIENCY_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            meta.getPersistentDataContainer().set(DURABILITY_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            item.setItemMeta(meta);
        }
        applyJetpackLore(item);
        return item;
    }

    public void setFuelItem(ItemStack item) {
        this.fuelItem = item.clone();
        getConfig().set("fuel-item", this.fuelItem);
        saveConfig();
    }

    public void setJetpackItem(ItemStack item) {
        this.jetpackItem = item.clone();
        markAsJetpack(this.jetpackItem);
        applyJetpackLore(this.jetpackItem);
        getConfig().set("jetpack-item", this.jetpackItem);
        saveConfig();
    }

    public void markAsJetpack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(JETPACK_KEY, PersistentDataType.BYTE, (byte) 1);
        if (!meta.getPersistentDataContainer().has(SPEED_LEVEL_KEY, PersistentDataType.INTEGER)) {
            meta.getPersistentDataContainer().set(SPEED_LEVEL_KEY, PersistentDataType.INTEGER, 0);
        }
        if (!meta.getPersistentDataContainer().has(EFFICIENCY_LEVEL_KEY, PersistentDataType.INTEGER)) {
            meta.getPersistentDataContainer().set(EFFICIENCY_LEVEL_KEY, PersistentDataType.INTEGER, 0);
        }
        if (!meta.getPersistentDataContainer().has(DURABILITY_LEVEL_KEY, PersistentDataType.INTEGER)) {
            meta.getPersistentDataContainer().set(DURABILITY_LEVEL_KEY, PersistentDataType.INTEGER, 0);
        }
        item.setItemMeta(meta);
    }

    public void applyJetpackLore(ItemStack item) {
        if (jetpackService == null || item == null || item.getType() == Material.AIR) return;
        jetpackService.updateJetpackLore(item);
    }
    
    public int getFuelPerDurability() {
        return getConfig().getInt("fuel-per-durability", 5);
    }

    public double getFuelBurnIntervalSeconds() {
        return getConfig().getDouble("fuel-burn-interval", 1.0);
    }

    public float getBaseFlySpeed() {
        return (float) getConfig().getDouble("base-fly-speed", 0.1);
    }
    
    public org.bukkit.Particle getJetpackParticle() {
        // Get the string from config, default to "FLAME" if it doesn't exist
        String particleName = getConfig().getString("particle", "FLAME");
        try {
            // Convert string to the Enum (e.g., "FLAME" -> Particle.FLAME)
            return org.bukkit.Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid particle name in config! Defaulting to FLAME.");
            return org.bukkit.Particle.FLAME;
        }
    }

    public boolean isUnrepairable() {
        return getConfig().getBoolean("unrepairable", true);
    }
    
    // Add this new getter method anywhere in the class:
    public long getFuelBurnRate() {
        double seconds = getFuelBurnIntervalSeconds();
        return (long) (seconds * 20L);
    }

    public String getMessage(String path) {
        String message = getRawMessage(path);
        if (!path.equals("prefix")) {
            message = getRawMessage("prefix") + message;
        }
        return Utils.colorize(message);
    }

    public String getRawMessage(String path) {
        return messagesConfig.getString(path, "&cMessage missing: " + path);
    }

    public ItemStack getFuelItem() { return fuelItem.clone(); }
    public ItemStack getJetpackItem() {
        ItemStack clone = jetpackItem.clone();
        markAsJetpack(clone);
        applyJetpackLore(clone);
        return clone;
    }

    public JetpackService getJetpackService() {
        return jetpackService;
    }
}