package ru.mevgeniy.malinaticket.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketMessage;
import ru.mevgeniy.malinaticket.model.TicketStatus;

class GuiTextFormatterTest {
    private final GuiTextFormatter formatter = new GuiTextFormatter("yyyy-MM-dd HH:mm", "UTC");

    @Test
    void wrapsPlainTextWithoutLosingLongWords() {
        assertEquals(List.of("abc", "def"), formatter.wrapPlain("abcdef", 3));
        assertEquals(List.of("one two", "three"), formatter.wrapPlain("one two three", 7));
    }

    @Test
    void prefixesWrappedLoreLinesWithColor() {
        String wrapped = formatter.wrapPlaceholder("alpha beta gamma", "<#6B5961>", 10);

        assertEquals("<#6B5961>alpha beta\n<#6B5961>gamma", wrapped);
    }

    @Test
    void rendersTicketMessageLoreWithStableTime() {
        UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Ticket ticket = new Ticket(
                5,
                owner,
                "Player",
                TicketStatus.OPEN,
                "bug",
                3,
                1_700_000_000_000L,
                1_700_000_000_000L,
                null,
                List.of(new TicketMessage(owner, "Player", false, 1_700_000_000_000L, "Первое сообщение"))
        );

        List<String> lore = formatter.messageLore(ticket);

        assertTrue(lore.get(0).contains("Player"));
        assertTrue(lore.get(0).contains("2023-11-14 22:13"));
        assertEquals("<#FBE8EE>Первое сообщение", lore.get(1));
    }

    @Test
    void priorityPresentationIsStable() {
        assertEquals("RED_WOOL", formatter.priorityMaterial(5));
        assertEquals("срочная", formatter.priorityName(5));
        assertEquals("<#E05A47>", formatter.priorityColor(5));
    }
}
