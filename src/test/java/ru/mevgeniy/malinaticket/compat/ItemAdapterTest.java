package ru.mevgeniy.malinaticket.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

class ItemAdapterTest {
    @Test
    void appliesLegacyGlowWithoutModernPaperApi() {
        RecordingMeta meta = new RecordingMeta();

        ItemAdapter.applyGlow(meta.proxy());

        assertTrue(meta.enchanted);
        assertTrue(meta.flags.contains(ItemFlag.HIDE_ENCHANTS));
    }

    @Test
    void appliesCustomModelDataThroughOldItemMetaApi() {
        RecordingMeta meta = new RecordingMeta();

        ItemAdapter.applyCustomModelData(meta.proxy(), 123, Logger.getLogger("MalinaTicketItemAdapterTest"));

        assertEquals(123, meta.customModelData);
    }

    @Test
    void ignoresNegativeCustomModelData() {
        RecordingMeta meta = new RecordingMeta();

        ItemAdapter.applyCustomModelData(meta.proxy(), -1, Logger.getLogger("MalinaTicketItemAdapterTest"));

        assertEquals(null, meta.customModelData);
    }

    private static final class RecordingMeta {
        private boolean enchanted;
        private Integer customModelData;
        private final Set<ItemFlag> flags = new HashSet<>();

        private ItemMeta proxy() {
            return (ItemMeta) Proxy.newProxyInstance(
                    ItemMeta.class.getClassLoader(),
                    new Class<?>[]{ItemMeta.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "addEnchant" -> {
                                enchanted = true;
                                return true;
                            }
                            case "addItemFlags" -> {
                                if (args != null && args.length > 0 && args[0] instanceof ItemFlag[] itemFlags) {
                                    flags.addAll(Arrays.asList(itemFlags));
                                }
                                return null;
                            }
                            case "setCustomModelData" -> {
                                customModelData = (Integer) args[0];
                                return null;
                            }
                            default -> {
                                return defaultValue(method.getReturnType());
                            }
                        }
                    }
            );
        }

        private Object defaultValue(Class<?> type) {
            if (type == boolean.class) {
                return false;
            }
            if (type == int.class) {
                return 0;
            }
            return null;
        }
    }
}
