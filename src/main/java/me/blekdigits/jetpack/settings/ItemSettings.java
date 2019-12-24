package me.blekdigits.jetpack.settings;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.settings.YamlSectionConfig;

import java.util.List;
import java.util.stream.Collectors;

public class ItemSettings extends YamlSectionConfig {

	public ItemSettings() {
		super("items");

		setHeader("Please edit these values through the in-game command!");
		loadConfiguration("items.yml");
	}

	public void setItem(final String path, final ItemStack value) {

		final ItemMeta itemMeta = value.getItemMeta();

		itemMeta.setDisplayName(itemMeta.getDisplayName().replace('§', '&'));

		List<String> lores = itemMeta.getLore();
		if(lores != null) {
			lores = lores.stream().map((lore) -> lore.replace("§", "&")).collect(Collectors.toList());
		}
		itemMeta.setLore(lores);

		value.setItemMeta(itemMeta);
		save(path, value);
		save();
	}

	public ItemStack getItem(final String path) {
		return (ItemStack) getObject(path);
	}
}
