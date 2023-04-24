package kacper0510.itemsigning;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemSigning extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onSmithingEvent(PrepareSmithingEvent event) {
        ItemStack book = event.getInventory().getInputMineral();
        ItemStack item = event.getInventory().getInputEquipment();

        if (book == null || item == null) return;
        if (!book.getType().equals(Material.WRITTEN_BOOK)) return;
        // TODO: original check

        event.setResult(item.clone());
    }

    @EventHandler
    public void onSmithingCraftingEvent(SmithItemEvent event) {
        ItemStack book = event.getInventory().getInputMineral();
        if (book == null || !book.getType().equals(Material.WRITTEN_BOOK)) return;

    }
}
