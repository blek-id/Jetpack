package me.blekdigits;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JetpackCommand implements CommandExecutor {

    private final JetpackPlugin plugin;

    public JetpackCommand(JetpackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Check if there are any arguments (e.g., /jetpack <arg>)
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 2. Route the sub-commands (Like a switch statement in JS)
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
        // We need at least: /jetpack give <type> (Length of 2)
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("usage"));
            return;
        }

        String type = args[1].toLowerCase(); // fuel or jetpack

        // 1. Determine the Target Player
        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]); // This is where we use 'plugin'!
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

        // 2. Determine the Amount
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessage("invalid_number"));
                return;
            }
        }

        // 3. The "Placeholder" logic for the items
        if (type.equals("fuel")) {
            target.sendMessage(plugin.getMessage("give_fuel").replace("{amount}", String.valueOf(amount)));
            // We will add the actual item giving logic in the next step
        } else if (type.equals("jetpack")) {
            target.sendMessage(plugin.getMessage("give_jetpack"));
        } else {
            sender.sendMessage(plugin.getMessage("invalid_type"));
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("not_player"));
            return;
        }
        player.sendMessage(plugin.getMessage("set_item"));
    }

    private void handleReload(CommandSender sender) {
        plugin.loadPluginData();
        sender.sendMessage(plugin.getMessage("reloaded"));
    }
}