package ru.mevgeniy.malinaticket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import ru.mevgeniy.malinaticket.model.TicketCategory;

class CategoryRegistryTest {
    @Test
    void categoryDefaultsKeepOldYamlCompatible() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("categories.bug.name", "Баг");

        TicketCategory category = CategoryRegistry.categoryFromConfig(config, "bug");

        assertEquals(1, category.amount());
        assertFalse(category.glow());
        assertNull(category.customModelData());
    }

    @Test
    void categoryAmountFallsBackWhenOutOfRange() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("categories.bug.amount", 0);
        assertEquals(1, CategoryRegistry.categoryFromConfig(config, "bug").amount());

        config.set("categories.bug.amount", 65);
        assertEquals(1, CategoryRegistry.categoryFromConfig(config, "bug").amount());
    }

    @Test
    void categoryKeepsGuiPresentationFields() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("categories.bug.amount", 4);
        config.set("categories.bug.glow", true);
        config.set("categories.bug.custom-model-data", 1207);

        TicketCategory category = CategoryRegistry.categoryFromConfig(config, "bug");
        GuiItemConfig item = new GuiItemConfig(
                category.slot(),
                category.material(),
                category.displayName(),
                category.description(),
                category.amount(),
                category.glow(),
                category.customModelData()
        );

        assertEquals(4, item.amount());
        assertTrue(item.glow());
        assertEquals(1207, item.customModelData());
    }
}
