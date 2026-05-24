package ru.mevgeniy.malinaticket.gui;

import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.mevgeniy.malinaticket.config.GuiItemConfig;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.util.PermissionNodes;

final class TicketViewMenu {
    private final GuiService gui;

    TicketViewMenu(GuiService gui) {
        this.gui = gui;
    }

    void openTicketDetails(Player player, Ticket ticket) {
        if (!gui.ticketService.canView(player, ticket)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        MenuHolder holder = new MenuHolder();
        Map<String, String> placeholders = gui.ticketService.placeholders(ticket);
        Inventory inventory = gui.createInventory(holder, "view", placeholders);
        gui.fillBackground(inventory);

        gui.placeClickableItem(inventory, holder, gui.guiConfig.item("view.info", 11, "BOOK", "<#D94F70>Информация", List.of(
                "<#FBE8EE>Тикет: <#F4A6B8>#%id%",
                "<#FBE8EE>Игрок: <#F4A6B8>%player%",
                "<#FBE8EE>Категория: <#F4A6B8>%category%",
                "<#FBE8EE>Важность: <#F2C14E>%priority%",
                "<#FBE8EE>Создан: <#F4A6B8>%created%",
                "<#FBE8EE>Место: <#F4A6B8>%location%"
        )), placeholders, event -> { });

        gui.placeClickableItem(inventory, holder, new GuiItemConfig(13, "WRITTEN_BOOK", "<#F4A6B8>Сообщения", gui.text.messageLore(ticket)), Map.of(), event -> { });

        if (gui.ticketService.canComment(player, ticket)) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("view.comment", 28, "FEATHER", "<#88C999>Ответить", List.of("<#FBE8EE>Добавить сообщение в тикет.")), placeholders, event -> {
                gui.chatInputs.awaitComment(player, ticket.id());
                player.closeInventory();
                gui.messages.send(player, "input.comment", placeholders);
            });
        }

        if (ticket.status().canBeClosed() && gui.ticketService.canClose(player, ticket)) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("view.close", 30, "REDSTONE", "<#E05A47>Закрыть", List.of("<#FBE8EE>Выбрать причину закрытия.")), placeholders, event -> gui.showCloseReasonPicker(player, ticket));
        }
        if (ticket.status().canBeReopened() && player.hasPermission(PermissionNodes.REOPEN)) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("view.reopen", 30, "LIME_DYE", "<#88C999>Переоткрыть", List.of("<#FBE8EE>Вернуть тикет в работу.")), placeholders, event -> {
                gui.ticketService.reopen(player, ticket);
                gui.showTicketDetails(player, ticket);
            });
        }
        if (player.hasPermission(PermissionNodes.TELEPORT) && ticket.location() != null) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("view.teleport", 32, "ENDER_PEARL", "<#F2C14E>К месту", List.of("<#FBE8EE>%location%")), placeholders, event -> {
                Location location = ticket.location().toBukkitLocation();
                if (location == null) {
                    gui.messages.send(player, "errors.world-not-found");
                    return;
                }
                player.teleport(location);
                gui.messages.send(player, "ticket.teleported", placeholders);
            });
        }
        if (player.hasPermission(PermissionNodes.DELETE) && ticket.status().canBeSoftDeleted()) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("view.delete", 34, "BARRIER", "<#E05A47>Удалить", List.of("<#FBE8EE>Скрыть тикет из обычных списков.")), placeholders, event -> {
                gui.ticketService.delete(player, ticket);
                openReturnMenu(player);
            });
        }
        if (player.hasPermission(PermissionNodes.PURGE) && ticket.status().canBePurged()) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("view.purge", 34, "LAVA_BUCKET", "<#E05A47>Удалить навсегда", List.of("<#FBE8EE>Окончательно стереть запись.")), placeholders, event -> {
                gui.ticketService.purge(player, ticket);
                openReturnMenu(player);
            });
        }
        gui.placeClickableItem(inventory, holder, gui.guiConfig.item("navigation.back", 49, "ARROW", "<#FBE8EE>Вернуться", List.of()), Map.of(), event -> openReturnMenu(player));
        player.openInventory(inventory);
    }

    private void openReturnMenu(Player player) {
        if (player.hasPermission(PermissionNodes.STAFF) || player.hasPermission(PermissionNodes.STAFF_GUI)) {
            gui.showStaffDashboard(player);
        } else {
            gui.showPlayerHome(player);
        }
    }
}
