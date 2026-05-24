package ru.mevgeniy.malinaticket.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import ru.mevgeniy.malinaticket.config.GuiItemConfig;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketStatus;
import ru.mevgeniy.malinaticket.util.PermissionNodes;

final class TicketListMenu {
    private final GuiService gui;

    TicketListMenu(GuiService gui) {
        this.gui = gui;
    }

    void openOwnTickets(Player player, TicketStatus status, int page) {
        List<Ticket> tickets = gui.storage.ownTickets(player.getUniqueId(), true, false).stream()
                .filter(ticket -> ticket.status() == status)
                .toList();
        openTickets(player, tickets, "list", page, event -> gui.showPlayerHome(player));
    }

    void openStaffTickets(Player player, TicketStatus status, String categoryId, boolean assignedOnly, int page) {
        if (!player.hasPermission(PermissionNodes.VIEW_ALL) && !player.hasPermission(PermissionNodes.STAFF)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        if (status == TicketStatus.DELETED && !player.hasPermission(PermissionNodes.VIEW_DELETED)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        if (status == TicketStatus.CLOSED && !player.hasPermission(PermissionNodes.VIEW_CLOSED) && !player.hasPermission(PermissionNodes.STAFF)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        List<Ticket> tickets = gui.storage.staffTickets(status, categoryId, assignedOnly ? player.getUniqueId() : null);
        openTickets(player, tickets, "list", page, event -> gui.showStaffDashboard(player));
    }

    private void openTickets(Player player, List<Ticket> tickets, String menuName, int page, Consumer<InventoryClickEvent> backAction) {
        int perPage = Math.min(gui.settings.ticketsPerPage(), gui.guiConfig.ticketSlots().size());
        int maxPage = Math.max(0, (tickets.size() - 1) / perPage);
        int safePage = Math.max(0, Math.min(page, maxPage));
        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, menuName, Map.of("page", String.valueOf(safePage + 1), "pages", String.valueOf(maxPage + 1)));
        gui.fillBackground(inventory);

        List<Integer> slots = gui.guiConfig.ticketSlots();
        int start = safePage * perPage;
        int end = Math.min(tickets.size(), start + perPage);
        for (int index = start; index < end; index++) {
            Ticket ticket = tickets.get(index);
            int slot = slots.get(index - start);
            Map<String, String> placeholders = new HashMap<>(gui.ticketService.placeholders(ticket));
            placeholders.put("first_message", gui.text.wrapPlaceholder(ticket.firstMessage() == null ? "" : ticket.firstMessage().text(), "<#6B5961>", GuiTextFormatter.LORE_WRAP_WIDTH));
            GuiItemConfig template = gui.guiConfig.item("ticket.item", slot, "PAPER", "<#D94F70>Тикет #%id%", List.of(
                    "<#FBE8EE>Игрок: <#F4A6B8>%player%",
                    "<#FBE8EE>Категория: <#F4A6B8>%category%",
                    "<#FBE8EE>Важность: <#F2C14E>%priority%",
                    "<#FBE8EE>Статус: <#88C999>%status%",
                    "<#6B5961>%first_message%"
            ));
            gui.placeClickableItem(inventory, holder, gui.itemAt(template, slot), placeholders, event -> gui.showTicketDetails(player, ticket));
        }

        if (safePage > 0) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("navigation.previous", 45, "ARROW", "<#FBE8EE>Страница назад", List.of()), Map.of(), event -> openTickets(player, tickets, menuName, safePage - 1, backAction));
        }
        gui.placeClickableItem(inventory, holder, gui.guiConfig.item("navigation.back", 49, "ARROW", "<#FBE8EE>Вернуться", List.of()), Map.of(), backAction);
        if (safePage < maxPage) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("navigation.next", 53, "ARROW", "<#FBE8EE>Страница вперед", List.of()), Map.of(), event -> openTickets(player, tickets, menuName, safePage + 1, backAction));
        }
        player.openInventory(inventory);
    }
}
