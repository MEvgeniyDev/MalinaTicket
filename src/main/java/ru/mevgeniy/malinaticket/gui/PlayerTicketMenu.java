package ru.mevgeniy.malinaticket.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import ru.mevgeniy.malinaticket.config.GuiItemConfig;
import ru.mevgeniy.malinaticket.model.TicketCategory;
import ru.mevgeniy.malinaticket.model.TicketStatus;
import ru.mevgeniy.malinaticket.util.PermissionNodes;
import org.bukkit.entity.Player;

final class PlayerTicketMenu {
    private final GuiService gui;

    PlayerTicketMenu(GuiService gui) {
        this.gui = gui;
    }

    void openHome(Player player) {
        if (!player.hasPermission(PermissionNodes.USE)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, "main", Map.of());
        gui.fillBackground(inventory);

        gui.placeClickableItem(inventory, holder, gui.guiConfig.item("main.create", 11, "WRITABLE_BOOK", "<#D94F70>Создать тикет", List.of("<#FBE8EE>Новое обращение в поддержку.")), Map.of(), event -> gui.showTicketCreation(player, null, 3));
        gui.placeClickableItem(inventory, holder, gui.guiConfig.item("main.open", 13, "BOOK", "<#F4A6B8>Мои тикеты", List.of("<#FBE8EE>Открытые обращения.")), Map.of(), event -> gui.showPlayerTickets(player, TicketStatus.OPEN, 0));
        gui.placeClickableItem(inventory, holder, gui.guiConfig.item("main.closed", 15, "CHEST", "<#F4A6B8>Закрытые", List.of("<#FBE8EE>История решенных обращений.")), Map.of(), event -> gui.showPlayerTickets(player, TicketStatus.CLOSED, 0));
        if (player.hasPermission(PermissionNodes.STAFF) || player.hasPermission(PermissionNodes.STAFF_GUI)) {
            gui.placeClickableItem(inventory, holder, gui.guiConfig.item("main.staff", 31, "AMETHYST_SHARD", "<#F2C14E>Персонал", List.of("<#FBE8EE>Меню обработки тикетов.")), Map.of(), event -> gui.showStaffDashboard(player));
        }
        player.openInventory(inventory);
    }

    void openCreateTicket(Player player, String selectedCategoryId, int selectedPriority) {
        if (!player.hasPermission(PermissionNodes.CREATE)) {
            gui.messages.send(player, "errors.no-permission");
            return;
        }
        int safePriority = Math.max(1, Math.min(5, selectedPriority));
        TicketCategory selected = selectedCategoryId == null ? gui.categories.first() : gui.categories.byId(selectedCategoryId);
        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, "create", Map.of(
                "category", selected == null ? "Не выбрана" : selected.displayName(),
                "priority", String.valueOf(safePriority)
        ));
        gui.fillBackground(inventory);

        for (TicketCategory category : gui.categories.all()) {
            boolean active = selected != null && selected.id().equalsIgnoreCase(category.id());
            List<String> lore = new ArrayList<>(category.description());
            lore.add(active ? "<#88C999>Выбрано" : "<#FBE8EE>Нажми, чтобы выбрать.");
            GuiItemConfig item = new GuiItemConfig(
                    category.slot(),
                    category.material(),
                    "<" + category.color() + ">" + category.displayName(),
                    lore,
                    category.amount(),
                    category.glow(),
                    category.customModelData()
            );
            gui.placeClickableItem(inventory, holder, item, Map.of(), event -> gui.showTicketCreation(player, category.id(), safePriority));
        }

        int[] prioritySlots = {29, 30, 31, 32, 33};
        for (int priority = 1; priority <= 5; priority++) {
            boolean active = priority == safePriority;
            GuiItemConfig item = new GuiItemConfig(
                    prioritySlots[priority - 1],
                    gui.text.priorityMaterial(priority),
                    gui.text.priorityColor(priority) + "Важность " + priority + ": " + gui.text.priorityName(priority),
                    List.of(active ? "<#88C999>Выбрано" : "<#FBE8EE>Нажми, чтобы выбрать.")
            );
            int selectedValue = priority;
            gui.placeClickableItem(inventory, holder, item, Map.of(), event -> gui.showTicketCreation(player, selected == null ? null : selected.id(), selectedValue));
        }

        boolean ready = selected != null && gui.categories.canCreate(player, selected);
        GuiItemConfig finalizeItem = gui.guiConfig.item(
                "create.finalize",
                49,
                ready ? "LIME_CONCRETE" : "RED_CONCRETE",
                ready ? "<#88C999>Написать обращение" : "<#E05A47>Недоступно",
                ready ? List.of("<#FBE8EE>Нажми и напиши текст в чат.", "<#6B5961>Для отмены напиши: отмена.") : List.of("<#FBE8EE>Нет прав на выбранную категорию.")
        );
        gui.placeClickableItem(inventory, holder, finalizeItem, Map.of(), event -> {
            if (!ready) {
                gui.messages.send(player, "errors.category-no-permission");
                return;
            }
            gui.chatInputs.awaitCreate(player, selected.id(), safePriority);
            player.closeInventory();
            gui.messages.send(player, "input.create");
        });
        gui.placeClickableItem(inventory, holder, gui.itemAt(gui.guiConfig.item("navigation.back", 45, "ARROW", "<#FBE8EE>Вернуться", List.of()), 45), Map.of(), event -> gui.showPlayerHome(player));
        player.openInventory(inventory);
    }
}
