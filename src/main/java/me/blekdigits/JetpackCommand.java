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
            sender.sendMessage("§cUsage: /jetpack <give|set|reload>");
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
                sender.sendMessage("§cUnknown sub-command.");
                break;
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        // We need at least: /jetpack give <type> (Length of 2)
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jetpack give <fuel|jetpack> [player] [amount]");
            return;
        }

        String type = args[1].toLowerCase(); // fuel or jetpack

        // 1. Determine the Target Player
        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]); // This is where we use 'plugin'!
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return;
            }
        } else {
            if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage("§cConsole must specify a player name.");
                return;
            }
        }

        // 2. Determine the Amount
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cAmount must be a number.");
                return;
            }
        }

        // 3. The "Placeholder" logic for the items
        if (type.equals("fuel")) {
            target.sendMessage("§aReceived " + amount + " jetpack fuel!");
            // We will add the actual item giving logic in the next step
        } else if (type.equals("jetpack")) {
            target.sendMessage("§aReceived a jetpack!");
        } else {
            sender.sendMessage("§cInvalid type. Use 'fuel' or 'jetpack'.");
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use 'set'.");
            return;
        }
        player.sendMessage("§aItem in hand set as Jetpack/Fuel!");
    }

    private void handleReload(CommandSender sender) {
        plugin.loadPluginData();
        sender.sendMessage("§e[Jetpack] Configuration reloaded from config.yml!");
    }
}