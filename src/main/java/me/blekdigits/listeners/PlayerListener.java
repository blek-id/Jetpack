package me.blekdigits.listeners;

import me.blekdigits.jetpack.JetpackService;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.blekdigits.jetpack.JetpackPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final JetpackPlugin plugin;
    private final JetpackService jetpackService;
    private final Map<UUID, BukkitTask> activeParticles = new HashMap<>();
    private final Map<UUID, BukkitTask> activeFlights = new HashMap<>();
    private final Map<UUID, Float> originalFlySpeeds = new HashMap<>();
    private final Map<UUID, Integer> fuelTickProgress = new HashMap<>();
    private final Map<UUID, Integer> durabilityProgress = new HashMap<>();

    public PlayerListener(JetpackPlugin plugin) {
        this.plugin = plugin;
        this.jetpackService = plugin.getJetpackService();
    }

    private void startParticleTask(Player player) {
        if (activeParticles.containsKey(player.getUniqueId())) return;

        // Run this very fast (every 2 ticks) for smooth rendering
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // If they stop flying, kill the interval
                if (!player.isFlying()) {
                    this.cancel();
                    activeParticles.remove(player.getUniqueId());
                    return;
                }

                org.bukkit.Particle particle = plugin.getJetpackParticle();
                org.bukkit.Location loc = player.getLocation();
                
                // Move the particle slightly below the player's feet
                loc.subtract(0, 0.2, 0);

                /*
                 * spawnParticle Parameters:
                 * 1. Particle type
                 * 2. Location
                 * 3. Count (amount of particles)
                 * 4,5,6. X, Y, Z spread (randomness/scatter)
                 * 7. Speed (how fast they fly away from the center)
                 */
                player.getWorld().spawnParticle(particle, loc, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 10L); // 2L = 2 ticks

        activeParticles.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking() || player.isFlying()) return;
        if (!isWearingJetpack(player)) return;
        if (!player.hasPermission("jetpack.use")) return;

        boolean currentAllowFlight = player.getAllowFlight();
        player.setAllowFlight(!currentAllowFlight);
        
        if (player.getAllowFlight()) {
            player.sendMessage(plugin.getMessage("flight_enabled"));
        } else {
            player.sendMessage(plugin.getMessage("flight_disabled"));
        }
    }

    @EventHandler
    public void onAnvilRepair(org.bukkit.event.inventory.PrepareAnvilEvent event) {
        if (!plugin.isUnrepairable()) return;

        org.bukkit.inventory.AnvilInventory inventory = event.getInventory();
        ItemStack firstItem = inventory.getItem(0); // The item in the left slot
        ItemStack secondItem = inventory.getItem(1); // The item in the middle slot (sacrifice/book)

        // If there's no item in the first slot, we don't care
        if (firstItem == null || firstItem.getType() == Material.AIR) return;

        // Check for your custom NBT tag
        org.bukkit.inventory.meta.ItemMeta meta = firstItem.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(JetpackPlugin.JETPACK_KEY, org.bukkit.persistence.PersistentDataType.BYTE)) {
            return;
        }

        // If there is an item in the second slot, they are trying to Repair or Enchant.
        // We set the result to null to block it.
        if (secondItem != null && secondItem.getType() != Material.AIR) {
            event.setResult(null);
            // Optional: Send a message or play a "denied" sound here if you want
        }
        
        // NOTE: If secondItem is null, but the result isn't, it means they are just renaming.
        // By doing nothing here, the rename remains allowed.
    }

    @EventHandler
    public void onGrindstone(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!plugin.isUnrepairable()) return;
        if (event.getInventory().getType() != org.bukkit.event.inventory.InventoryType.GRINDSTONE) return;
        
        // Check if they are clicking the result slot (slot 2)
        if (event.getRawSlot() != 2) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (item.getItemMeta().getPersistentDataContainer().has(JetpackPlugin.JETPACK_KEY, org.bukkit.persistence.PersistentDataType.BYTE)) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(plugin.getMessage("cannot_repair_jetpack"));
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!activeFlights.containsKey(player.getUniqueId())) return;
        validateFlightNextTick(player);
    }

    @EventHandler
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!activeFlights.containsKey(player.getUniqueId())) return;

        validateFlightNextTick(player);
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (event.isFlying()) {
            // Only take over the flight event when the player is actually trying to use the jetpack.
            // Otherwise leave it to whatever plugin granted flight (e.g. /fly from EssentialsX).
            if (!isWearingJetpack(player)) return;

            if (!player.hasPermission("jetpack.use")) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);

            if (!hasFuel(player)) {
                player.sendMessage(plugin.getMessage("out_of_fuel"));
                player.setAllowFlight(false);
                player.setFlying(false);
                clearFuelProgress(player);
                stopFlightTask(player);
                return;
            }

            player.setAllowFlight(true);
            player.setFlying(true);
            ItemStack jetpack = player.getInventory().getChestplate();
            applyJetpackFlySpeed(player, jetpack);
            startFlightTask(player);
            startParticleTask(player);
        } else {
            // Only intercept stop when this is a jetpack flight we're tracking.
            if (!activeFlights.containsKey(player.getUniqueId())) return;

            event.setCancelled(true);
            player.setFlying(false);
            stopFlightTask(player);
        }
    }

    private void startFlightTask(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeFlights.containsKey(uuid)) return;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isFlying() || !isWearingJetpack(player)) {
                    player.sendMessage(plugin.getMessage("jetpack_removed"));
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    clearFuelProgress(player);
                    stopFlightTask(player);
                    this.cancel();
                    return;
                }

                ItemStack chest = player.getInventory().getChestplate();
                long burnTicks = jetpackService.getFuelBurnTicks(chest);
                int fuelPerDurability = jetpackService.getFuelPerDurability(chest);

                int ticks = fuelTickProgress.getOrDefault(uuid, 0) + 1;

                if (ticks >= burnTicks) {
                    if (!hasFuel(player)) {
                        player.sendMessage(plugin.getMessage("out_of_fuel"));
                        player.setAllowFlight(false);
                        player.setFlying(false);
                        clearFuelProgress(player);
                        stopFlightTask(player);
                        this.cancel();
                        return;
                    }

                    consumeFuel(player);
                    ticks -= burnTicks;

                    int durability = durabilityProgress.getOrDefault(uuid, 0) + 1;
                    if (durability >= fuelPerDurability) {
                        durability = 0;
                        reduceJetpackDurability(player);
                    }
                    durabilityProgress.put(uuid, durability);
                }

                fuelTickProgress.put(uuid, ticks);
            }
        }.runTaskTimer(plugin, 1L, 1L);

        activeFlights.put(uuid, task);
    }

    private void clearFuelProgress(Player player) {
        fuelTickProgress.remove(player.getUniqueId());
        durabilityProgress.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();

        stopFlightTask(player);
        clearFuelProgress(player);

        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    private void stopFlightTask(Player player) {
        BukkitTask task = activeFlights.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        BukkitTask pTask = activeParticles.remove(player.getUniqueId());
        if (pTask != null) {
            pTask.cancel();
        }

        Float previous = originalFlySpeeds.remove(player.getUniqueId());
        if (previous != null) {
            player.setFlySpeed(previous);
        }
    }

    private boolean isWearingJetpack(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() == org.bukkit.Material.AIR) return false;
        return jetpackService.isJetpack(chestplate);
    }

    private boolean hasFuel(Player player) {
        ItemStack fuelTemplate = plugin.getFuelItem();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(fuelTemplate)) {
                return true;
            }
        }
        return false;
    }

    private void consumeFuel(Player player) {
        ItemStack fuelTemplate = plugin.getFuelItem();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(fuelTemplate)) {
                item.setAmount(item.getAmount() - 1);
                return;
            }
        }
    }

    private void validateFlightNextTick(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isWearingJetpack(player) || !hasFuel(player)) {
                    if (!hasFuel(player)) {
                        player.sendMessage(plugin.getMessage("out_of_fuel"));
                    } else {
                        player.sendMessage(plugin.getMessage("jetpack_removed"));
                    }
                    disableJetpackFlight(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private void reduceJetpackDurability(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null) return;

        org.bukkit.inventory.meta.ItemMeta meta = chest.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            int currentDamage = damageable.getDamage();
            int maxDamage = chest.getType().getMaxDurability();

            if (currentDamage >= maxDamage) {
                player.getInventory().setChestplate(null);
                player.sendMessage(plugin.getMessage("jetpack_broken"));
                player.setAllowFlight(false);
                player.setFlying(false);
                clearFuelProgress(player);
                stopFlightTask(player);
            } else {
                damageable.setDamage(currentDamage + 1);
                chest.setItemMeta(damageable);
                jetpackService.updateJetpackLore(chest);
            }
        }
    }

    private void applyJetpackFlySpeed(Player player, ItemStack jetpack) {
        if (jetpack == null || jetpack.getType() == Material.AIR) return;
        originalFlySpeeds.putIfAbsent(player.getUniqueId(), player.getFlySpeed());
        player.setFlySpeed(jetpackService.getFlySpeed(jetpack));
    }

    private void disableJetpackFlight(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
        clearFuelProgress(player);
        stopFlightTask(player);
    }
}