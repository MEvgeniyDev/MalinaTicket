package ru.mevgeniy.malinaticket.compat;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class TextAdapter implements AutoCloseable {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final BukkitAudiences audiences;
    private final Logger logger;

    public TextAdapter(JavaPlugin plugin) {
        this(BukkitAudiences.create(plugin), plugin.getLogger());
    }

    TextAdapter(BukkitAudiences audiences, Logger logger) {
        this.audiences = audiences;
        this.logger = logger;
    }

    public void send(CommandSender sender, String raw, Map<String, String> placeholders) {
        audiences.sender(sender).sendMessage(component(apply(raw, placeholders), logger));
    }

    public String legacy(String raw, Map<String, String> placeholders) {
        return legacy(apply(raw, placeholders), logger);
    }

    public static String escapeTags(String input) {
        return MINI_MESSAGE.escapeTags(input == null ? "" : input);
    }

    static String legacy(String raw, Logger logger) {
        return LEGACY.serialize(component(raw, logger).decoration(TextDecoration.ITALIC, false));
    }

    static Component component(String raw, Logger logger) {
        String rendered = raw == null ? "" : raw;
        try {
            return MINI_MESSAGE.deserialize(rendered);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Ошибка MiniMessage в тексте: " + rendered, exception);
            return Component.text(rendered);
        }
    }

    public static String apply(String raw, Map<String, String> placeholders) {
        String result = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    @Override
    public void close() {
        audiences.close();
    }
}
