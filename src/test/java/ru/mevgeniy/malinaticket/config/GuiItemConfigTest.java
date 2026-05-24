package ru.mevgeniy.malinaticket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuiItemConfigTest {
    @Test
    void keepsOptionalItemPresentationFields() {
        GuiItemConfig item = new GuiItemConfig(10, "PAPER", "Name", List.of("Lore"), 3, true, 123);

        assertEquals(10, item.slot());
        assertEquals(3, item.amount());
        assertTrue(item.glow());
        assertEquals(123, item.customModelData());
    }

    @Test
    void defaultConstructorKeepsOldGuiConfigShape() {
        GuiItemConfig item = new GuiItemConfig(10, "PAPER", "Name", null);

        assertEquals(List.of(), item.lore());
        assertEquals(1, item.amount());
        assertFalse(item.glow());
        assertNull(item.customModelData());
    }
}
