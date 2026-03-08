package me.blekdigits.listeners;

import me.blekdigits.jetpack.JetpackPlugin;
import me.blekdigits.jetpack.JetpackService;
import me.blekdigits.jetpack.JetpackService.RepairResult;
import me.blekdigits.jetpack.JetpackService.Tier;
import me.blekdigits.jetpack.JetpackService.UpgradeResult;
import me.blekdigits.jetpack.JetpackService.UpgradeType;
import me.blekdigits.jetpack.JetpackService.Cost;
import me.blekdigits.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JetpackMenuListener implements Listener {

    private static final class JetpackMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final int SPEED_SLOT = 10;
    private static final int EFFICIENCY_SLOT = 12;
    private static final int DURABILITY_SLOT = 14;
    private static final int REPAIR_SLOT = 16;

    private final JetpackPlugin plugin;
    private final JetpackService jetpackService;
    private final Set<UUID> openedMenus = new HashSet<>();

    public JetpackMenuListener(JetpackPlugin plugin) {
        this.plugin = plugin;
        this.jetpackService = plugin.getJetpackService();
    }

    @EventHandler
    public void onShiftRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getPlayer().isSneaking()) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (!jetpackService.isJetpack(inHand)) return;

        if (!player.hasPermission("jetpack.upgrade") && !player.hasPermission("jetpack.repair")) {
            player.sendMessage(plugin.getMessage("no_upgrade_permission"));
            return;
        }

        event.setCancelled(true);
        openMenu(player);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isJetpackMenu(event.getView().getTopInventory())) return;
        if (!openedMenus.contains(player.getUniqueId())) return;

        // Hard-cancel every click while this menu is open (top + bottom inventories).
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (!jetpackService.isJetpack(inHand)) {
            player.sendMessage(plugin.getMessage("menu_invalid_item"));
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot == SPEED_SLOT) {
            if (!player.hasPermission("jetpack.upgrade")) {
                player.sendMessage(plugin.getMessage("no_upgrade_permission"));
                return;
            }
            handleUpgrade(player, inHand, UpgradeType.SPEED, plugin.getRawMessage("menu_speed"));
        } else if (slot == EFFICIENCY_SLOT) {
            if (!player.hasPermission("jetpack.upgrade")) {
                player.sendMessage(plugin.getMessage("no_upgrade_permission"));
                return;
            }
            handleUpgrade(player, inHand, UpgradeType.EFFICIENCY, plugin.getRawMessage("menu_efficiency"));
        } else if (slot == DURABILITY_SLOT) {
            if (!player.hasPermission("jetpack.upgrade")) {
                player.sendMessage(plugin.getMessage("no_upgrade_permission"));
                return;
            }
            handleUpgrade(player, inHand, UpgradeType.DURABILITY, plugin.getRawMessage("menu_durability"));
        } else if (slot == REPAIR_SLOT) {
            if (!player.hasPermission("jetpack.repair")) {
                player.sendMessage(plugin.getMessage("no_repair_permission"));
                return;
            }
            handleRepair(player, inHand);
        }

        openMenu(player);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isJetpackMenu(event.getView().getTopInventory())) return;
        if (!openedMenus.contains(player.getUniqueId())) return;

        int topSize = event.getView().getTopInventory().getSize();
        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (touchesTop) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        openedMenus.remove(event.getPlayer().getUniqueId());
    }

    private void openMenu(Player player) {
        String title = Utils.colorize(plugin.getRawMessage("menu_title"));
        Inventory inventory = plugin.getServer().createInventory(new JetpackMenuHolder(), 27, title);

        ItemStack inHand = player.getInventory().getItemInMainHand();

        inventory.setItem(SPEED_SLOT, createUpgradeButton(Material.FEATHER, plugin.getRawMessage("menu_speed"), inHand, UpgradeType.SPEED));
        inventory.setItem(EFFICIENCY_SLOT, createUpgradeButton(Material.BLAZE_POWDER, plugin.getRawMessage("menu_efficiency"), inHand, UpgradeType.EFFICIENCY));
        inventory.setItem(DURABILITY_SLOT, createUpgradeButton(Material.ANVIL, plugin.getRawMessage("menu_durability"), inHand, UpgradeType.DURABILITY));
        inventory.setItem(REPAIR_SLOT, createRepairButton(inHand));

        openedMenus.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    private ItemStack createUpgradeButton(Material icon, String name, ItemStack jetpack, UpgradeType type) {
        ItemStack button = new ItemStack(icon);
        ItemMeta meta = button.getItemMeta();
        if (meta == null) return button;

        meta.setDisplayName(Utils.colorize(name));
        List<String> lore = new ArrayList<>();
        lore.add(Utils.colorize(plugin.getMessage("menu_current_value")
                .replace("{value}", String.valueOf(jetpackService.getCurrentValue(jetpack, type)))));

        Tier next = jetpackService.getNextTier(jetpack, type);
        if (next == null) {
            lore.add(Utils.colorize(plugin.getMessage("menu_maxed")));
        } else {
            lore.add(Utils.colorize(plugin.getMessage("menu_next_level").replace("{level}", String.valueOf(next.level()))));
            lore.add(Utils.colorize(plugin.getMessage("menu_cost")
                    .replace("{cost}", jetpackService.formatCost(next.cost()))));
        }

        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }

    private ItemStack createRepairButton(ItemStack jetpack) {
        ItemStack button = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = button.getItemMeta();
        if (meta == null) return button;

        meta.setDisplayName(Utils.colorize(plugin.getRawMessage("menu_repair")));
        List<String> lore = new ArrayList<>();
        if (jetpack.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
            int maxDurability = jetpack.getType().getMaxDurability();
            int remaining = maxDurability - damageable.getDamage();
            lore.add(Utils.colorize("&7Current Durability: &f" + remaining + "&7/&f" + maxDurability));
        }

        Cost repairCost = jetpackService.getRepairCost();
        lore.add(Utils.colorize(plugin.getMessage("menu_cost")
                .replace("{cost}", jetpackService.formatCost(repairCost))));

        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }

    private void handleUpgrade(Player player, ItemStack jetpack, UpgradeType type, String label) {
        UpgradeResult result = jetpackService.tryUpgrade(player, jetpack, type);
        switch (result.status()) {
            case SUCCESS -> player.sendMessage(plugin.getMessage("upgrade_success")
                    .replace("{type}", stripColors(label))
                    .replace("{level}", String.valueOf(result.level())));
            case MAXED -> player.sendMessage(plugin.getMessage("upgrade_maxed")
                    .replace("{type}", stripColors(label)));
            case MISSING_MATERIAL -> player.sendMessage(plugin.getMessage("upgrade_missing_material")
                    .replace("{cost}", jetpackService.formatCost(result.cost())));
            default -> player.sendMessage(plugin.getMessage("menu_invalid_item"));
        }
    }

    private void handleRepair(Player player, ItemStack jetpack) {
        RepairResult result = jetpackService.tryRepair(player, jetpack);
        switch (result.status()) {
            case SUCCESS -> player.sendMessage(plugin.getMessage("repair_success")
                    .replace("{amount}", String.valueOf(result.repairedAmount())));
            case NOT_DAMAGED -> player.sendMessage(plugin.getMessage("repair_not_damaged"));
            case NOT_REPAIRABLE -> player.sendMessage(plugin.getMessage("repair_not_damageable"));
            case MISSING_MATERIAL -> player.sendMessage(plugin.getMessage("upgrade_missing_material")
                    .replace("{cost}", jetpackService.formatCost(result.cost())));
            default -> player.sendMessage(plugin.getMessage("menu_invalid_item"));
        }
    }

    private static String stripColors(String input) {
        return org.bukkit.ChatColor.stripColor(Utils.colorize(input));
    }

    private boolean isJetpackMenu(Inventory topInventory) {
        return topInventory != null && topInventory.getHolder() instanceof JetpackMenuHolder;
    }
}