package io.captainyannick.advancedShops.shop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Shop {
    private UUID owner;
    private Location location;
    private ItemStack item;
    private double buyPrice;
    private double sellPrice;
    private int stock;
    private boolean isEnabled;
    private boolean isAdminShop;
    private List<UUID> managers;
    private Location signLocation;

    public Shop(UUID owner, Location location, ItemStack item, double buyPrice, double sellPrice, boolean isAdminShop) {
        this.owner = owner;
        this.location = location;
        this.item = item;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.isAdminShop = isAdminShop;
        this.isEnabled = true;
        this.managers = new ArrayList<>();
    }

    // Getter and Setter methods
    public UUID getOwner() { return owner; }
    public Location getLocation() { return location; }
    public ItemStack getItem() { return item; }
    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }
    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }
    public int getStock() { return stock; }
    public boolean isEnabled() { return isEnabled; }
    public boolean isAdminShop() { return isAdminShop; }
    public String getOwnerName() {return Bukkit.getOfflinePlayer(owner).getName();}

    public void setStock(int stock) { this.stock = stock; }
    public void setEnabled(boolean enabled) { this.isEnabled = enabled; }

    public boolean canBuy() { return isEnabled && (isAdminShop || stock > 0); }
    public boolean canSell() { return isEnabled && (isAdminShop || stock > 0); }

    public List<UUID> getManagers() { return managers; }
    public void addManager(UUID managerId) { managers.add(managerId); }
    public void removeManager(UUID managerId) { managers.remove(managerId); }

    public Location getSignLocation() {
        return signLocation;
    }
    public void setSignLocation(Location signLocation) {
        this.signLocation = signLocation;
    }
}
