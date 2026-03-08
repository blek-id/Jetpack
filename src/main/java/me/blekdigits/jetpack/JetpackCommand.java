package me.blekdigits.jetpack;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class JetpackCommand implements CommandExecutor {

    private final JetpackPlugin plugin;

    public JetpackCommand(JetpackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jetpack.admin")) {
            sender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                handleGive(sender, args);
                break;
            case "set":
                handleSet(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                sender.sendMessage(plugin.getMessage("usage"));
                break;
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("usage"));
            return;
        }

        String type = args[1].toLowerCase();

        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getMessage("player_not_found"));
                return;
            }
        } else {
            if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(plugin.getMessage("not_player"));
                return;
            }
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessage("invalid_number"));
                return;
            }
        }

        if (type.equals("fuel")) {
            ItemStack item = plugin.getFuelItem();
            item.setAmount(amount);
            target.getInventory().addItem(item);
            target.sendMessage(plugin.getMessage("give_fuel").replace("{amount}", String.valueOf(amount)));
        } else if (type.equals("jetpack")) {
            ItemStack item = plugin.getJetpackItem();
            item.setAmount(amount);
            target.getInventory().addItem(item);
            target.sendMessage(plugin.getMessage("give_jetpack").replace("{amount}", String.valueOf(amount)));
        } else {
            sender.sendMessage(plugin.getMessage("invalid_type"));
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("not_player"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("usage_set"));
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(plugin.getMessage("must_hold_item"));
            return;
        }

        String type = args[1].toLowerCase();
        ItemStack savedItem = itemInHand.clone();
        savedItem.setAmount(1);

        if (type.equals("fuel")) {
            plugin.setFuelItem(savedItem);
            player.sendMessage(plugin.getMessage("set_fuel"));
        } else if (type.equals("jetpack")) {
            plugin.setJetpackItem(savedItem);
            player.sendMessage(plugin.getMessage("set_jetpack"));
        } else {
            player.sendMessage(plugin.getMessage("invalid_type"));
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.loadPluginData();
        plugin.loadMessages();
        sender.sendMessage(plugin.getMessage("reloaded"));
    }
}