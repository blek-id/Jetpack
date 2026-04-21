package me.blekdigits.jetpack;

import me.blekdigits.utils.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class JetpackService {

    public enum UpgradeType {
        SPEED("speed", JetpackPlugin.SPEED_LEVEL_KEY),
        EFFICIENCY("efficiency", JetpackPlugin.EFFICIENCY_LEVEL_KEY),
        DURABILITY("durability", JetpackPlugin.DURABILITY_LEVEL_KEY);

        private final String path;
        private final org.bukkit.NamespacedKey levelKey;

        UpgradeType(String path, org.bukkit.NamespacedKey levelKey) {
            this.path = path;
            this.levelKey = levelKey;
        }

        public String getPath() {
            return path;
        }

        public org.bukkit.NamespacedKey getLevelKey() {
            return levelKey;
        }
    }

    public enum CostProvider {
        MATERIAL,
        MONEY,
        MMOITEMS
    }

    public record Cost(CostProvider provider, int amount, Material material, double money, String mmoType, String mmoId) {}
    public record Tier(int level, double value, Cost cost) {}

    private final JetpackPlugin plugin;
    private Object economyProvider;
    private boolean economyLookupDone;

    public JetpackService(JetpackPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isJetpack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(JetpackPlugin.JETPACK_KEY, PersistentDataType.BYTE);
    }

    public void updateJetpackLore(ItemStack item) {
        if (!isJetpack(item)) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String fuelName = beautifyMaterial(plugin.getFuelItem().getType());
        int speedLevel = getLevel(item, UpgradeType.SPEED);
        int efficiencyLevel = getLevel(item, UpgradeType.EFFICIENCY);
        int durabilityLevel = getLevel(item, UpgradeType.DURABILITY);

        double speedValue = getCurrentValue(item, UpgradeType.SPEED);
        double efficiencyValue = getCurrentValue(item, UpgradeType.EFFICIENCY);
        double durabilityValue = getCurrentValue(item, UpgradeType.DURABILITY);

        List<String> loreTemplate = plugin.getConfig().getStringList("jetpack-lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            String replaced = line
                    .replace("{fuel_name}", fuelName)
                    .replace("{speed_level}", String.valueOf(speedLevel))
                    .replace("{efficiency_level}", String.valueOf(efficiencyLevel))
                    .replace("{durability_level}", String.valueOf(durabilityLevel))
                    .replace("{speed_value}", formatValue(speedValue))
                    .replace("{efficiency_value}", formatValue(efficiencyValue))
                    .replace("{durability_value}", formatValue(durabilityValue));
            lore.add(Utils.colorize(replaced));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public int getLevel(ItemStack item, UpgradeType type) {
        if (!isJetpack(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer value = pdc.get(type.getLevelKey(), PersistentDataType.INTEGER);
        return value == null ? 0 : Math.max(0, value);
    }

    public void setLevel(ItemStack item, UpgradeType type, int level) {
        if (!isJetpack(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(type.getLevelKey(), PersistentDataType.INTEGER, Math.max(0, level));
        item.setItemMeta(meta);
        updateJetpackLore(item);
    }

    public Map<Integer, Tier> getTiers(UpgradeType type) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("upgrades." + type.getPath() + ".tiers");
        Map<Integer, Tier> tiers = new TreeMap<>(Comparator.naturalOrder());
        if (section == null) return tiers;

        for (String key : section.getKeys(false)) {
            int level;
            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException ex) {
                continue;
            }

            String path = "upgrades." + type.getPath() + ".tiers." + key;
            double value = plugin.getConfig().getDouble(path + ".value", 0.0);
            Cost cost = parseCost(path + ".cost");

            tiers.put(level, new Tier(level, value, cost));
        }

        return tiers;
    }

    public Tier getTier(UpgradeType type, int level) {
        return getTiers(type).get(level);
    }

    public Tier getNextTier(ItemStack item, UpgradeType type) {
        int current = getLevel(item, type);
        return getTier(type, current + 1);
    }

    public double getCurrentValue(ItemStack item, UpgradeType type) {
        int level = getLevel(item, type);
        Tier tier = getTier(type, level);

        if (tier != null) {
            return tier.value();
        }

        return switch (type) {
            case SPEED -> plugin.getBaseFlySpeed();
            case EFFICIENCY -> 1.0;
            case DURABILITY -> 1.0;
        };
    }

    public float getFlySpeed(ItemStack jetpack) {
        return (float) Math.max(0.05, Math.min(1.0, getCurrentValue(jetpack, UpgradeType.SPEED)));
    }

    public long getFuelBurnTicks(ItemStack jetpack) {
        double efficiencyMultiplier = Math.max(0.1, getCurrentValue(jetpack, UpgradeType.EFFICIENCY));
        double intervalSeconds = plugin.getFuelBurnIntervalSeconds() * efficiencyMultiplier;
        long ticks = (long) Math.ceil(intervalSeconds * 20.0);
        return Math.max(1L, ticks);
    }

    public int getFuelPerDurability(ItemStack jetpack) {
        double durabilityMultiplier = Math.max(0.1, getCurrentValue(jetpack, UpgradeType.DURABILITY));
        int value = (int) Math.round(plugin.getFuelPerDurability() * durabilityMultiplier);
        return Math.max(1, value);
    }

    public boolean canAfford(Player player, Material material, int amount) {
        return countMaterial(player.getInventory(), material) >= amount;
    }

    public int countMaterial(PlayerInventory inventory, Material material) {
        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null || stack.getType() != material) continue;
            total += stack.getAmount();
        }
        return total;
    }

    public void removeMaterial(PlayerInventory inventory, Material material, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() != material) continue;

            if (stack.getAmount() <= remaining) {
                remaining -= stack.getAmount();
                inventory.setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - remaining);
                inventory.setItem(i, stack);
                return;
            }

            if (remaining <= 0) return;
        }
    }

    public UpgradeResult tryUpgrade(Player player, ItemStack item, UpgradeType type) {
        if (!isJetpack(item)) return UpgradeResult.invalid();

        Tier next = getNextTier(item, type);
        if (next == null) return UpgradeResult.maxed();

        Cost cost = next.cost();
        if (!canAfford(player, cost)) {
            return UpgradeResult.missing(cost);
        }

        takeCost(player, cost);
        setLevel(item, type, next.level());
        return UpgradeResult.success(next.level(), cost);
    }

    public RepairResult tryRepair(Player player, ItemStack item) {
        if (!isJetpack(item)) return RepairResult.invalid();

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return RepairResult.notRepairable();
        int currentDamage = damageable.getDamage();
        if (currentDamage <= 0) return RepairResult.notDamaged();

        Cost repairCost = getRepairCost();

        if (!canAfford(player, repairCost)) {
            return RepairResult.missing(repairCost);
        }

        int maxDurability = item.getType().getMaxDurability();
        String mode = plugin.getConfig().getString("repair.mode", "PERCENT").toUpperCase(Locale.ROOT);
        double configuredAmount = plugin.getConfig().getDouble("repair.amount", 10.0);

        int repairedAmount;
        if ("FLAT".equals(mode)) {
            repairedAmount = (int) Math.round(configuredAmount);
        } else {
            repairedAmount = (int) Math.round((configuredAmount / 100.0) * maxDurability);
        }

        repairedAmount = Math.max(1, repairedAmount);
        int newDamage = Math.max(0, currentDamage - repairedAmount);
        int actuallyRepaired = currentDamage - newDamage;
        if (actuallyRepaired <= 0) return RepairResult.notDamaged();

        takeCost(player, repairCost);

        damageable.setDamage(newDamage);
        item.setItemMeta(damageable);
        updateJetpackLore(item);
        return RepairResult.success(actuallyRepaired, repairCost);
    }

    public Cost getRepairCost() {
        return parseCost("repair.cost");
    }

    public String formatCost(Cost cost) {
        if (cost == null) return "Unknown";
        return switch (cost.provider()) {
            case MONEY -> "$" + formatValue(cost.money());
            case MMOITEMS -> cost.amount() + "x " + beautifyId(cost.mmoId());
            case MATERIAL -> cost.amount() + "x " + beautifyMaterial(cost.material());
        };
    }

    public boolean canAfford(Player player, Cost cost) {
        if (cost == null) return false;
        return switch (cost.provider()) {
            case MONEY -> hasMoney(player, cost.money());
            case MMOITEMS -> countMMOItems(player.getInventory(), cost.mmoType(), cost.mmoId()) >= cost.amount();
            case MATERIAL -> canAfford(player, cost.material(), cost.amount());
        };
    }

    public void takeCost(Player player, Cost cost) {
        if (cost == null) return;
        switch (cost.provider()) {
            case MONEY -> withdrawMoney(player, cost.money());
            case MMOITEMS -> removeMMOItems(player.getInventory(), cost.mmoType(), cost.mmoId(), cost.amount());
            case MATERIAL -> removeMaterial(player.getInventory(), cost.material(), cost.amount());
        }
    }

    private Cost parseCost(String path) {
        String providerText = plugin.getConfig().getString(path + ".provider", "MATERIAL").toUpperCase(Locale.ROOT);
        CostProvider provider;
        try {
            provider = CostProvider.valueOf(providerText);
        } catch (IllegalArgumentException ex) {
            provider = CostProvider.MATERIAL;
        }

        if (provider == CostProvider.MONEY) {
            double money = Math.max(0.0, plugin.getConfig().getDouble(path + ".money", 0.0));
            return new Cost(CostProvider.MONEY, 0, null, money, null, null);
        }

        if (provider == CostProvider.MMOITEMS) {
            String type = plugin.getConfig().getString(path + ".mmoitems.type", "MATERIAL");
            String id = plugin.getConfig().getString(path + ".mmoitems.id", "UNKNOWN");
            int amount = Math.max(1, plugin.getConfig().getInt(path + ".mmoitems.amount", plugin.getConfig().getInt(path + ".amount", 1)));
            return new Cost(CostProvider.MMOITEMS, amount, null, 0.0, type, id);
        }

        Material material = Material.matchMaterial(plugin.getConfig().getString(path + ".material", "STONE"));
        int amount = Math.max(1, plugin.getConfig().getInt(path + ".amount", 1));
        if (material == null) material = Material.STONE;
        return new Cost(CostProvider.MATERIAL, amount, material, 0.0, null, null);
    }

    private boolean hasMoney(Player player, double amount) {
        Object economy = getEconomyProvider();
        if (economy == null) return false;

        try {
            Method method = findMethod(economy.getClass(), "has", OfflinePlayer.class, double.class);
            if (method != null) {
                Object result = method.invoke(economy, player, amount);
                return result instanceof Boolean b && b;
            }

            Method fallback = findMethod(economy.getClass(), "has", String.class, double.class);
            if (fallback != null) {
                Object result = fallback.invoke(economy, player.getName(), amount);
                return result instanceof Boolean b && b;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void withdrawMoney(Player player, double amount) {
        Object economy = getEconomyProvider();
        if (economy == null) return;

        try {
            Method method = findMethod(economy.getClass(), "withdrawPlayer", OfflinePlayer.class, double.class);
            Object response;

            if (method != null) {
                response = method.invoke(economy, player, amount);
            } else {
                Method fallback = findMethod(economy.getClass(), "withdrawPlayer", String.class, double.class);
                if (fallback == null) return;
                response = fallback.invoke(economy, player.getName(), amount);
            }

            if (response != null) {
                Method success = findMethod(response.getClass(), "transactionSuccess");
                if (success != null) {
                    success.invoke(response);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Object getEconomyProvider() {
        if (economyLookupDone) return economyProvider;
        economyLookupDone = true;

        if (!plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            return null;
        }

        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            @SuppressWarnings("unchecked")
            RegisteredServiceProvider<?> registration = plugin.getServer().getServicesManager().getRegistration((Class) economyClass);
            if (registration != null) {
                economyProvider = registration.getProvider();
            }
        } catch (Exception ignored) {
        }

        return economyProvider;
    }

    private int countMMOItems(PlayerInventory inventory, String requiredType, String requiredId) {
        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (!isMatchingMMOItem(stack, requiredType, requiredId)) continue;
            total += stack.getAmount();
        }
        return total;
    }

    private void removeMMOItems(PlayerInventory inventory, String requiredType, String requiredId, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) continue;
            if (!isMatchingMMOItem(stack, requiredType, requiredId)) continue;

            if (stack.getAmount() <= remaining) {
                remaining -= stack.getAmount();
                inventory.setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - remaining);
                inventory.setItem(i, stack);
                return;
            }

            if (remaining <= 0) return;
        }
    }

    private boolean isMatchingMMOItem(ItemStack stack, String requiredType, String requiredId) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("MMOItems")) {
            return false;
        }

        try {
            Class<?> nbtItemClass = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
            Method getMethod = nbtItemClass.getMethod("get", ItemStack.class);
            Object nbtItem = getMethod.invoke(null, stack);
            if (nbtItem == null) return false;

            String itemId = invokeString(nbtItem, "getString", "MMOITEMS_ITEM_ID");
            String itemType = invokeString(nbtItem, "getString", "MMOITEMS_ITEM_TYPE");

            if ((itemType == null || itemType.isBlank())) {
                Method getTypeMethod = findMethod(nbtItem.getClass(), "getType");
                if (getTypeMethod != null) {
                    Object rawType = getTypeMethod.invoke(nbtItem);
                    if (rawType != null) itemType = rawType.toString();
                }
            }

            return itemId != null
                    && itemType != null
                    && itemId.equalsIgnoreCase(requiredId)
                    && itemType.equalsIgnoreCase(requiredType);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String invokeString(Object target, String method, String input) {
        try {
            Method m = target.getClass().getMethod(method, String.class);
            Object value = m.invoke(target, input);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static String beautifyMaterial(Material material) {
        return beautifyId(material.name());
    }

    public static String beautifyId(String id) {
        if (id == null || id.isBlank()) return "Unknown";
        String name = id.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder output = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            output.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return output.toString().trim();
    }

    public static String formatValue(double value) {
        if (Math.floor(value) == value) return String.valueOf((int) value);
        String formatted = String.format(Locale.US, "%.2f", value);
        formatted = formatted.replaceAll("0+$", "");
        formatted = formatted.replaceAll("\\.$", "");
        return formatted;
    }

    public record UpgradeResult(Status status, int level, Cost cost) {
        public enum Status { SUCCESS, MAXED, MISSING_MATERIAL, INVALID }

        public static UpgradeResult success(int level, Cost cost) {
            return new UpgradeResult(Status.SUCCESS, level, cost);
        }

        public static UpgradeResult maxed() {
            return new UpgradeResult(Status.MAXED, 0, null);
        }

        public static UpgradeResult missing(Cost cost) {
            return new UpgradeResult(Status.MISSING_MATERIAL, 0, cost);
        }

        public static UpgradeResult invalid() {
            return new UpgradeResult(Status.INVALID, 0, null);
        }
    }

    public record RepairResult(Status status, int repairedAmount, Cost cost) {
        public enum Status { SUCCESS, NOT_DAMAGED, MISSING_MATERIAL, NOT_REPAIRABLE, INVALID }

        public static RepairResult success(int repairedAmount, Cost cost) {
            return new RepairResult(Status.SUCCESS, repairedAmount, cost);
        }

        public static RepairResult notDamaged() {
            return new RepairResult(Status.NOT_DAMAGED, 0, null);
        }

        public static RepairResult missing(Cost cost) {
            return new RepairResult(Status.MISSING_MATERIAL, 0, cost);
        }

        public static RepairResult notRepairable() {
            return new RepairResult(Status.NOT_REPAIRABLE, 0, null);
        }

        public static RepairResult invalid() {
            return new RepairResult(Status.INVALID, 0, null);
        }
    }
}