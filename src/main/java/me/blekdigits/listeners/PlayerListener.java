package me.blekdigits.listeners;

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
    private final Map<UUID, BukkitTask> activeParticles = new HashMap<>();
    // This Map is like storing your interval IDs: const intervals = {}
    private final Map<UUID, BukkitTask> activeFlights = new HashMap<>();

    public PlayerListener(JetpackPlugin plugin) {
        this.plugin = plugin;
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

    // ... (Keep your existing onPlayerSneak method here) ...
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking() || player.isFlying()) return;
        if (!isWearingJetpack(player)) return;

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

    // --- NEW LOGIC BELOW ---
    @EventHandler
        public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
            // 1. Is it a player?
            if (!(event.getWhoClicked() instanceof Player player)) return;

            // 2. FAST FAIL: Are they actively flying right now?
            // If not, we don't care what they are doing in their inventory.
            if (!activeFlights.containsKey(player.getUniqueId())) return;

            // 3. The "nextTick" validation
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

        // event.isFlying() is true when they double jump to START flying
        if (event.isFlying()) {
            if (!isWearingJetpack(player)) {
                event.setCancelled(true); 
                player.setAllowFlight(false);
                return;
            }

            if (!hasFuel(player)) {
                player.sendMessage(plugin.getMessage("out_of_fuel"));
                event.setCancelled(true);
                player.setAllowFlight(false);
                return;
            }

            // They are good to go! Start the fuel drain loop.
            startFlightTask(player);
            startParticleTask(player);
        } else {
            // They double jumped to stop flying, or hit the ground.
            stopFlightTask(player);
        }
    }

    private void startFlightTask(Player player) {
        // Prevent duplicate intervals if they spam spacebar
        if (activeFlights.containsKey(player.getUniqueId())) return;

        // This is exactly like: const taskId = setInterval(() => { ... }, 1000);
        BukkitTask task = new BukkitRunnable() {
            private int fuelCount = 0;

            @Override
            public void run() {
                // 1. Check if they should STILL be flying
                if (!player.isFlying() || !isWearingJetpack(player) || !hasFuel(player)) {
                    player.sendMessage(plugin.getMessage("jetpack_removed"));
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    stopFlightTask(player); // Remove from Map
                    this.cancel(); // clearInterval(taskId)
                    return;
                }

                // 2. Consume 1 fuel
                consumeFuel(player);
                fuelCount++;

                if (fuelCount >= plugin.getFuelPerDurability()) {
                    fuelCount = 0; // Reset counter
                    reduceJetpackDurability(player);
                }
            }
        }.runTaskTimer(plugin, 0L, plugin.getFuelBurnRate()); 
        // 0L = delay before first run (0 ticks)
        // 20L = interval between runs (20 ticks = 1 second)

        // Store the task so we can cancel it later
        activeFlights.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 1. Clear intervals to prevent memory leaks
        stopFlightTask(player);

        // 2. Reset their flight permissions so they don't exploit it
        // (Unless they are in Creative mode, of course)
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    private void stopFlightTask(Player player) {
        BukkitTask task = activeFlights.remove(player.getUniqueId());
        if (task != null) {
            task.cancel(); // clearInterval
        }

        BukkitTask pTask = activeParticles.remove(player.getUniqueId());
        if (pTask != null) {
            pTask.cancel();
        }
    }

    // --- INVENTORY HELPERS ---

    private boolean isWearingJetpack(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() == org.bukkit.Material.AIR) return false;
        return chestplate.getItemMeta().getPersistentDataContainer().has(JetpackPlugin.JETPACK_KEY, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    private boolean hasFuel(Player player) {
        ItemStack fuelTemplate = plugin.getFuelItem();
        // Loop through the array of items in their inventory
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
                return; // Stop after taking 1
            }
        }
    }

    private void validateFlightNextTick(Player player) {
        // Wait exactly 1 server tick (50ms) for the inventory math to finish
        new BukkitRunnable() {
            @Override
            public void run() {
                // Now we check the TRUE state of their inventory
                if (!isWearingJetpack(player) || !hasFuel(player)) {
                    player.sendMessage(plugin.getMessage("jetpack_removed"));
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    stopFlightTask(player); // Kills the fuel and particle loops
                }
            }
        }.runTaskLater(plugin, 1L); // 1L = 1 tick delay
    }

    private void reduceJetpackDurability(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null) return;

        org.bukkit.inventory.meta.ItemMeta meta = chest.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            int currentDamage = damageable.getDamage();
            int maxDamage = chest.getType().getMaxDurability();

            if (currentDamage >= maxDamage) {
                // Jetpack is broken!
                player.getInventory().setChestplate(null);
                player.sendMessage(plugin.getMessage("jetpack_broken"));
                
                // Force stop flight since the item is gone
                player.setAllowFlight(false);
                player.setFlying(false);
                stopFlightTask(player);
            } else {
                // Apply 1 point of damage
                damageable.setDamage(currentDamage + 1);
                chest.setItemMeta(damageable);
            }
        }
    }
}