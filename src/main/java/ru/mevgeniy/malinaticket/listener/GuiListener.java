package ru.mevgeniy.malinaticket.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import ru.mevgeniy.malinaticket.gui.GuiService;
import ru.mevgeniy.malinaticket.gui.MenuHolder;

public final class GuiListener implements Listener {
    private final GuiService guiService;

    public GuiListener(GuiService guiService) {
        this.guiService = guiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            guiService.playClick(player);
        }
        holder.handle(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}
