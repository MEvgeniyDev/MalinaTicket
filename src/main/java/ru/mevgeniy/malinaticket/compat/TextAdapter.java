package ru.mevgeniy.malinaticket.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class TextAdapter implements AutoCloseable {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX_COLOR = Pattern.compile("\\{#([0-9a-fA-F]{6})}");
    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z][a-zA-Z0-9_]*)%");
    private static final GsonComponentSerializer COMPONENT_JSON = GsonComponentSerializer.builder()
            .editOptions(options -> options
                    .value(JSONOptions.EMIT_CLICK_EVENT_TYPE, JSONOptions.ClickEventValueMode.BOTH)
                    .value(JSONOptions.EMIT_HOVER_EVENT_TYPE, JSONOptions.HoverEventValueMode.BOTH))
            .build();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final Logger logger;
    private final JavaPlugin plugin;
    private BukkitAudiences audiences;
    private final Method nativeSend;
    private final Method nativeDeserialize;
    private final Object nativeSerializer;
    private final Method nativeDisplayName;
    private final Method nativeLore;

    public TextAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        Method send = null;
        Method deserialize = null;
        Object serializer = null;
        Method displayName = null;
        Method lore = null;
        try {
            Class<?> nativeComponent = Class.forName("net.kyori.adventure.text.Component");
            Class<?> serializerType = Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
            serializer = serializerType.getMethod("gson").invoke(null);
            try {
                deserialize = serializerType.getMethod("deserialize", String.class);
            } catch (NoSuchMethodException ignored) {
                deserialize = serializerType.getMethod("deserialize", Object.class);
            }
            send = CommandSender.class.getMethod("sendMessage", nativeComponent);
            displayName = ItemMeta.class.getMethod("displayName", nativeComponent);
            lore = ItemMeta.class.getMethod("lore", List.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logger.fine("Штатный Adventure Paper недоступен: " + exception.getMessage());
        }
        this.nativeSend = send;
        this.nativeDeserialize = deserialize;
        this.nativeSerializer = serializer;
        this.nativeDisplayName = displayName;
        this.nativeLore = lore;
    }

    public void send(CommandSender sender, String raw, Map<String, String> placeholders) {
        sendPrepared(sender, apply(raw, placeholders));
    }

    public void sendPrepared(CommandSender sender, String raw) {
        Component rendered = component(raw, logger);
        if (nativeSend != null) {
            try {
                nativeSend.invoke(sender, nativeComponent(rendered));
                return;
            } catch (ReflectiveOperationException | LinkageError exception) {
                logger.log(Level.FINE, "Не удалось отправить сообщение через штатный Adventure.", exception);
            }
        }
        if (audiences == null) {
            audiences = BukkitAudiences.create(plugin);
        }
        audiences.sender(sender).sendMessage(rendered);
    }

    public String legacy(String raw, Map<String, String> placeholders) {
        return legacyPrepared(apply(raw, placeholders));
    }

    public String legacyPrepared(String raw) {
        return legacy(raw, logger);
    }

    public void applyItemText(ItemMeta meta, String name, List<String> lore) {
        List<String> visibleLore = lore.size() <= 256 ? lore : lore.subList(0, 256);
        if (nativeDisplayName != null && nativeLore != null) {
            try {
                Object displayName = nativeComponent(guiComponent(name, logger));
                List<Object> components = new ArrayList<>(visibleLore.size());
                for (String line : visibleLore) {
                    components.add(nativeComponent(guiComponent(line, logger)));
                }
                nativeDisplayName.invoke(meta, displayName);
                nativeLore.invoke(meta, components);
                return;
            } catch (ReflectiveOperationException | LinkageError exception) {
                logger.log(Level.FINE, "Не удалось применить оформление предмета через Adventure.", exception);
            }
        }
        meta.setDisplayName(legacyPrepared(name));
        meta.setLore(visibleLore.stream().map(this::legacyPrepared).toList());
    }

    private Object nativeComponent(Component component) throws ReflectiveOperationException {
        return nativeDeserialize.invoke(nativeSerializer, componentJson(component));
    }

    public static String escapeTags(String input) {
        return input == null ? "" : input.replace("\\", "\\\\").replace("<", "\\<");
    }

    static String legacy(String raw, Logger logger) {
        return LEGACY.serialize(guiComponent(raw, logger));
    }

    static Component guiComponent(String raw, Logger logger) {
        return component(raw, logger).decoration(TextDecoration.ITALIC, false);
    }

    static Component component(String raw, Logger logger) {
        String rendered = raw == null ? "" : raw;
        try {
            return MINI_MESSAGE.deserialize(rendered);
        } catch (RuntimeException exception) {
            logger.log(Level.FINE, "Не удалось обработать шаблон MiniMessage.", exception);
            return Component.text(rendered);
        }
    }

    static String componentJson(Component component) {
        return COMPONENT_JSON.serialize(component);
    }

    public static String apply(String raw, Map<String, String> placeholders) {
        String template = HEX_COLOR.matcher(raw == null ? "" : raw).replaceAll("<#$1>");
        return PLACEHOLDER.matcher(template).replaceAll(token -> Matcher.quoteReplacement(
                placeholders.getOrDefault(token.group(1), token.group())));
    }

    @Override
    public void close() {
        if (audiences != null) {
            audiences.close();
        }
    }
}
