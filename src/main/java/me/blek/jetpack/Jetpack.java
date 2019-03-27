package me.blek.jetpack;

import co.aikar.commands.PaperCommandManager;
import me.blek.commands.JetpackBaseCommands;
import me.blek.events.JetpackListener;
import me.blek.items.FuelItem;
import me.blek.items.JetpackItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

public final class Jetpack extends JavaPlugin {

    private static Jetpack plugin;
    private static PaperCommandManager commandManager;
    public Supplier<ItemStack> jetpackSupplier;
    public Supplier<ItemStack> fuelSupplier;
    public FileConfiguration configuration;

    @Override
    public void onEnable() {
        plugin = this;

        this.saveDefaultConfig();
        configuration = this.getConfig();
        commandManager = new PaperCommandManager(this);

        commandManager.registerDependency(FileConfiguration.class, "config", configuration);
        registerSupplier();
        registerCommands();

        Bukkit.getPluginManager().registerEvents(new JetpackListener(plugin), plugin);

    }

    private void registerSupplier(){

        JetpackItem jetpackItem = new JetpackItem(configuration);
        jetpackSupplier = () -> jetpackItem.create();
        commandManager.registerDependency(Supplier.class, "jetpack", jetpackSupplier);
        
        FuelItem fuelItem = new FuelItem(configuration);
        fuelSupplier = () -> fuelItem.create();
        commandManager.registerDependency(Supplier.class, "fuel", fuelSupplier);
    }

    private void registerCommands(){
        commandManager.registerCommand(new JetpackBaseCommands());
    }

    @Override
    public void onDisable() {
        saveConfig();
    }


    public void onReload(){
        HandlerList.unregisterAll(this);

        reloadConfig();

        commandManager = new PaperCommandManager(this);
        configuration = this.getConfig();

        registerSupplier();
        registerCommands();

        Bukkit.getPluginManager().registerEvents(new JetpackListener(plugin), plugin);
    }
}
