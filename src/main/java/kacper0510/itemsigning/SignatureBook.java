package kacper0510.itemsigning;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

public class SignatureBook {
    private final @NotNull Component signature;

    public SignatureBook(@NotNull final ItemStack book) throws SignatureException {
        var bookMeta = (BookMeta) book.getItemMeta();
        if (!bookMeta.hasAuthor()) throw new SignatureException("The book doesn't have an author!");
        signature = Objects.requireNonNull(bookMeta.author());

        var generation = bookMeta.hasGeneration() ? Objects.requireNonNull(bookMeta.getGeneration()) : BookMeta.Generation.ORIGINAL;
        if (generation.equals(BookMeta.Generation.COPY_OF_COPY)) throw new SignatureException("This copy cannot be used as a signature!");

        var text = bookMeta.pages().stream()
                .map(LegacyComponentSerializer.legacySection()::serialize)
                .flatMap(Pattern.compile("\n")::splitAsStream)
                .toList();

        // decorationifabsent
    }

    public static boolean isInvalid(@Nullable final ItemStack book) {
        return book == null || !book.getType().equals(Material.WRITTEN_BOOK);
    }

    public static class SignatureException extends Exception {
        public SignatureException(String message) {
            super(message);
        }
    }
}
