package me.blekdigits;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import me.blekdigits.listeners.PlayerListener;

import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.List;

public class JetpackPlugin extends JavaPlugin {

    private ItemStack fuelItem;
    private ItemStack jetpackItem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPluginData();
        
        getCommand("jetpack").setExecutor(new JetpackCommand(this));
        
        // REGISTER THE LISTENER HERE
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().info("Jetpack Plugin enabled!");
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

        if (!getConfig().contains("particle")) {
            getConfig().set("particle", "FLAME");
        }
        // Add this inside your loadPluginData() method, right under the particle setup:
        if (!getConfig().contains("fuel-burn-interval")) {
            getConfig().set("fuel-burn-interval", 1.0); // 1.0 seconds per fuel consumed
        }

        saveConfig(); // Save any newly created defaults to the actual file
    }

    private ItemStack createDefaultJetpack() {
        ItemStack item = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Jetpack");
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
    
    // Add this new getter method anywhere in the class:
    public long getFuelBurnRate() {
        // Read the double (e.g., 1.5), multiply by 20 ticks, and cast to long
        double seconds = getConfig().getDouble("fuel-burn-interval", 1.0);
        return (long) (seconds * 20L);
    }

    public ItemStack getFuelItem() { return fuelItem.clone(); }
    public ItemStack getJetpackItem() { return jetpackItem.clone(); }
}