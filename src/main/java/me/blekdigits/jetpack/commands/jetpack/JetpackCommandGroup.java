package me.blekdigits.jetpack.commands.jetpack;

import org.mineacademy.fo.command.SimpleCommandGroup;

public class JetpackCommandGroup extends SimpleCommandGroup {
	@Override
	protected void registerSubcommands() {
		registerSubcommand(new SetCommand(this));
		registerSubcommand(new GiveCommand(this));
		registerSubcommand(new ReloadCommand(this));
	}
}
