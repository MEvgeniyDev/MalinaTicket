package ru.mevgeniy.malinaticket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class GuiSelectionHintTest {
    @Test
    void olderGuiKeepsDefaultHintsAndExplicitBlankHidesThem() {
        YamlConfiguration yaml = new YamlConfiguration();
        assertEquals("<#FBE8EE>Нажми, чтобы выбрать.", GuiConfig.selectionHint(yaml, false));
        assertEquals("<#88C999>Выбрано", GuiConfig.selectionHint(yaml, true));

        yaml.set("menus.create.selection-hint", "{#FBE8EE}Нажмите, чтобы выбрать.");
        yaml.set("menus.create.selected-hint", "");
        assertEquals("{#FBE8EE}Нажмите, чтобы выбрать.", GuiConfig.selectionHint(yaml, false));
        assertEquals("", GuiConfig.selectionHint(yaml, true));
    }
}
