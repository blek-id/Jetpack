package me.blekdigits.jetpack.items;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.blekdigits.jetpack.settings.ItemSettings;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.tool.Tool;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FuelItem extends Tool {

	@Getter
	private static final Tool instance = new FuelItem();

	@Override
	public ItemStack getItem() {
		final ItemSettings itemSettings = new ItemSettings();
		return itemSettings.getItem("fuel");
	}

	@Override
	protected void onBlockClick(final PlayerInteractEvent event) {

	}
}
