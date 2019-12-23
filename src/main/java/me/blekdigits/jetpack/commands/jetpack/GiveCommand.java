package me.blekdigits.jetpack.commands.jetpack;

import me.blekdigits.jetpack.items.FuelItem;
import me.blekdigits.jetpack.items.JetpackItem;
import me.blekdigits.jetpack.settings.MessagesSettings;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;

import java.util.List;

public class GiveCommand extends SimpleSubCommand {
	protected GiveCommand(final SimpleCommandGroup parent) {
		super(parent, "give|get");

		setPermission("jetpack.give");
		setPermissionMessage(MessagesSettings.NO_PERMISSION);
		setMinArguments(1);
		setDescription("Gives you or other player jetpack / fuel");
		setUsage(MessagesSettings.USAGE_GIVE);
	}

	@Override
	protected void onCommand() {
		final String itemType = args[0].toLowerCase();
		int amount = 1;
		Player player = getPlayer();

		final ItemStack jetpackItem = JetpackItem.getInstance().getItem();
		final ItemStack fuelItem = FuelItem.getInstance().getItem();

		if(args.length > 1 && args[1] != null) {
			if(NumberUtils.isNumber(args[1])) {
				amount = Integer.parseInt(args[1]);
				findNumber(1, 1, 2304, MessagesSettings.AMOUNT_ERROR);
			} else {
				checkConsole();
				player = findPlayer(args[1]);
			}
		}

		if(args.length > 2 && args[2] != null) {
			player = findPlayer(args[2]);
		}

		switch (itemType) {
			case "jetpack":
				if(!hasPerm("jetpack.give.jetpack")) {
					returnTell(MessagesSettings.NO_PERMISSION);
				}

				if(player == null) {
					returnInvalidArgs();
				}

				if(player != getPlayer()) {
					tell(MessagesSettings.GIVE_SENDER.replace("{amount}", Integer.toString(amount)).replace("{item}", ItemUtil.bountifyCapitalized(itemType)).replace("{player}", player.getDisplayName()));
				}
				Common.tell(player, MessagesSettings.GIVE_RECEIVER.replace("{amount}", Integer.toString(amount)).replace("{item}", ItemUtil.bountifyCapitalized(itemType)));

				jetpackItem.setAmount(amount);
				player.getInventory().addItem(jetpackItem);

				break;
			case "fuel":
				if(!hasPerm("jetpack.give.fuel")) {
					returnTell(MessagesSettings.NO_PERMISSION);
				}

				if(player == null) {
					returnInvalidArgs();
				}

				if(player != getPlayer() ) {
					tell(MessagesSettings.GIVE_SENDER.replace("{amount}", Integer.toString(amount)).replace("{item}", ItemUtil.bountifyCapitalized(itemType)).replace("{player}", player.getDisplayName()));
				}
				Common.tell(player, MessagesSettings.GIVE_RECEIVER.replace("{amount}", Integer.toString(amount)).replace("{item}", ItemUtil.bountifyCapitalized(itemType)));

				fuelItem.setAmount(amount);
				player.getInventory().addItem(fuelItem);

				break;
			default:
				returnInvalidArgs();
		}
	}

	@Override
	public List<String> tabComplete() {
		if(args.length == 1) {
			return completeLastWord("jetpack", "fuel");
		}
		return completeLastWordPlayerNames();
	}
}
