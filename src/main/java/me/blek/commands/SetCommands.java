package me.blek.commands;

import co.aikar.commands.annotation.Dependency;
import me.blek.jetpack.Jetpack;
import me.blek.utils.Common;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SetCommands {

    public SetCommands(){
    }

    public void onSetJetpackCommand(Player player, FileConfiguration configuration){
        ItemStack handItem = player.getInventory().getItemInMainHand().clone();

        if(handItem.getType() == Material.AIR){

            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.hand-air")));
            return;
        }

        if(!handItem.getType().name().contains("CHESTPLATE")){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.hand-no-chestplate")));
        }

        handItem.setAmount(1);
        configuration.set("jetpack-item", handItem);

        player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.commands.jetpack.set")));
    }


    public void onSetFuelCommand(Player player, FileConfiguration configuration){
        ItemStack handItem = player.getInventory().getItemInMainHand().clone();

        if(handItem.getType() == Material.AIR){

            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.hand-air")));
            return;
        }

        handItem.setAmount(1);
        configuration.set("fuel-item", handItem);

        player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.commands.fuel.set")));
    }
}
