package me.blek.commands;

import co.aikar.commands.annotation.Dependency;
import me.blek.utils.Common;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public class GetCommands {



    public GetCommands() {

    }

    public void getJetpack(Player player, Supplier<ItemStack> itemStackSupplier, int amount){

        ItemStack jetpack = itemStackSupplier.get().clone();
        jetpack.setAmount(amount);

        player.getInventory().addItem(jetpack);
    }


    public void getFuel(Player player, Supplier<ItemStack> itemStackSupplier, int amount){
        ItemStack fuel = itemStackSupplier.get().clone();
        fuel.setAmount(amount);

        player.getInventory().addItem(fuel);
    }
}
