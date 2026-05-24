package ru.mevgeniy.malinaticket.session;

public record ChatInput(
        ChatInputType type,
        int ticketId,
        String categoryId,
        int priority,
        long startedAt
) {
}
