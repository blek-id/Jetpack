package me.blekdigits.jetpack.commands.jetpack;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleLocalization;

public class ReloadCommand extends SimpleSubCommand {

	public ReloadCommand(final SimpleCommandGroup parent) {
		super(parent,"reload|rl");

		setPermission("jetpack.reload");
		setDescription("Reload the configuration.");
	}

	@Override
	protected void onCommand() {
		try {
			SimplePlugin.getInstance().reload();
			Common.tell(sender, SimpleLocalization.Commands.RELOAD_SUCCESS.replace("{plugin_name}", SimplePlugin.getNamed()).replace("{plugin_version}", SimplePlugin.getVersion()));

		} catch (final Throwable t) {
			Common.tell(sender, SimpleLocalization.Commands.RELOAD_FAIL.replace("{error}", t.getMessage() != null ? t.getMessage() : "unknown"));

			t.printStackTrace();
		}
	}
}