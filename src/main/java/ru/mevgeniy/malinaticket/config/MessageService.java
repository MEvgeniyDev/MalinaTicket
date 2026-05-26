package ru.mevgeniy.malinaticket.config;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mevgeniy.malinaticket.compat.TextAdapter;

public final class MessageService {
    private static final String MISSING_PREFIX = "<#E05A47>Сообщение не найдено: ";

    private final JavaPlugin plugin;
    private final TextAdapter textAdapter;
    private FileConfiguration config;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.textAdapter = new TextAdapter(plugin);
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
        textAdapter.send(sender, raw(path), placeholders);
    }

    public void sendRaw(CommandSender sender, String raw, Map<String, String> placeholders) {
        textAdapter.send(sender, raw, placeholders);
    }

    public String text(String path, Map<String, String> placeholders) {
        return apply(raw(path), placeholders);
    }

    public String legacyFromText(String raw, Map<String, String> placeholders) {
        return textAdapter.legacy(raw, placeholders);
    }

    public String applyPlaceholders(String raw, Map<String, String> placeholders) {
        return apply(raw, placeholders);
    }

    public String escapeTags(String input) {
        return TextAdapter.escapeTags(input);
    }

    public void close() {
        textAdapter.close();
    }

    private String raw(String path) {
        return config == null ? MISSING_PREFIX + path : config.getString(path, MISSING_PREFIX + path);
    }

    private String apply(String raw, Map<String, String> placeholders) {
        return TextAdapter.apply(raw, placeholders);
    }
}
