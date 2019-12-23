package me.blekdigits.jetpack;

import me.blekdigits.jetpack.commands.jetpack.JetpackCommandGroup;
import me.blekdigits.jetpack.events.PlayerListener;
import me.blekdigits.jetpack.settings.MessagesSettings;
import me.blekdigits.jetpack.settings.Settings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.YamlStaticConfig;

import java.util.Arrays;
import java.util.List;

public final class JetpackPlugin extends SimplePlugin {
	@Override
	protected void onPluginStart() {
		registerCommands("jetpack|jp", new JetpackCommandGroup());
		registerEvents(new PlayerListener());

		Common.ADD_TELL_PREFIX = true;
	}

	@Override
	public List<Class<? extends YamlStaticConfig>> getSettings() {
		return Arrays.asList(Settings.class, MessagesSettings.class);
	}
}
