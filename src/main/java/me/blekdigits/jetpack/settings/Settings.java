package me.blekdigits.jetpack.settings;

import org.mineacademy.fo.settings.SimpleSettings;

public class Settings extends SimpleSettings {
	@Override
	protected int getConfigVersion() {
		return 3;
	}

	public static Double BURN_RATE;

	public static String PARTICLE;
	public static Integer PARTICLE_AMOUNT;
	public static String PARTICLE_REDSTONE_COLOR;

	private static void init() {
		BURN_RATE = getDoubleSafe("Burn_Rate");

		PARTICLE = getString("Particle");
		PARTICLE_AMOUNT = getInteger("Particle_Amount");
		PARTICLE_REDSTONE_COLOR = getString("Particle_Redstone_Color");
	}
}
