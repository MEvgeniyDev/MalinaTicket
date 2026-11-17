package ru.mevgeniy.malinaticket.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mevgeniy.malinaticket.gui.GuiService;
import ru.mevgeniy.malinaticket.gui.MenuHolder;

public final class GuiListener implements Listener {
    private final JavaPlugin plugin;
    private final GuiService guiService;

    public GuiListener(JavaPlugin plugin, GuiService guiService) {
        this.plugin = plugin;
        this.guiService = guiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if ((event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT)
                || slot < 0 || slot >= inventory.getSize() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && inventory.getViewers().contains(player)) {
                guiService.playClick(player);
                holder.handle(event);
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}
