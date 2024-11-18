package io.captainyannick.advancedShops.core.utils;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiUtils {

    /**
     * Fills all empty slots in the given inventory with Black Stained Glass Pane.
     *
     * @param inventory The inventory to fill.
     */
    public static void fillEmptySlots(Inventory inventory) {
        ItemStack filler = createFillerItem();

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    /**
     * Creates a filler item (Black Stained Glass Pane).
     *
     * @return The filler item.
     */
    private static ItemStack createFillerItem() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        return filler;
    }
}
