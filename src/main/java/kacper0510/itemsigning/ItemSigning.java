package kacper0510.itemsigning;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public final class ItemSigning extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private boolean bookCheckFailure(final ItemStack book) {
        return book == null || !book.getType().equals(Material.WRITTEN_BOOK);
        // TODO: not second copy, not already signed in that way
    }

    @EventHandler
    public void onSmithingEvent(PrepareSmithingEvent event) {
        ItemStack book = event.getInventory().getInputMineral();
        ItemStack item = event.getInventory().getInputEquipment();
        if (item == null || item.getType().equals(Material.WRITTEN_BOOK) || bookCheckFailure(book)) return;

        var result = item.clone();
        result.setAmount(1);
        event.setResult(result);
    }

    // Inspired by https://github.com/WolfyScript/CustomCrafting/blob/master/src/main/java/me/wolfyscript/customcrafting/listeners/SmithingListener.java
    @EventHandler
    public void onSmithingCraftingEvent(SmithItemEvent event) {
        ItemStack item = Objects.requireNonNull(event.getInventory().getInputEquipment());
        ItemStack book = event.getInventory().getInputMineral();
        ItemStack result = Objects.requireNonNull(event.getCurrentItem());
        if (bookCheckFailure(book)) return;

        int left = item.getAmount() - 1;
        if (event.isShiftClick()) {
            var newBase = event.getWhoClicked().getInventory().addItem(result.asQuantity(item.getAmount())).get(0);
            left = newBase != null ? newBase.getAmount() : 0;
            if (left == item.getAmount()) return; // Full inventory
        } else if (event.getClick().equals(ClickType.DROP)) {
            var playerLocation = event.getWhoClicked().getLocation();
            var droppedItem = playerLocation.getWorld().dropItem(playerLocation, result);
            droppedItem.setThrower(event.getWhoClicked().getUniqueId());
            droppedItem.setPickupDelay(40);
        } else if (event.getClick().equals(ClickType.CONTROL_DROP)) {
            var playerLocation = event.getWhoClicked().getLocation();
            var droppedItem = playerLocation.getWorld().dropItem(playerLocation, result.asQuantity(item.getAmount()));
            droppedItem.setThrower(event.getWhoClicked().getUniqueId());
            droppedItem.setPickupDelay(40);
            left = 0;
        } else {
            var cursor = event.getCursor();
            if (cursor == null || cursor.getType().equals(Material.AIR)) { // Empty cursor
                event.getView().setCursor(result);
            } else if (cursor.isSimilar(result) && cursor.getAmount() != cursor.getMaxStackSize()) { // The same item in cursor
                cursor.add();
            } else { // Any other item
                return;
            }
        }

        item.setAmount(left);
        if (left == 0) {
            event.setCurrentItem(null);
        }
        var location = event.getInventory().getLocation();
        if (location != null) {
            location.getWorld().playSound(location, Sound.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1, 1);
        }
    }
}
