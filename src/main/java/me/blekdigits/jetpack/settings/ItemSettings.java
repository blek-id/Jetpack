package me.blekdigits.jetpack.settings;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.settings.YamlSectionConfig;

public class ItemSettings extends YamlSectionConfig {

	public ItemSettings() {
		super("items");

		setHeader("Please edit these values through the in-game command!");
		loadConfiguration("items.yml");
	}

	public void setItem(final String path, final Object value) {
		save(path, value);
	}

	public ItemStack getItem(final String path) {
		return (ItemStack) getObject(path);
	}
}
