package ru.mevgeniy.malinaticket.compat;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemAdapter {
    private static final Method GLINT_OVERRIDE = findGlintOverride();

    private ItemAdapter() {
    }

    public static void applyGlow(ItemMeta meta) {
        if (GLINT_OVERRIDE != null) {
            try {
                GLINT_OVERRIDE.invoke(meta, true);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Используем обычное зачарование, если ядро не применило свечение.
            }
        }
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
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (enchantment != null) {
            return enchantment;
        }
        try {
            return (Enchantment) Enchantment.class.getField("DURABILITY").get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method findGlintOverride() {
        try {
            return ItemMeta.class.getMethod("setEnchantmentGlintOverride", Boolean.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
