package ru.mevgeniy.malinaticket.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import ru.mevgeniy.malinaticket.config.PluginSettings;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketMessage;
import ru.mevgeniy.malinaticket.util.TimeFormatter;

final class GuiTextFormatter {
    static final int LORE_WRAP_WIDTH = 38;

    private final String dateFormat;
    private final String timeZone;

    GuiTextFormatter(PluginSettings settings) {
        this(settings.dateFormat(), settings.timeZone());
    }

    GuiTextFormatter(String dateFormat, String timeZone) {
        this.dateFormat = dateFormat;
        this.timeZone = timeZone;
    }

    String priorityMaterial(int priority) {
        return switch (priority) {
            case 1 -> "LIGHT_GRAY_WOOL";
            case 2 -> "LIME_WOOL";
            case 3 -> "YELLOW_WOOL";
            case 4 -> "ORANGE_WOOL";
            case 5 -> "RED_WOOL";
            default -> "WHITE_WOOL";
        };
    }

    String priorityName(int priority) {
        return switch (priority) {
            case 1 -> "низкая";
            case 2 -> "обычная";
            case 3 -> "средняя";
            case 4 -> "высокая";
            case 5 -> "срочная";
            default -> "неизвестная";
        };
    }

    String priorityColor(int priority) {
        return switch (priority) {
            case 1 -> "<#A8A8A8>";
            case 2 -> "<#88C999>";
            case 3 -> "<#F2C14E>";
            case 4 -> "<#F39A4A>";
            case 5 -> "<#E05A47>";
            default -> "<#FBE8EE>";
        };
    }

    List<String> messageLore(Ticket ticket) {
        if (ticket.messages().isEmpty()) {
            return List.of("<#6B5961>Сообщений пока нет.");
        }

        List<String> lore = new ArrayList<>();
        List<TicketMessage> ticketMessages = ticket.messages();
        TimeFormatter formatter = new TimeFormatter(dateFormat, timeZone);
        for (int index = 0; index < ticketMessages.size(); index++) {
            TicketMessage message = ticketMessages.get(index);
            lore.add("<#F4A6B8>" + escape(message.authorName()) + " <#6B5961>(" + formatter.format(message.timestamp()) + ")");
            lore.addAll(wrapLore(escape(message.text()), "<#FBE8EE>", LORE_WRAP_WIDTH));
            if (index + 1 < ticketMessages.size()) {
                lore.add(" ");
            }
        }
        return lore;
    }

    String wrapPlaceholder(String text, String color, int width) {
        List<String> wrapped = wrapLore(escape(text), color, width);
        return String.join("\n", wrapped);
    }

    List<String> wrapLore(String text, String color, int width) {
        return wrapPlain(text, width).stream()
                .map(line -> color + line)
                .toList();
    }

    List<String> wrapPlain(String text, int width) {
        String clean = text == null ? "" : text.strip();
        if (clean.isEmpty()) {
            return List.of("");
        }

        List<String> result = new ArrayList<>();
        for (String paragraph : clean.split("\\R")) {
            wrapParagraph(paragraph, width, result);
        }
        return result;
    }

    private void wrapParagraph(String paragraph, int width, List<String> result) {
        StringBuilder line = new StringBuilder();
        for (String word : paragraph.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (line.isEmpty()) {
                appendWordOrSplit(word, width, result, line);
                continue;
            }
            if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
                continue;
            }
            result.add(line.toString());
            line.setLength(0);
            appendWordOrSplit(word, width, result, line);
        }
        if (!line.isEmpty()) {
            result.add(line.toString());
        }
    }

    private void appendWordOrSplit(String word, int width, List<String> result, StringBuilder line) {
        if (word.length() <= width) {
            line.append(word);
            return;
        }
        int start = 0;
        while (start < word.length()) {
            int end = Math.min(start + width, word.length());
            String part = word.substring(start, end);
            if (end < word.length()) {
                result.add(part);
            } else {
                line.append(part);
            }
            start = end;
        }
    }

    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return MiniMessage.miniMessage().escapeTags(input);
    }
}
