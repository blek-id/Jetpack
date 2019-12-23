package me.blekdigits.jetpack.events;

import me.blekdigits.jetpack.JetpackPlugin;
import me.blekdigits.jetpack.PlayerCache;
import me.blekdigits.jetpack.items.FuelItem;
import me.blekdigits.jetpack.items.JetpackItem;
import me.blekdigits.jetpack.settings.MessagesSettings;
import me.blekdigits.jetpack.settings.Settings;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.PlayerUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

	final private ItemStack jetpackItem = JetpackItem.getInstance().getItem();
	final private ItemStack fuelItem = FuelItem.getInstance().getItem();
	final private Map<UUID, BukkitTask> runningTasks = PlayerCache.getRunningTasks();

	@EventHandler
	private void onPlayerSneak(final PlayerToggleSneakEvent event) {
		final Player player = event.getPlayer();
		if(player.isSneaking() || player.isFlying()) {
			return;
		}
		if(!isPlayerWearingJetpack(player)) {
			return;
		}
		if(!isFuelAvailable(player)) {
			Common.tell(player, MessagesSettings.NEED_FUEL);
			return;
		}

		if(!player.hasPermission("jetpack.use")) {
			Common.tell(player, MessagesSettings.NO_PERMISSION);
			return;
		}

		toggleFlight(player, !player.getAllowFlight());
	}

	@EventHandler
	private void onPlayerToggleFlight(final PlayerToggleFlightEvent event) {

		final Player player = event.getPlayer();

		if(!isPlayerWearingJetpack(player) || !isFuelAvailable(player)) {
			return;
		}

		if (player.isFlying()) {
			if(runningTasks.containsKey(player.getUniqueId())) {
				final BukkitTask currentTask = runningTasks.remove(player.getUniqueId());
				currentTask.cancel();
			}
			return;
		}

		if(runningTasks.containsKey(player.getUniqueId())) {
			return;
		}

		final BukkitTask task = Common.runTimer((int) (Settings.BURN_RATE * 20), () -> {

			if (!isFuelAvailable(player) || !isPlayerWearingJetpack(player)) {
				cancelFlying(player);
				return;
			}

			takePlayerFuel(player);
		});
		spawnParticle(player);
		runningTasks.put(player.getUniqueId(), task);
	}

	@EventHandler
	private void onPlayerInventoryInteract(final InventoryClickEvent event) {
		final Player player = (Player) event.getWhoClicked();
		checkFlightRequirements(player);
	}
	@EventHandler
	private void onPlayerDropItem(final PlayerDropItemEvent event) {
		final Player player = event.getPlayer();
		checkFlightRequirements(player);
	}

	private boolean isPlayerWearingJetpack(final Player player) {
		final List<ItemStack> playerEquipments = Arrays.asList(player.getInventory().getArmorContents());
		final ItemStack playerJetpack = playerEquipments.stream()
				.filter(playerEquipment -> ItemUtil.isSimilar(jetpackItem, playerEquipment))
				.findAny()
				.orElse(null);

		if(playerJetpack != null) {
			return true;
		}
		return false;
	}

	private boolean isFuelAvailable(final Player player) {
		final ItemStack playerFuel = PlayerUtil.getFirstItem(player, fuelItem);
		if(playerFuel != null) {
			return true;
		}
		return false;
	}

	private void takePlayerFuel(final Player player) {
		final ItemStack playerFuel = PlayerUtil.getFirstItem(player, fuelItem);
		PlayerUtil.takeOnePiece(player, playerFuel);
	}

	private void cancelFlying(final Player player) {
		toggleFlight(player, false);
		final BukkitTask currentTask = runningTasks.remove(player.getUniqueId());
		currentTask.cancel();
	}

	private void toggleFlight(final Player player, final boolean isAllowFlight) {
		player.setAllowFlight(isAllowFlight);
		Common.tell(player, (isAllowFlight ? MessagesSettings.TURN_ON : MessagesSettings.TURN_OFF));
	}

	private void checkFlightRequirements(final Player player) {
		if(!runningTasks.containsKey(player.getUniqueId())) {
			return;
		}
		if(!isPlayerWearingJetpack(player)) {
			cancelFlying(player);
		}
		if(!isFuelAvailable(player)) {
			Common.tell(player, MessagesSettings.OUT_OF_FUEL);
			cancelFlying(player);
		}
	}

	private void spawnParticle(final Player player){
		new BukkitRunnable() {
			@Override
			public void run() {
				if(player.isFlying()){
					final Location location = player.getLocation();
					final Particle particle;

					final String particleString = Settings.PARTICLE;
					particle = Particle.valueOf(particleString);

					final int amount = Settings.PARTICLE_AMOUNT;

					final float newX = (float)( 0.2 * Math.sin(Math.toRadians(player.getLocation().getYaw() + 90 * 3)));
					final float newZ = (float)( 0.2 * Math.sin(Math.toRadians(player.getLocation().getYaw() + 90 * 3)));


					if(particleString.equals("REDSTONE")){
						final String redstoneParticle = Settings.PARTICLE_REDSTONE_COLOR;
						final String[] redstoneParticles = redstoneParticle.split(",");
						final Particle.DustOptions particleDustOptions = new Particle.DustOptions(Color.fromRGB(Integer.parseInt(redstoneParticles[0]), Integer.parseInt(redstoneParticles[1]), Integer.parseInt(redstoneParticles[2])), 1);

						player.getWorld().spawnParticle(particle, location.getX() + newX, + location.getY() + 0.8, location.getZ() + newZ, amount, 0, -0.1, 0, particleDustOptions);
					} else {
						player.getWorld().spawnParticle(particle, location.getX() + newX, + location.getY() + 0.8, location.getZ() + newZ, amount, 0, -0.1, 0);
					}

				}

				if(!player.getAllowFlight() || !player.isFlying()){
					cancel();
				}
			}
		}.runTaskTimer(JetpackPlugin.getInstance(), 0, 5);
	}
}
