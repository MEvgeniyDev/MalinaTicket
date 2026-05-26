package ru.mevgeniy.malinaticket.compat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class TextAdapterTest {
    private final Logger logger = Logger.getLogger("MalinaTicketTextAdapterTest");

    @Test
    void convertsMiniMessageToLegacyGuiText() {
        String text = TextAdapter.legacy("<#FF3366>MalinaTicket", logger);

        assertTrue(text.contains("MalinaTicket"));
        assertTrue(text.contains("\u00A7"));
    }

    @Test
    void acceptsClickAndHoverTagsForChatMessages() {
        String text = TextAdapter.legacy("<click:run_command:'/ticket'><hover:show_text:'Открыть'>[Открыть]</hover></click>", logger);

        assertTrue(text.contains("[Открыть]"));
    }
}
