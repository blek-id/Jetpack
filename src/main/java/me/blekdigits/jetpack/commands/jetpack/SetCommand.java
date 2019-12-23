package me.blekdigits.jetpack.commands.jetpack;

import me.blekdigits.jetpack.settings.ItemSettings;
import me.blekdigits.jetpack.settings.MessagesSettings;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.remain.CompMaterial;

import java.util.List;

public class SetCommand extends SimpleSubCommand {
	protected SetCommand(final SimpleCommandGroup parent) {
		super(parent, "set");

		setPermission("jetpack.set");
		setPermissionMessage(MessagesSettings.NO_PERMISSION);
		setMinArguments(1);
		setDescription("Set current item in hand into fuel / jetpack");
		setUsage(MessagesSettings.USAGE_SET);
	}

	@Override
	protected void onCommand() {
		checkConsole();

		final String itemType = args[0];
		final ItemStack mainhandItem = getPlayer().getInventory().getItemInMainHand();
		final ItemSettings itemSettings = new ItemSettings();

		if(CompMaterial.isAir(mainhandItem.getType())) {
			returnTell(MessagesSettings.HAND_AIR);
		}

		switch(itemType) {
			case "jetpack":
				if(!hasPerm("jetpack.set.jetpack")) {
					returnTell(MessagesSettings.NO_PERMISSION);
				}

				if(!isArmor(mainhandItem)) {
					returnTell(MessagesSettings.HAND_NOT_ARMOR);
				}

				itemSettings.setItem(itemType, mainhandItem);
				tell(MessagesSettings.SET_SUCCESS.replace("{item}", ItemUtil.bountifyCapitalized(itemType)));
				break;
			case "fuel":
				if(!hasPerm("jetpack.set.fuel")) {
					returnTell(MessagesSettings.NO_PERMISSION);
				}

				itemSettings.setItem(itemType, mainhandItem);
				tell(MessagesSettings.SET_SUCCESS.replace("{item}", ItemUtil.bountifyCapitalized(itemType)));
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
		return null;
	}

	private boolean isArmor(final ItemStack itemStack) {
		if (itemStack == null)
			return false;
		final String typeNameString = itemStack.getType().name();
		return typeNameString.endsWith("_HELMET")
				|| typeNameString.endsWith("_CHESTPLATE")
				|| typeNameString.endsWith("_LEGGINGS")
				|| typeNameString.endsWith("_BOOTS");
	}
}
