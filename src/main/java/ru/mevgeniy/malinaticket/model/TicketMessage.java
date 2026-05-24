package ru.mevgeniy.malinaticket.model;

import java.util.UUID;

public record TicketMessage(
        UUID authorUuid,
        String authorName,
        boolean staff,
        long timestamp,
        String text
) {
}
