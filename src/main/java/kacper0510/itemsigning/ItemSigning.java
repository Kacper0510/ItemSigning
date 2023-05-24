package kacper0510.itemsigning;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import sun.misc.Unsafe;

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
    public void onAnvilEvent(PrepareAnvilEvent event) {
        ItemStack book = event.getInventory().getSecondItem();
        ItemStack item = event.getInventory().getFirstItem();
        if (item == null) return;

        try {
            var sb = SignatureBook.newInstance(book);
            var result = sb.sign(item);
            result.setAmount(1);
            event.setResult(result);
            event.getInventory().setRepairCost(0);
        } catch (SignatureBook.SignatureException ex) {
            event.getView().getPlayer().sendActionBar(ex.getMessageAsComponent());
            event.setResult(null);
        } catch (InstantiationException ignored) { // Not a book
            event.setResult(null);
        }
    }

    // Inspired by https://github.com/WolfyScript/CustomCrafting/blob/master/src/main/java/me/wolfyscript/customcrafting/listeners/AnvilListener.java
    @EventHandler
    public void onAnvilCraftingEvent(InventoryClickEvent event) {
        if (!event.getInventory().getType().equals(InventoryType.ANVIL)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        AnvilInventory inv = (AnvilInventory) event.getInventory();
        ItemStack item = inv.getFirstItem();
        ItemStack book = inv.getSecondItem();
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir() || item == null) return;
        if (SignatureBook.isInvalid(book)) return;

        int left = item.getAmount() - 1;
        if (event.isShiftClick()) {
            var newBase = event.getWhoClicked().getInventory().addItem(result.asQuantity(item.getAmount())).get(0);
            left = newBase != null ? newBase.getAmount() : 0;
            if (left == item.getAmount()) return; // Full inventory
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
            location.getWorld().playSound(location, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1, 1);
        }
        event.setCancelled(true);
    }
}
