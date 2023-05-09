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
import sun.misc.Unsafe;

import java.util.Objects;

public final class ItemSigning extends JavaPlugin implements Listener {

    // Disables an Adventure API warning through very unsafe reflection.
    // We can't guarantee that a user doesn't use section signs in a book and that would cause a warning in the console.
    // https://stackoverflow.com/questions/56039341/get-declared-fields-of-java-lang-reflect-fields-in-jdk12/71465198#71465198
    private void disableAdventureLegacyWarning() {
        try {
            var warningField = Class.forName("net.kyori.adventure.text.TextComponentImpl")
                                    .getDeclaredField("WARN_WHEN_LEGACY_FORMATTING_DETECTED");

            var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            var unsafe = (Unsafe) unsafeField.get(null);
            unsafeField.setAccessible(false);

            Object fieldBase = unsafe.staticFieldBase(warningField);
            long fieldOffset = unsafe.staticFieldOffset(warningField);

            unsafe.putBoolean(fieldBase, fieldOffset, false);
        } catch (Exception ex) {
            getLogger().warning("Couldn't remove Adventure warnings!");
        }
    }

    @Override
    public void onEnable() {
        disableAdventureLegacyWarning();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onSmithingEvent(PrepareSmithingEvent event) {
        ItemStack book = event.getInventory().getInputMineral();
        ItemStack item = event.getInventory().getInputEquipment();
        if (item == null || SignatureBook.isInvalid(book)) return;

        try {
            new SignatureBook(book);
        } catch (SignatureBook.SignatureException e) {
            throw new RuntimeException(e);
        }
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
        if (SignatureBook.isInvalid(book)) return;

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
