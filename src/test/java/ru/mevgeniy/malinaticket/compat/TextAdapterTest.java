package ru.mevgeniy.malinaticket.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

class TextAdapterTest {
    private final Logger logger = Logger.getLogger("MalinaTicketTextAdapterTest");

    @Test
    void convertsMiniMessageToLegacyGuiText() {
        String text = TextAdapter.legacy("<#FF3366>MalinaTicket", logger);

        assertTrue(text.contains("MalinaTicket"));
        assertTrue(text.contains("\u00A7"));
        assertEquals(TextDecoration.State.FALSE, TextAdapter.guiComponent("<#FF3366>MalinaTicket", logger).decoration(TextDecoration.ITALIC));
    }

    @Test
    void acceptsClickAndHoverTagsForChatMessages() {
        var component = TextAdapter.component("<click:run_command:'/ticket'><hover:show_text:'Открыть'>[Открыть]</hover></click>", logger);

        assertEquals("/ticket", component.clickEvent().value());
        assertTrue(component.hoverEvent().value().toString().contains("Открыть"));
        String json = TextAdapter.componentJson(component);
        assertTrue(json.contains("\"clickEvent\""));
        assertTrue(json.contains("\"click_event\""));
        assertTrue(json.contains("\"hoverEvent\""));
        assertTrue(json.contains("\"hover_event\""));
    }

    @Test
    void expandsTemplateColorsAndKeepsPlayerTextLiteral() {
        String playerText = "{#123ABC} <red> \\<bold> %id%";
        String rendered = TextAdapter.apply("{#D94F70}%message%", Map.of("message", TextAdapter.escapeTags(playerText), "id", "101"));
        assertTrue(rendered.startsWith("<#D94F70>"));
        assertEquals(playerText, PlainTextComponentSerializer.plainText().serialize(TextAdapter.component(rendered, logger)));
    }
}
