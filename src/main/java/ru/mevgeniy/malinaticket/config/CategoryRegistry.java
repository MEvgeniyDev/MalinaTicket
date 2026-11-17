package ru.mevgeniy.malinaticket.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mevgeniy.malinaticket.model.TicketCategory;

public final class CategoryRegistry {
    private final JavaPlugin plugin;
    private final Map<String, TicketCategory> byId = new HashMap<>();
    private final List<TicketCategory> ordered = new ArrayList<>();

    public CategoryRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "categories.yml");
        if (!file.exists()) {
            plugin.saveResource("categories.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        byId.clear();
        ordered.clear();

        ConfigurationSection section = config.getConfigurationSection("categories");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            TicketCategory category = categoryFromConfig(config, id);
            byId.put(id.toLowerCase(Locale.ROOT), category);
            ordered.add(category);
        }
        ordered.sort(Comparator.comparingInt(TicketCategory::slot).thenComparing(TicketCategory::id));
    }

    public List<TicketCategory> all() {
        return List.copyOf(ordered);
    }

    public TicketCategory first() {
        return ordered.isEmpty() ? null : ordered.get(0);
    }

    public TicketCategory byId(String id) {
        if (id == null) {
            return first();
        }
        return byId.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean canCreate(Player player, TicketCategory category) {
        return category != null && (category.permission().isBlank() || player.hasPermission(category.permission()));
    }

    static TicketCategory categoryFromConfig(FileConfiguration config, String id) {
        String base = "categories." + id + ".";
        Integer customModelData = config.isInt(base + "custom-model-data") ? config.getInt(base + "custom-model-data") : null;
        String color = config.getString(base + "color", "#D94F70");
        if (color.startsWith("{#") && color.endsWith("}")) {
            color = color.substring(1, color.length() - 1);
        }
        return new TicketCategory(
                id,
                config.getString(base + "name", id),
                config.getString(base + "material", "PAPER"),
                config.getInt(base + "slot", 10),
                color,
                config.getString(base + "permission", "malinaticket.category." + id + ".create"),
                config.getStringList(base + "description"),
                safeAmount(config.getInt(base + "amount", 1)),
                config.getBoolean(base + "glow", false),
                customModelData
        );
    }

    static int safeAmount(int amount) {
        return amount < 1 || amount > 64 ? 1 : amount;
    }
}
