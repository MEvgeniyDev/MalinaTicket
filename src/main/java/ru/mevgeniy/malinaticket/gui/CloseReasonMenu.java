package ru.mevgeniy.malinaticket.gui;

import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.mevgeniy.malinaticket.config.CloseReasonConfig;
import ru.mevgeniy.malinaticket.config.GuiItemConfig;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketStatus;

final class CloseReasonMenu {
    private final GuiService gui;

    CloseReasonMenu(GuiService gui) {
        this.gui = gui;
    }

    void openReasonPicker(Player player, Ticket ticket) {
        if (!gui.ticketService.canClose(player, ticket)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            gui.messages.send(player, "errors.ticket-deleted");
            return;
        }
        if (!ticket.status().canBeClosed()) {
            gui.messages.send(player, "errors.ticket-not-open");
            return;
        }

        MenuHolder holder = new MenuHolder();
        Map<String, String> placeholders = gui.ticketService.placeholders(ticket);
        Inventory inventory = gui.createInventory(holder, "close_reasons", placeholders);
        gui.fillBackground(inventory);

        for (CloseReasonConfig option : gui.guiConfig.closeReasons(defaultReasons())) {
            gui.placeClickableItem(inventory, holder, option.item(), placeholders, event -> {
                if (option.custom()) {
                    gui.chatInputs.awaitCloseReason(player, ticket.id());
                    player.closeInventory();
                    gui.messages.send(player, "input.close", placeholders);
                    return;
                }
                gui.ticketService.close(player, ticket, option.reason());
                gui.showTicketDetails(player, ticket);
            });
        }

        gui.placeClickableItem(inventory, holder, gui.itemAt(gui.guiConfig.item("navigation.back", 26, "ARROW", "<#FBE8EE>Вернуться", List.of()), 26), Map.of(), event -> gui.showTicketDetails(player, ticket));
        player.openInventory(inventory);
    }

    private List<CloseReasonConfig> defaultReasons() {
        return List.of(
                reason("resolved", 10, "LIME_WOOL", "<#88C999>Проблема решена", "Проблема решена", List.of("<#FBE8EE>Вопрос закрыт, решение найдено."), false),
                reason("no_info", 11, "YELLOW_WOOL", "<#F2C14E>Недостаточно информации", "Недостаточно информации", List.of("<#FBE8EE>Не хватает деталей для проверки."), false),
                reason("duplicate", 12, "PAPER", "<#F4A6B8>Дубликат обращения", "Дубликат обращения", List.of("<#FBE8EE>Такая заявка уже есть."), false),
                reason("not_confirmed", 13, "LIGHT_GRAY_WOOL", "<#A8A8A8>Нарушение не подтверждено", "Нарушение не подтверждено", List.of("<#FBE8EE>Доказательств недостаточно."), false),
                reason("not_server", 14, "COMPASS", "<#F39A4A>Не относится к серверу", "Не относится к серверу", List.of("<#FBE8EE>Проблема вне зоны поддержки."), false),
                reason("cancelled", 15, "GRAY_DYE", "<#6B5961>Отменено игроком", "Отменено игроком", List.of("<#FBE8EE>Автор отказался от обращения."), false),
                reason("custom", 16, "FEATHER", "<#D94F70>Своя причина", "", List.of("<#FBE8EE>Написать причину вручную в чат."), true)
        );
    }

    private CloseReasonConfig reason(String key, int slot, String material, String name, String reason, List<String> lore, boolean custom) {
        return new CloseReasonConfig(key, new GuiItemConfig(slot, material, name, lore), reason, custom);
    }
}
