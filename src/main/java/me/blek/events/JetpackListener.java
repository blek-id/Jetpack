package me.blek.events;

import co.aikar.commands.annotation.Dependency;
import me.blek.items.JetpackItem;
import me.blek.jetpack.Jetpack;
import me.blek.utils.Common;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.function.Supplier;

public class JetpackListener implements Listener {

    private FileConfiguration config;
    private Supplier<ItemStack> fuelSupplier;
    private Supplier<ItemStack> jetpackSupplier;
    private Jetpack plugin;
    private boolean isFlying = false;

    public JetpackListener(Jetpack plugin){
        this.plugin = plugin;
        this.config = plugin.configuration;
        this.fuelSupplier = plugin.fuelSupplier;
        this.jetpackSupplier = plugin.jetpackSupplier;
    }

    @EventHandler
    public void onShift (PlayerToggleSneakEvent e) {

        Player player = e.getPlayer();

        ItemStack chestplate = player.getInventory().getChestplate();

        if(chestplate == null){
            return;
        }

        if(e.isSneaking()){
            return;
        }

        ItemStack configFuel = fuelSupplier.get();
        ItemStack playerFuel = null;


        ItemStack playerChestplate = chestplate.clone();
        ItemStack configChestplate = jetpackSupplier.get().clone();

        playerChestplate.setDurability((short) 1);
        configChestplate.setDurability((short) 1);

        if(!playerChestplate.isSimilar(configChestplate)){
            return;
        }

        if(!player.hasPermission("jetpack.use")) {
            player.sendMessage(Common.colorize(config.getString("messages.prefix") + " " + config.getString("messages.no-permission")));
            return;
        }

        playerFuel = getFuelFromPlayer(player, configFuel);

        if(playerFuel == null){
            player.sendMessage(Common.colorize(config.getString("messages.prefix") + " " + config.getString("messages.jetpack.need-fuel")));
            return;
        }

        if(player.isOnGround()){
            isFlying = toggleFlying(player, playerFuel);
        }

        if(!isFlying){
            return;
        }

        int burnRate = config.getInt("burn-rate");

        new BukkitRunnable() {

            @Override
            public void run() {
                ItemStack currentConfigFuel = fuelSupplier.get();
                ItemStack playerFuel = getFuelFromPlayer(player, currentConfigFuel);

                ItemStack playerChestplate = player.getInventory().getChestplate();

                if(playerChestplate == null){
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage(Common.colorize(config.getString("messages.prefix") + " " + config.getString("messages.jetpack.turn-off")));
                    cancel();
                } else {
                    controlFlight(playerFuel, playerChestplate);
                }


            }

            private void controlFlight(ItemStack playerFuel, ItemStack playerChestplate) {
                playerChestplate =  playerChestplate.clone();
                playerChestplate.setDurability((short) 1);

                if(!playerChestplate.isSimilar(configChestplate)){
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage(Common.colorize(config.getString("messages.prefix") + " " + config.getString("messages.jetpack.turn-off")));
                    cancel();
                }

                if(playerFuel == null){
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage(Common.colorize(config.getString("messages.prefix") + " " + config.getString("messages.jetpack.out-of-fuel")));
                    cancel();
                }

                if(player.isFlying()){
                    if (playerFuel != null) {
                        playerFuel.setAmount(playerFuel.getAmount()-1);
                    }
                }

                if(!player.getAllowFlight() && !player.isFlying()){
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20 * burnRate);
    }

    @EventHandler
    public void OnFlying (PlayerToggleFlightEvent e){
        if(isFlying){
            spawnParticle(e.getPlayer());
        }
    }

    private void spawnParticle(Player player){
        new BukkitRunnable() {
            @Override
            public void run() {
                if(player.isFlying()){
                    Location location = player.getLocation();
                    Particle particle;

                    String particleString = config.getString("jetpack-particle");
                    particle = Particle.valueOf(particleString);

                    int amount = config.getInt("jetpack-particle-amount");

                    final float newX = (float)( 0.2 * Math.sin(Math.toRadians(player.getLocation().getYaw() + 90 * 3)));
                    final float newZ = (float)( 0.2 * Math.sin(Math.toRadians(player.getLocation().getYaw() + 90 * 3)));


                    if(particleString.equals("REDSTONE")){
                        String redstoneParticle = config.getString("jetpack-redstone-particle");
                        String[] redstoneParticles = redstoneParticle.split(",");
                        Particle.DustOptions particleDustOptions = new Particle.DustOptions(Color.fromRGB(Integer.parseInt(redstoneParticles[0]), Integer.parseInt(redstoneParticles[1]), Integer.parseInt(redstoneParticles[2])), 1);

                        player.getWorld().spawnParticle(particle, location.getX() + newX, + location.getY() + 0.8, location.getZ() + newZ, amount, 0, -0.1, 0, particleDustOptions);
                    } else {
                        player.getWorld().spawnParticle(particle, location.getX() + newX, + location.getY() + 0.8, location.getZ() + newZ, amount, 0, -0.1, 0);
                    }

                }

                if(!player.getAllowFlight() || !player.isFlying()){
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private boolean toggleFlying(Player pl, ItemStack fuel){
        if(!pl.getAllowFlight()){
            pl.setAllowFlight(true);
            pl.sendMessage(Common.colorize(config.getString("messages.prefix") + " " + config.getString("messages.jetpack.turn-on")));
            return true;
        } else {
            pl.setAllowFlight(false);
            pl.sendMessage(Common.colorize(config.getString("messages.prefix") + " " + config.getString("messages.jetpack.turn-off")));
            return false;
        }
    }

    private ItemStack getFuelFromPlayer(Player pl, ItemStack fuel){
        for (ItemStack item : pl.getInventory().getContents()) {
            if(item != null){
                if(item.isSimilar(fuel)){
                    return item;
                }
            }
        }

        return null;
    }
}
