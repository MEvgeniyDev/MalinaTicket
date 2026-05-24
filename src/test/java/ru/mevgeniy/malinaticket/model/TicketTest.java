package ru.mevgeniy.malinaticket.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketTest {
    @Test
    void closeAndReopenKeepTicketStateConsistent() {
        UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID staff = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Ticket ticket = new Ticket(7, owner, "Player", TicketStatus.OPEN, "bug", 3, 100L, 100L, null, List.of());

        ticket.close(staff, "Admin", "Проверено");

        assertEquals(TicketStatus.CLOSED, ticket.status());
        assertEquals(staff, ticket.closedByUuid());
        assertEquals("Проверено", ticket.closeReason());

        ticket.reopen();

        assertEquals(TicketStatus.OPEN, ticket.status());
        assertEquals(null, ticket.closedByUuid());
        assertEquals(null, ticket.closeReason());
    }

    @Test
    void messagesViewIsImmutableButTicketCanAppend() {
        UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Ticket ticket = new Ticket(8, owner, "Player", TicketStatus.OPEN, "question", 2, 100L, 100L, null, List.of());

        assertThrows(UnsupportedOperationException.class, () -> ticket.messages().add(new TicketMessage(owner, "Player", false, 100L, "test")));

        ticket.addMessage(new TicketMessage(owner, "Player", false, 101L, "Нужна помощь"));

        assertFalse(ticket.messages().isEmpty());
        assertTrue(ticket.isOwner(owner));
    }

    @Test
    void statusAllowsOnlyExpectedTicketOperations() {
        assertTrue(TicketStatus.OPEN.canReceiveComments());
        assertTrue(TicketStatus.OPEN.canBeClosed());
        assertTrue(TicketStatus.OPEN.canBeAssigned());
        assertTrue(TicketStatus.OPEN.canBeSoftDeleted());
        assertFalse(TicketStatus.OPEN.canBeReopened());
        assertFalse(TicketStatus.OPEN.canBePurged());

        assertTrue(TicketStatus.CLOSED.canBeReopened());
        assertTrue(TicketStatus.CLOSED.canBeSoftDeleted());
        assertFalse(TicketStatus.CLOSED.canReceiveComments());
        assertFalse(TicketStatus.CLOSED.canBeClosed());
        assertFalse(TicketStatus.CLOSED.canBeAssigned());
        assertFalse(TicketStatus.CLOSED.canBePurged());

        assertTrue(TicketStatus.DELETED.canBePurged());
        assertFalse(TicketStatus.DELETED.canReceiveComments());
        assertFalse(TicketStatus.DELETED.canBeClosed());
        assertFalse(TicketStatus.DELETED.canBeReopened());
        assertFalse(TicketStatus.DELETED.canBeAssigned());
        assertFalse(TicketStatus.DELETED.canBeSoftDeleted());
    }
}
