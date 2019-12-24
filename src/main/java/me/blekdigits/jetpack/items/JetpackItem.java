package me.blekdigits.jetpack.items;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.blekdigits.jetpack.settings.ItemSettings;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JetpackItem extends Tool implements Listener {

	@Getter
	private static final Tool instance = new JetpackItem();

	@Override
	public ItemStack getItem() {
		final ItemSettings itemSettings = new ItemSettings();
		final ItemStack item = itemSettings.getItem("jetpack");

		if(item.hasItemMeta()) {
			final ItemMeta itemMeta = item.getItemMeta();

			final ItemCreator.ItemCreatorBuilder itemBuilder = ItemCreator.of(item);
			if(itemMeta.hasDisplayName()) {
				itemBuilder.name(itemMeta.getDisplayName());
			}
			if(itemMeta.hasLore()) {
				itemBuilder.lores(itemMeta.getLore());
			}
			return itemBuilder.build().make();
		}

		return ItemCreator.of(item).build().make();
	}

	@Override
	protected void onBlockClick(final PlayerInteractEvent event) {

	}
}
