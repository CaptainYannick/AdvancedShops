package io.captainyannick.advancedShops.shop;

import io.captainyannick.advancedShops.AdvancedShops;
import io.captainyannick.advancedShops.core.utils.GuiUtils;
import io.captainyannick.advancedShops.core.utils.TextUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopManager {

    private static final Map<Location, Shop> shops = new HashMap<>();
    private static final Map<UUID, PriceAdjustment> priceAdjustmentState = new HashMap<>();
    private static AdvancedShops plugin;
    private static final Map<UUID, Shop> toStockEditGUI = new HashMap<>();
    private static final Map<UUID, Shop> activeShopSessions = new HashMap<>();

    public static void addToStockEditGui(Player player, Shop shop) {
        toStockEditGUI.put(player.getUniqueId(), shop);
    }

    public static Shop getToStockEditGui(Player player) {
        return toStockEditGUI.get(player.getUniqueId());
    }

    public static void removeStockEditGui(Player player) { toStockEditGUI.remove(player.getUniqueId()); }

    public static void startPriceAdjustment(Player player, Shop shop, boolean isBuyPrice) {
        priceAdjustmentState.put(player.getUniqueId(), new PriceAdjustment(shop, isBuyPrice));
    }

    public static PriceAdjustment getPriceAdjustment(Player player) {
        return priceAdjustmentState.get(player.getUniqueId());
    }

    public static void finishPriceAdjustment(Player player) {
        priceAdjustmentState.remove(player.getUniqueId());
    }

    public static void initialize(AdvancedShops pluginInstance) {
        plugin = pluginInstance;
        loadShops();
    }

    public static void addShopSession(Player player, Shop shop) {
        activeShopSessions.put(player.getUniqueId(), shop);
    }

    public static Shop getActiveShopSession(Player player) {
        return activeShopSessions.get(player.getUniqueId());
    }

    public static void removeShopSession(Player player) {
        activeShopSessions.remove(player.getUniqueId());
    }

    public static void createShop(Player player, String[] args) {

        int maxShops = 1000;//TODO: getMaxShopsForPlayer(player);
        int playerShopCount = getShopCount(player.getUniqueId());
        if (playerShopCount >= maxShops) {
            player.sendMessage(ChatColor.RED + "You have reached your maximum number of shops.");
            return;
        }

        Economy economy = AdvancedShops.getEconomy();
        double creationCost = plugin.getMainConfig().getDouble("shop_creation_cost");
        if (!economy.has(player, creationCost)) {
            player.sendMessage(ChatColor.RED + "You don't have enough money to create a shop. Cost: " + creationCost);
            return;
        }

        economy.withdrawPlayer(player, creationCost);
        Block block = player.getTargetBlockExact(5);
        if (block == null || !(block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST || block.getType() == Material.BARREL)) {
            player.sendMessage(ChatColor.RED + "You must be looking at a chest, trapped chest, or barrel to create a shop.");
            return;
        }

        if (shops.containsKey(block.getLocation())) {
            player.sendMessage(ChatColor.RED + "A shop already exists at this location.");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must hold the item you want to sell in your hand.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /shop create <buyPrice> <sellPrice>");
            return;
        }

        double buyPrice;
        double sellPrice;

        try {
            buyPrice = Double.parseDouble(args[1]);
            sellPrice = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid price values.");
            return;
        }

        Shop shop = new Shop(player.getUniqueId(), block.getLocation(), item.clone(), buyPrice, sellPrice, false);
        if (ShopSign.createShopSign(shop, player)) {
            shops.put(block.getLocation(), shop);
            ShopHologram.createFloatingItem(shop);
            player.sendMessage(ChatColor.GREEN + "Shop created successfully!");
        } else {
            ShopManager.deleteShop(shop);
            player.sendMessage(ChatColor.RED + "Shop creation failed!");
        }
    }

    public static List<Shop> getAllShops() {
        return new ArrayList<>(shops.values());
    }

    public static void deleteShop(Shop shop) {
        Bukkit.getScheduler().runTaskLater(AdvancedShops.getInstance(), () -> {
            ShopHologram.removeFloatingItem(shop);
            ItemStack shopItem = shop.getItem().clone();
            shopItem.setAmount(shop.getStock());
            shop.getLocation().getWorld().dropItem(shop.getLocation(),shopItem);
            shops.remove(shop.getLocation());
            removeShopFromFile(shop);
            ShopSign.removeShopSign(shop);

            Player owner = Bukkit.getPlayer(shop.getOwner());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(ChatColor.GREEN + "Your shop has been successfully deleted.");
            }
        }, 5L);
    }

    private static void removeShopFromFile(Shop shop) {


        String locKey = locationToString(shop.getLocation());
        plugin.getDataFile().set(locKey, null);

        plugin.getDataFileRaw().save();
    }

    public static void updateShop(Shop shop) {
        ShopSign.updateShopSign(shop.getSignLocation().getBlock(), shop);
    }

    private static int getShopCount(UUID owner) {
        return (int) shops.values().stream().filter(shop -> shop.getOwner().equals(owner)).count();
    }

    public static Shop getShopAtLocation(Location location) {
        return shops.get(location);
    }

    public static void openCustomerGUI(Player player, Shop shop) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GREEN + "Shop: " + TextUtils.formatItemName(shop.getItem()));
        addShopSession(player, shop);

        // Slot 11: Buy option
        ItemStack buyItem = new ItemStack(shop.getItem());
        ItemMeta buyMeta = buyItem.getItemMeta();
        buyMeta.setDisplayName(ChatColor.GREEN + "Buy Item");
        if (shop.getBuyPrice() > 0){
            buyMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + shop.getBuyPrice()));
        } else {
            buyMeta.setLore(Arrays.asList(ChatColor.RED + "Buying disabled"));
        }
        buyItem.setItemMeta(buyMeta);
        gui.setItem(11, buyItem);

        // Slot 15: Sell option
        ItemStack sellItem = new ItemStack(shop.getItem());
        ItemMeta sellMeta = sellItem.getItemMeta();
        sellMeta.setDisplayName(ChatColor.RED + "Sell Item");
        if (shop.getSellPrice() > 0){
            sellMeta.setLore(Arrays.asList(ChatColor.YELLOW + "Price: " + shop.getSellPrice()));
        } else {
            sellMeta.setLore(Arrays.asList(ChatColor.RED + "Selling disabled"));
        }
        sellItem.setItemMeta(sellMeta);
        gui.setItem(15, sellItem);

        GuiUtils.fillEmptySlots(gui);

        player.openInventory(gui);
    }

    public static void openManagementGUI(Player player, Shop shop) {
        Inventory gui = Bukkit.createInventory(null, 36, ChatColor.DARK_GRAY + "Manage Shop");
        addShopSession(player, shop);

        // Slot 10: Adjust Buy Price
        ItemStack adjustBuy = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta buyMeta = adjustBuy.getItemMeta();
        buyMeta.setDisplayName(TextUtils.formatText("&dAdjust Buy Price"));
        buyMeta.setLore(Arrays.asList(TextUtils.formatText("&7Current&8: &d" + shop.getBuyPrice()), TextUtils.formatText("&7Set to 0 to disable")));
        adjustBuy.setItemMeta(buyMeta);
        gui.setItem(10, adjustBuy);

        // Slot 12: Adjust Sell Price
        ItemStack adjustSell = new ItemStack(Material.IRON_NUGGET);
        ItemMeta sellMeta = adjustSell.getItemMeta();
        sellMeta.setDisplayName(TextUtils.formatText("&dAdjust Sell Price"));
        sellMeta.setLore(Arrays.asList(TextUtils.formatText("&7Current&8: &d" + shop.getSellPrice()), TextUtils.formatText("&7Set to 0 to disable")));
        adjustSell.setItemMeta(sellMeta);
        gui.setItem(12, adjustSell);

        // Slot 14: Toggle Enable/Disable
        ItemStack toggle = new ItemStack(Material.LEVER);
        ItemMeta toggleMeta = toggle.getItemMeta();
        toggleMeta.setDisplayName(TextUtils.formatText("&dToggle Shop"));
        toggleMeta.setLore(Arrays.asList(TextUtils.formatText("&7Current&8: &d" + (shop.isEnabled() ? "Enabled" : "Disabled"))));
        toggle.setItemMeta(toggleMeta);
        gui.setItem(14, toggle);

        // Slot 16: Manage Stock
        ItemStack stock = new ItemStack(Material.CHEST);
        ItemMeta stockMeta = stock.getItemMeta();
        stockMeta.setDisplayName(TextUtils.formatText("&dManage Stock"));
        stockMeta.setLore(Arrays.asList(TextUtils.formatText("&7Current stock&8: &d" + shop.getStock())));
        stock.setItemMeta(stockMeta);
        gui.setItem(16, stock);

        //Slot 26: Delete Shop
        ItemStack deleteButton = new ItemStack(Material.TNT);
        ItemMeta deleteMeta = deleteButton.getItemMeta();
        deleteMeta.setDisplayName(ChatColor.RED + "Delete Shop");
        deleteMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to delete this shop.",
                ChatColor.RED + "Warning: This action cannot be undone!"
        ));
        deleteButton.setItemMeta(deleteMeta);
        gui.setItem(31, deleteButton);

        GuiUtils.fillEmptySlots(gui);

        player.openInventory(gui);
    }

    public static void saveShops() {

        for (Map.Entry<Location, Shop> entry : shops.entrySet()) {
            Location loc = entry.getKey();
            Shop shop = entry.getValue();
            String locKey = locationToString(loc);

            plugin.getDataFile().set(locKey + ".owner", shop.getOwner().toString());
            plugin.getDataFile().set(locKey + ".item", shop.getItem().getType().name());
            plugin.getDataFile().set(locKey + ".buyPrice", shop.getBuyPrice());
            plugin.getDataFile().set(locKey + ".sellPrice", shop.getSellPrice());
            plugin.getDataFile().set(locKey + ".stock", shop.getStock());
            plugin.getDataFile().set(locKey + ".isEnabled", shop.isEnabled());
            plugin.getDataFile().set(locKey + ".isAdminShop", shop.isAdminShop());
            plugin.getDataFile().set(locKey + ".signLocation", locationToString(shop.getSignLocation()));
            List<String> managerUUIDs = shop.getManagers().stream().map(UUID::toString).toList();
            plugin.getDataFile().set(locKey + ".managers", managerUUIDs);
        }

        plugin.getDataFileRaw().save();
    }

    public static void loadShops() {

        for (String locKey : plugin.getDataFile().getKeys(false)) {
            Location loc = stringToLocation(locKey);
            UUID owner = UUID.fromString(plugin.getDataFile().getString(locKey + ".owner"));
            String itemTypeString = plugin.getDataFile().getString(locKey + ".item");
            ItemStack item = new ItemStack(Material.valueOf(itemTypeString));
            double buyPrice = plugin.getDataFile().getDouble(locKey + ".buyPrice");
            double sellPrice = plugin.getDataFile().getDouble(locKey + ".sellPrice");
            int stock = plugin.getDataFile().getInt(locKey + ".stock");
            boolean isEnabled = plugin.getDataFile().getBoolean(locKey + ".isEnabled");
            boolean isAdminShop = plugin.getDataFile().getBoolean(locKey + ".isAdminShop");
            Location signLocation = stringToLocation(plugin.getDataFile().getString(locKey + ".signLocation"));
            List<String> managers = plugin.getDataFile().getStringList(locKey + ".managers");


            Shop shop = new Shop(owner, loc, item, buyPrice, sellPrice, isAdminShop);
            shop.setStock(stock);
            shop.setEnabled(isEnabled);
            shop.setSignLocation(signLocation);
            for (String uuid : managers) {
                shop.addManager(UUID.fromString(uuid));
            }
            ShopHologram.updateFloatingItem(shop);

            shops.put(loc, shop);
        }
    }

    private static Map<UUID, Inventory> stockInventories = new HashMap<>();

    public static void openStockManagementGUI(Player player, Shop shop) {
        Inventory stockInventory = Bukkit.createInventory(player, 54, ChatColor.DARK_GREEN + "Adjust Stock");
        addShopSession(player, shop);

        ItemStack shopItem = shop.getItem();
        int stock = shop.getStock();

        while (stock > 0) {
            int amount = Math.min(stock, shopItem.getMaxStackSize());
            ItemStack stack = shopItem.clone();
            stack.setAmount(amount);
            stockInventory.addItem(stack);
            stock -= amount;
        }

        stockInventories.put(player.getUniqueId(), stockInventory);
        player.openInventory(stockInventory);
        }

    public static Inventory getStockInventory(Player player) {
        return stockInventories.get(player.getUniqueId());
    }

    public static void removeStockInventory(Player player) {
        stockInventories.remove(player.getUniqueId());
    }

    private static String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static Location stringToLocation(String locString) {
        String[] parts = locString.split(",");
        return new Location(
                Bukkit.getWorld(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3])
        );
    }

    public static List<Shop> getShopsOwnedBy(Player player) {
        List<Shop> ownedShops = new ArrayList<>();
        for (Shop shop : getAllShops()) {
            if (shop.getOwner().equals(player.getUniqueId())) {
                ownedShops.add(shop);
            }
        }
        return ownedShops;
    }

    public static Shop getShopPlayerIsLookingAt(Player player) {
        Block targetBlock = player.getTargetBlockExact(5); // Max 5 blocks away
        if (targetBlock != null) {
            for (Shop shop : getAllShops()) {
                if (shop.getLocation().equals(targetBlock.getLocation())) {
                    return shop;
                }
            }
        }
        return null;
    }
}