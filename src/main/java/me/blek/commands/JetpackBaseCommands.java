package me.blek.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import me.blek.events.JetpackListener;
import me.blek.jetpack.Jetpack;
import me.blek.utils.Common;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;


@CommandAlias("jp|jetpack|jpack")
public class JetpackBaseCommands extends BaseCommand {

    private final GetCommands getCommands;
    private final SetCommands setCommands;

    @Dependency
    private Jetpack plugin;
    @Dependency("config")
    private FileConfiguration configuration;
    @Dependency("jetpack")
    private Supplier<ItemStack> jetpackSupplier;
    @Dependency("fuel")
    private Supplier<ItemStack> fuelSupplier;

    public JetpackBaseCommands(){
        getCommands = new GetCommands();
        setCommands = new SetCommands();
    }

    @Subcommand("get jetpack")
    public void onGetJetpack(Player player, @Default("1")String amount){

        if(!player.hasPermission("jetpack.get.jetpack")) {
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.no-permission")));
            return;
        }

        if(!StringUtils.isNumeric(amount)){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.not-numeric")));
            return;
        }

        int amountNumeric = Integer.parseInt(amount);

        if(amountNumeric > 2304){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.amount-overload")));
        }

        String getMessage = configuration.getString("messages.prefix") + " " + configuration.getString("messages.commands.jetpack.give-obtain");
        getMessage = getMessage.replace("{amount}", Integer.toString(amountNumeric));

        getCommands.getJetpack(player, jetpackSupplier, amountNumeric);

        player.sendMessage(Common.colorize(getMessage));
    }

    @Subcommand("give jetpack")
    public void onGiveJetpack(Player player, String targetPlayer, @Default("1")String amount){

        Player onlinePlayer = Bukkit.getServer().getPlayer(targetPlayer);

        if(onlinePlayer == null){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.player-not-found")));
            return;
        }

        if(!player.hasPermission("jetpack.give.jetpack")) {
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.no-permission")));
            return;
        }

        if(!StringUtils.isNumeric(amount)){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.not-numeric")));
            return;
        }

        int amountNumeric = Integer.parseInt(amount);

        if(amountNumeric > 2304){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.amount-overload")));
        }

        String giveMessage = configuration.getString("messages.prefix") + " " + configuration.getString("messages.commands.jetpack.give-sender");
        giveMessage = giveMessage.replace("{player}", onlinePlayer.getPlayer().getDisplayName()).replace("{amount}", Integer.toString(amountNumeric));

        String getMessage = configuration.getString("messages.prefix") + " " + configuration.getString("messages.commands.jetpack.give-obtain");
        getMessage = getMessage.replace("{amount}", Integer.toString(amountNumeric));

        getCommands.getJetpack(onlinePlayer.getPlayer(), jetpackSupplier, amountNumeric);

        player.sendMessage(Common.colorize(giveMessage));
        onlinePlayer.getPlayer().sendMessage(Common.colorize(getMessage));
    }

    @Subcommand("get fuel")
    public void onGetFuel(Player player, @Default("1")String amount){

        if(!player.hasPermission("jetpack.get.fuel")) {
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.no-permission")));
            return;
        }

        if (!StringUtils.isNumeric(amount)){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.not-numeric")));
            return;
        }

        int amountNumeric = Integer.parseInt(amount);

        if(amountNumeric > 2304){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.amount-overload")));
        }

        String getMessage = configuration.getString("messages.prefix") + " " + configuration.getString("messages.commands.fuel.give-obtain");
        getMessage = getMessage.replace("{amount}", Integer.toString(amountNumeric));

        getCommands.getFuel(player, fuelSupplier, amountNumeric);
        player.sendMessage(Common.colorize(getMessage));
    }

    @Subcommand("give fuel")
    public void onGiveFuel(Player player, String targetPlayer, @Default("1")String amount){

        Player onlinePlayer = Bukkit.getServer().getPlayer(targetPlayer);

        if(onlinePlayer == null){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.player-not-found")));
            return;
        }

        if(!player.hasPermission("jetpack.give.fuel")) {
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.no-permission")));
            return;
        }

        if(!StringUtils.isNumeric(amount)){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.not-numeric")));
            return;
        }

        int amountNumeric = Integer.parseInt(amount);

        if(amountNumeric > 2304){
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.amount-overload")));
        }

        String giveMessage = configuration.getString("messages.prefix") + " " + configuration.getString("messages.commands.fuel.give-sender");
        giveMessage = giveMessage.replace("{player}", onlinePlayer.getPlayer().getDisplayName()).replace("{amount}", Integer.toString(amountNumeric));

        String getMessage = configuration.getString("messages.prefix") + " " + configuration.getString("messages.commands.fuel.give-obtain");
        getMessage = getMessage.replace("{amount}", Integer.toString(amountNumeric));

        getCommands.getFuel(onlinePlayer.getPlayer(), fuelSupplier, amountNumeric);
        player.sendMessage(Common.colorize(giveMessage));
        onlinePlayer.getPlayer().sendMessage(Common.colorize(getMessage));
    }


    @Subcommand("set jetpack")
    public void onSetJetpack(Player player){

        if(!player.hasPermission("jetpack.set")) {
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.no-permission")));
            return;
        }

        setCommands.onSetJetpackCommand(player, configuration);
    }

    @Subcommand("set fuel")
    public void onSetFuel(Player player){

        if(!player.hasPermission("jetpack.set")) {
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.no-permission")));
            return;
        }

        setCommands.onSetFuelCommand(player, configuration);
    }

    @Subcommand("reload")
    public void OnReload(Player player){

        if(!player.hasPermission("jetpack.reload")) {
            player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.no-permission")));
            return;
        }

        plugin.onReload();

        player.sendMessage(Common.colorize(configuration.getString("messages.prefix") + " " + configuration.getString("messages.commands.reload")));
    }
}
