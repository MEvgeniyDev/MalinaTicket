package ru.mevgeniy.malinaticket.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
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
        if (!player.hasPermission(PermissionNodes.VIEW_OWN)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        List<Ticket> tickets = gui.storage.ownTickets(player.getUniqueId(), true, false).stream()
                .filter(ticket -> ticket.status() == status)
                .toList();
        openTickets(player, tickets, page, event -> gui.showPlayerHome(player), next -> openOwnTickets(player, status, next));
    }

    void openStaffTickets(Player player, TicketStatus status, String categoryId, boolean assignedOnly, int page) {
        if (!gui.ticketService.canViewStaffTickets(player, status)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        List<Ticket> tickets = gui.storage.staffTickets(status, categoryId, assignedOnly ? player.getUniqueId() : null);
        openTickets(player, tickets, page, event -> gui.showStaffDashboard(player), next -> openStaffTickets(player, status, categoryId, assignedOnly, next));
    }

    private void openTickets(Player player, List<Ticket> tickets, int page, Consumer<InventoryClickEvent> backAction, IntConsumer openPage) {
        GuiItemConfig previous = gui.guiConfig.item("navigation.previous", 45, "ARROW", "<#FBE8EE>Страница назад", List.of());
        GuiItemConfig back = gui.guiConfig.item("navigation.back", 49, "ARROW", "<#FBE8EE>Вернуться", List.of());
        GuiItemConfig next = gui.guiConfig.item("navigation.next", 53, "ARROW", "<#FBE8EE>Страница вперед", List.of());
        int size = gui.guiConfig.size("list", 54);
        GuiItemConfig previousButton = navigationAt(previous, size - 9, size);
        GuiItemConfig backButton = navigationAt(back, size - 5, size);
        GuiItemConfig nextButton = navigationAt(next, size - 1, size);
        List<Integer> slots = gui.guiConfig.ticketSlots().stream().distinct()
                .filter(slot -> slot >= 0 && slot < size && slot != previousButton.slot() && slot != backButton.slot() && slot != nextButton.slot())
                .toList();
        if (slots.isEmpty()) {
            slots = IntStream.range(0, size).boxed()
                    .filter(slot -> slot != previousButton.slot() && slot != backButton.slot() && slot != nextButton.slot()).toList();
        }
        int perPage = Math.min(gui.settings.ticketsPerPage(), slots.size());
        int maxPage = Math.max(0, (tickets.size() - 1) / perPage);
        int safePage = Math.max(0, Math.min(page, maxPage));
        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, "list", Map.of("page", String.valueOf(safePage + 1), "pages", String.valueOf(maxPage + 1)));
        gui.fillBackground(inventory);

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
            gui.placeClickableItem(inventory, holder, previousButton, Map.of(), event -> openPage.accept(safePage - 1));
        }
        gui.placeClickableItem(inventory, holder, backButton, Map.of(), backAction);
        if (safePage < maxPage) {
            gui.placeClickableItem(inventory, holder, nextButton, Map.of(), event -> openPage.accept(safePage + 1));
        }
        player.openInventory(inventory);
    }

    private GuiItemConfig navigationAt(GuiItemConfig item, int fallback, int size) {
        return item.slot() >= 0 && item.slot() < size ? item : gui.itemAt(item, fallback);
    }
}
