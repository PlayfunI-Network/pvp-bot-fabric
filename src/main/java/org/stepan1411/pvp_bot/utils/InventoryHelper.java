package org.stepan1411.pvp_bot.utils;

import net.minecraft.world.entity.player.Inventory;

public class InventoryHelper {
    public static void setSelectedSlot(Inventory inventory, int slot) {
        inventory.selected = slot;
    }

    public static int getSelectedSlot(Inventory inventory) {
        return inventory.selected;
    }
}
