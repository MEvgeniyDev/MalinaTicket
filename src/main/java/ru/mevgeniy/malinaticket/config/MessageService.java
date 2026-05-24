package ru.mevgeniy.malinaticket.config;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageService {
    private static final String MISSING_PREFIX = "<#E05A47>Сообщение не найдено: ";

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration config;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Collections.emptyMap());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(component(path, placeholders));
    }

    public Component component(String path, Map<String, String> placeholders) {
        String raw = config == null ? MISSING_PREFIX + path : config.getString(path, MISSING_PREFIX + path);
        return componentFromText(raw, placeholders);
    }

    public String text(String path, Map<String, String> placeholders) {
        String raw = config == null ? MISSING_PREFIX + path : config.getString(path, MISSING_PREFIX + path);
        return apply(raw, placeholders);
    }

    public Component componentFromText(String raw, Map<String, String> placeholders) {
        String rendered = apply(raw, placeholders);
        try {
            return miniMessage.deserialize(rendered);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Ошибка MiniMessage в тексте: " + rendered, exception);
            return Component.text(rendered);
        }
    }

    public String applyPlaceholders(String raw, Map<String, String> placeholders) {
        return apply(raw, placeholders);
    }

    private String apply(String raw, Map<String, String> placeholders) {
        String result = raw == null ? "" : raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
}
