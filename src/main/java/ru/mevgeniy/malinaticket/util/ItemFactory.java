package ru.mevgeniy.malinaticket.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.mevgeniy.malinaticket.compat.ItemAdapter;
import ru.mevgeniy.malinaticket.config.GuiItemConfig;
import ru.mevgeniy.malinaticket.config.MessageService;

public final class ItemFactory {
    private final MessageService messages;
    private final Logger logger;

    public ItemFactory(MessageService messages) {
        this(messages, Logger.getLogger(ItemFactory.class.getName()));
    }

    public ItemFactory(MessageService messages, Logger logger) {
        this.messages = messages;
        this.logger = logger;
    }

    public ItemStack build(GuiItemConfig item, Map<String, String> placeholders) {
        return build(item.material(), item.name(), item.lore(), placeholders, item.amount(), item.glow(), item.customModelData());
    }

    public ItemStack build(String materialName, String name, List<String> lore, Map<String, String> placeholders) {
        return build(materialName, name, lore, placeholders, 1, false, null);
    }

    public ItemStack build(String materialName, String name, List<String> lore, Map<String, String> placeholders, int amount, boolean glow, Integer customModelData) {
        Material material = parseMaterial(materialName, Material.PAPER);
        ItemStack item = new ItemStack(material);
        item.setAmount(clampAmount(amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            logger.warning("Материал " + material + " не поддерживает ItemMeta. Используется PAPER.");
            item = new ItemStack(Material.PAPER, clampAmount(amount));
            meta = item.getItemMeta();
        }
        meta.setDisplayName(messages.legacyFromText(name, placeholders));
        List<String> renderedLore = new ArrayList<>();
        for (String line : lore) {
            for (String part : splitRenderedLine(line, placeholders)) {
                renderedLore.add(messages.legacyFromText(part, Map.of()));
            }
        }
        meta.setLore(renderedLore);
        if (glow) {
            ItemAdapter.applyGlow(meta);
        }
        ItemAdapter.applyCustomModelData(meta, customModelData, logger);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack named(Material material, String name, List<String> lore, Map<String, String> placeholders) {
        return build(material.name(), name, lore, placeholders);
    }

    public Material parseMaterial(String materialName, Material fallback) {
        if (materialName == null || materialName.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(materialName.trim());
        if (material == null || material.isAir()) {
            logger.warning("Неверный GUI material '" + materialName + "'. Используется " + fallback + ".");
            return fallback;
        }
        return material;
    }

    private List<String> splitRenderedLine(String line, Map<String, String> placeholders) {
        String rendered = messages.applyPlaceholders(line, placeholders);
        return Arrays.asList(rendered.split("\\R", -1));
    }

    private int clampAmount(int amount) {
        if (amount < 1 || amount > 64) {
            logger.warning("GUI amount должен быть от 1 до 64. Получено: " + amount + ".");
            return Math.max(1, Math.min(64, amount));
        }
        return amount;
    }
}
