package me.blekdigits.jetpack.settings;

import org.mineacademy.fo.settings.YamlStaticConfig;

public class MessagesSettings extends YamlStaticConfig {

	@Override
	protected void load() throws Exception {
		createFileAndLoad("messages.yml");
	}

	public static String NO_PERMISSION;
	public static String HAND_AIR;
	public static String HAND_NOT_ARMOR;
	public static String AMOUNT_ERROR;

	public static String TURN_ON;
	public static String TURN_OFF;
	public static String NEED_FUEL;
	public static String OUT_OF_FUEL;

	public static String GIVE_SENDER;
	public static String GIVE_RECEIVER;
	public static String SET_SUCCESS;

	public static String USAGE_GIVE;
	public static String USAGE_SET;

	private static void init() {
		pathPrefix("messages");

		NO_PERMISSION = getString("no-permission");
		HAND_AIR = getString("hand-air");
		HAND_NOT_ARMOR = getString("hand-not-armor");
		AMOUNT_ERROR = getString("amount-error");

		pathPrefix("messages.jetpack");

		TURN_ON = getString("turn-on");
		TURN_OFF = getString("turn-off");
		NEED_FUEL = getString("need-fuel");
		OUT_OF_FUEL = getString("out-of-fuel");

		pathPrefix("messages.commands");

		GIVE_SENDER = getString("give-sender");
		GIVE_RECEIVER = getString("give-receiver");
		SET_SUCCESS = getString("set-success");

		pathPrefix("messages.usage");

		USAGE_GIVE = getString("give");
		USAGE_SET = getString("set");


	}
}
