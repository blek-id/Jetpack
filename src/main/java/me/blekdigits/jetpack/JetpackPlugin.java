package me.blekdigits.jetpack;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import me.blekdigits.listeners.PlayerListener;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import net.md_5.bungee.api.ChatColor; // NOTE: Make sure this is the net.md_5 version!
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Override
    public void onEnable() {
        JETPACK_KEY = new NamespacedKey(this, "is_jetpack");
        saveDefaultConfig();
        loadPluginData();
        loadMessages();
        
        getCommand("jetpack").setExecutor(new JetpackCommand(this));
        
        // REGISTER THE LISTENER HERE
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
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

        boolean modified = false;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!messagesConfig.contains(entry.getKey())) {
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

        if (!getConfig().contains("fuel-per-durability")) {
            getConfig().set("fuel-per-durability", 5); 
        }

        if (!getConfig().contains("particle")) {
            getConfig().set("particle", "FLAME");
        }
        // Add this inside your loadPluginData() method, right under the particle setup:
        if (!getConfig().contains("fuel-burn-interval")) {
            getConfig().set("fuel-burn-interval", 1.0); // 1.0 seconds per fuel consumed
        }

        if (!getConfig().contains("unrepairable")) {
            getConfig().set("unrepairable", true); 
        }

        saveConfig(); // Save any newly created defaults to the actual file
    }

    private ItemStack createDefaultJetpack() {
        ItemStack item = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Jetpack");
            meta.getPersistentDataContainer().set(JETPACK_KEY, PersistentDataType.BYTE, (byte) 1);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Fuel: Coal");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Use shift to toggle jetpack.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Updated setters to also save to the config file
    public void setFuelItem(ItemStack item) {
        this.fuelItem = item.clone();
        getConfig().set("fuel-item", this.fuelItem);
        saveConfig();
    }

    public void setJetpackItem(ItemStack item) {
        this.jetpackItem = item.clone();
        getConfig().set("jetpack-item", this.jetpackItem);
        saveConfig();
    }
    
    public int getFuelPerDurability() {
        return getConfig().getInt("fuel-per-durability", 5);
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
        // Read the double (e.g., 1.5), multiply by 20 ticks, and cast to long
        double seconds = getConfig().getDouble("fuel-burn-interval", 1.0);
        return (long) (seconds * 20L);
    }

    public String getMessage(String path) {
        String rawMessage = messagesConfig.getString(path, "&cMessage missing: " + path);
        
        if (!path.equals("prefix")) {
            String prefix = messagesConfig.getString("prefix", "");
            rawMessage = prefix + rawMessage;
        }
        
        // 2. Parse Hex Codes (Format: {#RRGGBB})
        Pattern pattern = Pattern.compile("\\{#([a-fA-F0-9]{6})\\}");
        Matcher matcher = pattern.matcher(rawMessage);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = "#" + matcher.group(1);
            net.md_5.bungee.api.ChatColor color = net.md_5.bungee.api.ChatColor.of(hex);
            matcher.appendReplacement(buffer, color.toString());
        }
        matcher.appendTail(buffer);
        
        // 3. Parse Legacy Codes (Format: &a, &l, etc.)
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public ItemStack getFuelItem() { return fuelItem.clone(); }
    public ItemStack getJetpackItem() { return jetpackItem.clone(); }
}