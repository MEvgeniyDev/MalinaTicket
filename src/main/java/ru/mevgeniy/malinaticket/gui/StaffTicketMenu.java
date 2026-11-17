package ru.mevgeniy.malinaticket.gui;

import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.mevgeniy.malinaticket.config.GuiItemConfig;
import ru.mevgeniy.malinaticket.model.TicketCategory;
import ru.mevgeniy.malinaticket.model.TicketStatus;

final class StaffTicketMenu {
    private final GuiService gui;

    StaffTicketMenu(GuiService gui) {
        this.gui = gui;
    }

    void openDashboard(Player player) {
        if (!gui.ticketService.canOpenStaffMenu(player)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, "staff", Map.of());
        gui.fillBackground(inventory);
        if (gui.ticketService.canViewStaffTickets(player, TicketStatus.OPEN)) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("staff.open", 10, "LIME_DYE", "<#88C999>Открытые", List.of("<#FBE8EE>Все тикеты, ожидающие ответа.")), Map.of(), event -> gui.showStaffTickets(player, TicketStatus.OPEN, null, false, 0));
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("staff.mine", 14, "NAME_TAG", "<#F2C14E>Назначены мне", List.of("<#FBE8EE>Тикеты, где ты ответственный.")), Map.of(), event -> gui.showStaffTickets(player, TicketStatus.OPEN, null, true, 0));
            addCategoryFilters(player, inventory, holder);
        }
        if (gui.ticketService.canViewStaffTickets(player, TicketStatus.CLOSED)) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("staff.closed", 12, "GRAY_DYE", "<#F4A6B8>Закрытые", List.of("<#FBE8EE>Завершенные обращения.")), Map.of(), event -> gui.showStaffTickets(player, TicketStatus.CLOSED, null, false, 0));
        }
        if (gui.ticketService.canViewStaffTickets(player, TicketStatus.DELETED)) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("staff.deleted", 16, "BARRIER", "<#E05A47>Удаленные", List.of("<#FBE8EE>Мягко удаленные тикеты.")), Map.of(), event -> gui.showStaffTickets(player, TicketStatus.DELETED, null, false, 0));
        }
        gui.placeClickableItem(inventory, holder, gui.itemAt(gui.guiConfig.item("navigation.back", 40, "ARROW", "<#FBE8EE>Вернуться", List.of()), 40), Map.of(), event -> gui.showPlayerHome(player));
        player.openInventory(inventory);
    }

    private void addCategoryFilters(Player player, Inventory inventory, MenuHolder holder) {
        int slot = 28;
        for (TicketCategory category : gui.categories.all()) {
            int targetSlot = category.slot() > 26 ? category.slot() : slot++;
            Map<String, String> placeholders = Map.of("category", gui.messages.escapeTags(category.displayName()), "category_id", gui.messages.escapeTags(category.id()));
            GuiItemConfig item = new GuiItemConfig(
                    targetSlot,
                    category.material(),
                    "<" + category.color() + ">%category%",
                    List.of("<#FBE8EE>Открытые тикеты этой категории."),
                    category.amount(),
                    category.glow(),
                    category.customModelData()
            );
            gui.placeClickableItem(inventory, holder, item, placeholders, event -> gui.showStaffTickets(player, TicketStatus.OPEN, category.id(), false, 0));
        }
    }
}
