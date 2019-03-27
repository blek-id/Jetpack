package me.blek.items;

import co.aikar.commands.annotation.Dependency;
import me.blek.jetpack.Jetpack;
import me.blek.utils.Common;
import me.blek.utils.ItemStackFactory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class FuelItem implements ItemStackFactory {

    @Dependency("config")
    private FileConfiguration configuration;

    public FuelItem(FileConfiguration configuration){
        this.configuration = configuration;
    }

    @Override
    public ItemStack create() {

        ItemStack fuel = configuration.getItemStack("fuel-item");

        if (fuel == null){

            fuel = new ItemStack(Material.COAL);
        } else {

            if(fuel.hasItemMeta()){

                ItemMeta meta = fuel.getItemMeta();

                if(meta.hasDisplayName()){

                    meta.setDisplayName(Common.colorize(meta.getDisplayName()));
                }

                if(meta.hasLore()){

                    List<String> lores = meta.getLore();
                    lores.replaceAll(string -> ChatColor.translateAlternateColorCodes('&', string));
                    meta.setLore(lores);
                    fuel.setItemMeta(meta);
                }
            }
        }

        return fuel;
    }
}
