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

import java.util.Arrays;
import java.util.List;

public class JetpackItem implements ItemStackFactory {


    private final FileConfiguration configuration;

    public JetpackItem(FileConfiguration configuration){
        this.configuration = configuration;
    }

    @Override
    public ItemStack create() {

        ItemStack jetpack = configuration.getItemStack("jetpack-item");

        if(jetpack == null){

            jetpack = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
            ItemMeta meta = jetpack.getItemMeta();
            meta.setDisplayName(Common.colorize("&3Jetpack"));
            meta.setLore(Arrays.asList(
                    Common.colorize("&7Sneak to toggle on/off"),
                    Common.colorize("&7Double-tap space to fly")));

            jetpack.setItemMeta(meta);

        } else {

            if(jetpack.hasItemMeta()){

                ItemMeta meta = jetpack.getItemMeta();

                if(meta.hasDisplayName()){

                    meta.setDisplayName(Common.colorize(meta.getDisplayName()));
                }

                if(meta.hasLore()){

                    List<String> lores = meta.getLore();
                    lores.replaceAll(string -> ChatColor.translateAlternateColorCodes('&', string));
                    meta.setLore(lores);
                    jetpack.setItemMeta(meta);
                }
            }
        }

        return jetpack;
    }
}
