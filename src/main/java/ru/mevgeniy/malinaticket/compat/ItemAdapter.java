package ru.mevgeniy.malinaticket.compat;

import java.util.logging.Logger;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemAdapter {
    private ItemAdapter() {
    }

    public static void applyGlow(ItemMeta meta) {
        Enchantment glowEnchant = glowEnchantment();
        if (glowEnchant == null) {
            return;
        }
        meta.addEnchant(glowEnchant, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    public static void applyCustomModelData(ItemMeta meta, Integer customModelData, Logger logger) {
        if (customModelData == null) {
            return;
        }
        if (customModelData < 0) {
            logger.warning("custom-model-data не может быть меньше 0: " + customModelData);
            return;
        }
        meta.setCustomModelData(customModelData);
    }

    @SuppressWarnings("deprecation")
    private static Enchantment glowEnchantment() {
        for (String name : new String[]{"DURABILITY", "UNBREAKING"}) {
            Enchantment enchantment = Enchantment.getByName(name);
            if (enchantment != null) {
                return enchantment;
            }
            try {
                Object value = Enchantment.class.getField(name).get(null);
                if (value instanceof Enchantment reflected) {
                    return reflected;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        Enchantment[] values = Enchantment.values();
        return values.length == 0 ? null : values[0];
    }
}
