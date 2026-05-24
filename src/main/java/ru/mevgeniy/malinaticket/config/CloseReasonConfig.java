package ru.mevgeniy.malinaticket.config;

public record CloseReasonConfig(
        String key,
        GuiItemConfig item,
        String reason,
        boolean custom
) {
}
