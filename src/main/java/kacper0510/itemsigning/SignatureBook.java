package kacper0510.itemsigning;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SignatureBook {
    private final @NotNull Component signature;
    private final @NotNull List<TextComponent> comment;
    private final boolean hideFlags, enchanted;

    private SignatureBook(@NotNull final ItemStack book) throws SignatureException {
        var bookMeta = (BookMeta) book.getItemMeta();
        if (!bookMeta.hasAuthor()) throw new SignatureException("The book doesn't have an author!");
        signature = Objects.requireNonNull(bookMeta.author())
                .colorIfAbsent(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, false);

        var generation = bookMeta.hasGeneration() ? Objects.requireNonNull(bookMeta.getGeneration()) : BookMeta.Generation.ORIGINAL;
        if (generation.equals(BookMeta.Generation.COPY_OF_COPY)) throw new SignatureException("This copy cannot be used as a signature!");

        var text = bookMeta.pages().stream()
                .map(LegacyComponentSerializer.legacySection()::serialize)
                .flatMap(Pattern.compile("\n")::splitAsStream)
                .collect(Collectors.toList());

        hideFlags = text.removeIf("[hideflags]"::equalsIgnoreCase);
        enchanted = text.removeIf("[enchanted]"::equalsIgnoreCase);

        boolean allowCopies = text.removeIf("[allowcopies]"::equalsIgnoreCase);
        if (!allowCopies && generation.equals(BookMeta.Generation.COPY_OF_ORIGINAL)) throw new SignatureException("This copy cannot be used as a signature!");
        // TODO: name style, multisign, custom model, custom potion color?

        var loreIndex = IntStream.range(0, text.size())
                .filter(i -> text.get(i).equalsIgnoreCase("[lore]"))
                .findFirst()
                .orElse(-1);
        comment = text.subList(loreIndex + 1, text.size()).stream()
                .map(LegacyComponentSerializer.legacySection()::deserialize)
                .map(comp -> (TextComponent) comp.compact())
                .filter(comp -> !comp.content().isBlank())
                .limit(5)
                .map(comp -> comp.colorIfAbsent(NamedTextColor.GRAY))
                .toList();
    }

    public static SignatureBook newInstance(@Nullable final ItemStack book) throws SignatureException, InstantiationException {
        if (isInvalid(book)) throw new InstantiationException();
        return new SignatureBook(book);
    }

    public ItemStack sign(@NotNull final ItemStack item) throws SignatureException {
        var alreadySigned = item.getItemMeta().getPersistentDataContainer().get(
                Objects.requireNonNull(NamespacedKey.fromString("itemsigning:signed")),
                PersistentDataType.BYTE
        );
        if (alreadySigned != null && alreadySigned > 0) throw new SignatureException("This item was already signed!");

        var signed = item.clone();
        var signature = Component.text("Signed by: ")
                .color(NamedTextColor.DARK_GREEN)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false)
                .append(this.signature);
        var lore = new ArrayList<>(comment);
        lore.add(Component.empty());
        lore.add(signature);
        signed.lore(lore);

        if (hideFlags) signed.addItemFlags(ItemFlag.values());
        if (enchanted && item.getEnchantments().size() == 0) {
            if (item.getType().equals(Material.FISHING_ROD)) {
                signed.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
            } else {
                signed.addUnsafeEnchantment(Enchantment.LUCK, 1); // Luck of the Sea, so it doesn't do anything
            }
            signed.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        signed.editMeta(meta -> meta.getPersistentDataContainer().set(
                Objects.requireNonNull(NamespacedKey.fromString("itemsigning:signed")),
                PersistentDataType.BYTE,
                (byte) 1
        ));

        return signed;
    }

    public static boolean isInvalid(@Nullable final ItemStack book) {
        return book == null || !book.getType().equals(Material.WRITTEN_BOOK);
    }

    public static class SignatureException extends Exception {
        public SignatureException(String message) {
            super(message);
        }

        public Component getMessageAsComponent() {
            return Component.text(getMessage()).color(NamedTextColor.RED);
        }
    }
}
