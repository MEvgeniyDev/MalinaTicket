package ru.mevgeniy.malinaticket.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.mevgeniy.malinaticket.config.MessageService;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketStatus;
import ru.mevgeniy.malinaticket.storage.TicketStorage;
import ru.mevgeniy.malinaticket.util.PermissionNodes;

public final class JoinListener implements Listener {
    private final TicketStorage storage;
    private final MessageService messages;

    public JoinListener(TicketStorage storage, MessageService messages) {
        this.storage = storage;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        storage.refreshOwnerName(player.getUniqueId(), player.getName());
        List<String> pending = storage.drainPendingNotifications(player.getUniqueId());
        if (!pending.isEmpty()) {
            messages.send(player, "notify.offline-header", Map.of("count", String.valueOf(pending.size())));
            for (String raw : pending) {
                player.sendMessage(messages.componentFromText(raw, Map.of()));
            }
        }
        sendStaffSummary(player);
    }

    private void sendStaffSummary(Player player) {
        if (!canSeeStaffSummary(player)) {
            return;
        }

        List<Ticket> openTickets = storage.allTickets().stream()
                .filter(ticket -> ticket.status() == TicketStatus.OPEN)
                .toList();
        long assigned = openTickets.stream()
                .filter(ticket -> player.getUniqueId().equals(ticket.assignedToUuid()))
                .count();
        long unassigned = openTickets.stream()
                .filter(ticket -> ticket.assignedToUuid() == null)
                .count();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("open", String.valueOf(openTickets.size()));
        placeholders.put("assigned", String.valueOf(assigned));
        placeholders.put("unassigned", String.valueOf(unassigned));
        placeholders.put("closed", String.valueOf(storage.countByStatus(TicketStatus.CLOSED)));
        placeholders.put("deleted", String.valueOf(storage.countByStatus(TicketStatus.DELETED)));

        messages.send(player, "notify.staff-join-header", placeholders);
        if (openTickets.isEmpty()) {
            messages.send(player, "notify.staff-join-empty", placeholders);
            messages.send(player, "notify.staff-join-extra", placeholders);
            return;
        }

        Ticket latest = openTickets.get(0);
        placeholders.put("latest_id", String.valueOf(latest.id()));
        placeholders.put("latest_player", latest.ownerName());
        messages.send(player, "notify.staff-join-stats", placeholders);
        messages.send(player, "notify.staff-join-extra", placeholders);
        messages.send(player, "notify.staff-join-latest", placeholders);
        messages.send(player, "notify.staff-join-command", placeholders);
    }

    private boolean canSeeStaffSummary(Player player) {
        return player.isOp()
                || player.hasPermission(PermissionNodes.NOTIFY_JOIN)
                || player.hasPermission(PermissionNodes.STAFF)
                || player.hasPermission(PermissionNodes.ADMIN);
    }
}
