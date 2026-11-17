package ru.mevgeniy.malinaticket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.mevgeniy.malinaticket.config.CategoryRegistry;
import ru.mevgeniy.malinaticket.config.MessageService;
import ru.mevgeniy.malinaticket.config.PluginSettings;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketCategory;
import ru.mevgeniy.malinaticket.model.TicketLocation;
import ru.mevgeniy.malinaticket.model.TicketMessage;
import ru.mevgeniy.malinaticket.model.TicketStatus;
import ru.mevgeniy.malinaticket.storage.TicketStorage;
import ru.mevgeniy.malinaticket.util.PermissionNodes;
import ru.mevgeniy.malinaticket.util.TimeFormatter;

public final class TicketService {
    private final TicketStorage storage;
    private final PluginSettings settings;
    private final MessageService messages;
    private final CategoryRegistry categories;
    private final Map<UUID, Long> lastCreation = new HashMap<>();
    private TimeFormatter timeFormatter;

    public TicketService(TicketStorage storage, PluginSettings settings, MessageService messages, CategoryRegistry categories) {
        this.storage = storage;
        this.settings = settings;
        this.messages = messages;
        this.categories = categories;
        this.timeFormatter = new TimeFormatter(settings.dateFormat(), settings.timeZone());
    }

    public void reloadFormatter() {
        this.timeFormatter = new TimeFormatter(settings.dateFormat(), settings.timeZone());
    }

    public Ticket createFromInput(Player player, String categoryId, int priority, String text) {
        TicketCategory category = categories.byId(categoryId);
        if (!player.hasPermission(PermissionNodes.CREATE)) {
            messages.send(player, "errors.no-permission");
            return null;
        }
        if (storage.isCreationBanned(player.getUniqueId())) {
            messages.send(player, "errors.creation-banned");
            return null;
        }
        if (!categories.canCreate(player, category)) {
            messages.send(player, "errors.category-no-permission");
            return null;
        }
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.isBlank()) {
            messages.send(player, "errors.empty-message");
            return null;
        }
        if (cleanText.length() > settings.maxMessageLength()) {
            messages.send(player, "errors.message-too-long", Map.of("max", String.valueOf(settings.maxMessageLength())));
            return null;
        }
        if (!player.hasPermission(PermissionNodes.BYPASS_LIMIT) && storage.countOpen(player.getUniqueId()) >= settings.maxOpenTickets()) {
            messages.send(player, "errors.open-limit", Map.of("limit", String.valueOf(settings.maxOpenTickets())));
            return null;
        }
        long now = System.currentTimeMillis();
        long last = lastCreation.getOrDefault(player.getUniqueId(), 0L);
        long wait = settings.cooldownSeconds() - ((now - last) / 1000L);
        if (!player.hasPermission(PermissionNodes.BYPASS_COOLDOWN) && wait > 0L) {
            messages.send(player, "errors.cooldown", Map.of("seconds", String.valueOf(wait)));
            return null;
        }

        Ticket ticket = storage.createTicket(player.getUniqueId(), player.getName(), player.getLocation(), category.id(), priority, cleanText);
        if (ticket == null) {
            messages.send(player, "errors.storage-write");
            return null;
        }
        lastCreation.put(player.getUniqueId(), now);
        messages.send(player, "ticket.created", placeholders(ticket));
        notifyStaff("notify.new-ticket", placeholders(ticket), PermissionNodes.NOTIFY_CREATE);
        return ticket;
    }

    public boolean canView(CommandSender sender, Ticket ticket) {
        if (ticket == null) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (ticket.status() != TicketStatus.DELETED && ticket.isOwner(player.getUniqueId()) && player.hasPermission(PermissionNodes.VIEW_OWN)) {
            return true;
        }
        return canViewStaffTickets(player, ticket.status());
    }

    public boolean canViewStaffTickets(CommandSender sender, TicketStatus status) {
        return sender.hasPermission(switch (status) {
            case OPEN -> PermissionNodes.VIEW_ALL;
            case CLOSED -> PermissionNodes.VIEW_CLOSED;
            case DELETED -> PermissionNodes.VIEW_DELETED;
        });
    }

    public boolean canOpenStaffMenu(CommandSender sender) {
        return sender.hasPermission(PermissionNodes.STAFF_GUI) || sender.hasPermission(PermissionNodes.STAFF);
    }

    public boolean canComment(CommandSender sender, Ticket ticket) {
        return ticket != null && ticket.status().canReceiveComments() && canView(sender, ticket)
                && (!(sender instanceof Player player) || player.hasPermission(ticket.isOwner(player.getUniqueId())
                        ? PermissionNodes.COMMENT_OWN : PermissionNodes.COMMENT_STAFF));
    }

    public void addComment(CommandSender sender, Ticket ticket, String text) {
        if (!checkAccess(sender, ticket) || !checkOpen(sender, ticket)) {
            return;
        }
        if (!canComment(sender, ticket)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.isBlank()) {
            messages.send(sender, "errors.empty-message");
            return;
        }
        if (cleanText.length() > settings.maxMessageLength()) {
            messages.send(sender, "errors.message-too-long", Map.of("max", String.valueOf(settings.maxMessageLength())));
            return;
        }
        boolean staff = !(sender instanceof Player player) || !ticket.isOwner(player.getUniqueId());
        UUID uuid = sender instanceof Player player ? player.getUniqueId() : null;
        String name = sender instanceof Player player ? player.getName() : "Консоль";
        Ticket updated = saveChange(sender, ticket,
                changed -> changed.addMessage(new TicketMessage(uuid, name, staff, System.currentTimeMillis(), cleanText)));
        if (updated == null) {
            return;
        }
        messages.send(sender, "ticket.comment-added", placeholders(updated));
        if (staff) {
            notifyOwner(updated, "notify.owner-comment", "offline.owner-comment");
        } else {
            notifyStaff("notify.player-comment", placeholders(updated), PermissionNodes.NOTIFY_COMMENT);
        }
    }

    public void close(CommandSender sender, Ticket ticket, String reason) {
        if (!checkAccess(sender, ticket)) {
            return;
        }
        if (!canClose(sender, ticket)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (!checkOpen(sender, ticket)) {
            return;
        }
        UUID uuid = sender instanceof Player player ? player.getUniqueId() : null;
        String name = sender instanceof Player player ? player.getName() : "Консоль";
        String cleanReason = reason == null || reason.isBlank() ? "Причина не указана" : reason.trim();
        if (cleanReason.length() > settings.maxMessageLength()) {
            messages.send(sender, "errors.message-too-long", Map.of("max", String.valueOf(settings.maxMessageLength())));
            return;
        }
        Ticket updated = saveChange(sender, ticket, changed -> changed.close(uuid, name, cleanReason));
        if (updated != null) {
            messages.send(sender, "ticket.closed", placeholders(updated));
            notifyOwner(updated, "notify.owner-closed", "offline.owner-closed");
            notifyStaff("notify.staff-closed", placeholders(updated), PermissionNodes.NOTIFY_CLOSE);
        }
    }

    public void reopen(CommandSender sender, Ticket ticket) {
        if (!checkAccess(sender, ticket)) {
            return;
        }
        if (!sender.hasPermission(PermissionNodes.REOPEN)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        if (!ticket.status().canBeReopened()) {
            messages.send(sender, "errors.ticket-not-closed");
            return;
        }
        Ticket updated = saveChange(sender, ticket, Ticket::reopen);
        if (updated != null) {
            messages.send(sender, "ticket.reopened", placeholders(updated));
            notifyOwner(updated, "notify.owner-reopened", "offline.owner-reopened");
        }
    }

    public void delete(CommandSender sender, Ticket ticket) {
        if (!checkAccess(sender, ticket)) {
            return;
        }
        if (!sender.hasPermission(PermissionNodes.DELETE)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (!ticket.status().canBeSoftDeleted()) {
            messages.send(sender, "errors.ticket-deleted");
            return;
        }
        UUID uuid = sender instanceof Player player ? player.getUniqueId() : null;
        String name = sender instanceof Player player ? player.getName() : "Консоль";
        Ticket updated = saveChange(sender, ticket, changed -> changed.markDeleted(uuid, name));
        if (updated != null) {
            messages.send(sender, "ticket.deleted", placeholders(updated));
        }
    }

    public void purge(CommandSender sender, Ticket ticket) {
        if (!checkAccess(sender, ticket)) {
            return;
        }
        if (!sender.hasPermission(PermissionNodes.PURGE)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (!ticket.status().canBePurged()) {
            messages.send(sender, "errors.ticket-not-deleted");
            return;
        }
        int id = ticket.id();
        if (!storage.purge(ticket)) {
            messages.send(sender, "errors.storage-write");
            return;
        }
        messages.send(sender, "ticket.purged", Map.of("id", String.valueOf(id)));
    }

    public void assign(CommandSender sender, Ticket ticket, Player target) {
        if (!checkAccess(sender, ticket)) {
            return;
        }
        if (!sender.hasPermission(PermissionNodes.ASSIGN)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (!checkOpen(sender, ticket)) {
            return;
        }
        Ticket updated = saveChange(sender, ticket, changed -> changed.assign(target.getUniqueId(), target.getName()));
        if (updated != null) {
            messages.send(sender, "ticket.assigned", placeholders(updated));
        }
    }

    public boolean canClose(CommandSender sender, Ticket ticket) {
        if (!canView(sender, ticket)) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (ticket.isOwner(player.getUniqueId())) {
            return player.hasPermission(PermissionNodes.CLOSE_OWN);
        }
        return player.hasPermission(PermissionNodes.CLOSE);
    }

    public void teleport(Player player, Ticket ticket) {
        if (!player.hasPermission(PermissionNodes.TELEPORT)) {
            messages.send(player, "errors.no-permission");
            return;
        }
        if (!checkAccess(player, ticket)) {
            return;
        }
        Location location = ticket.location() == null ? null : ticket.location().toBukkitLocation();
        if (location == null) {
            messages.send(player, "errors.world-not-found");
        } else if (player.teleport(location)) {
            messages.send(player, "ticket.teleported", placeholders(ticket));
        } else {
            messages.send(player, "errors.teleport-failed");
        }
    }

    private boolean checkAccess(CommandSender sender, Ticket ticket) {
        if (ticket == null || storage.byId(ticket.id()) != ticket) {
            messages.send(sender, "errors.ticket-not-found");
            return false;
        }
        if (!canView(sender, ticket)) {
            messages.send(sender, "errors.no-permission");
            return false;
        }
        return true;
    }

    private boolean checkOpen(CommandSender sender, Ticket ticket) {
        if (ticket.status() == TicketStatus.OPEN) {
            return true;
        }
        messages.send(sender, ticket.status() == TicketStatus.DELETED ? "errors.ticket-deleted" : "errors.ticket-not-open");
        return false;
    }

    private Ticket saveChange(CommandSender sender, Ticket ticket, Consumer<Ticket> change) {
        Ticket updated = storage.updateTicket(ticket, change);
        if (updated == null) {
            messages.send(sender, "errors.storage-write");
        }
        return updated;
    }

    public Map<String, String> placeholders(Ticket ticket) {
        TicketCategory category = categories.byId(ticket.categoryId());
        TicketMessage first = ticket.firstMessage();
        TicketMessage last = ticket.lastMessage();
        TicketLocation location = ticket.location();
        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(ticket.id()));
        map.put("player", escape(ticket.ownerName()));
        map.put("status", ticket.status().displayName());
        map.put("status_raw", ticket.status().name());
        map.put("category", escape(category == null ? ticket.categoryId() : category.displayName()));
        map.put("category_id", escape(ticket.categoryId()));
        map.put("priority", String.valueOf(ticket.priority()));
        map.put("created", timeFormatter.format(ticket.createdAt()));
        map.put("updated", timeFormatter.format(ticket.updatedAt()));
        map.put("closed", timeFormatter.format(ticket.closedAt()));
        map.put("closed_by", escape(ticket.closedByName()));
        map.put("close_reason", escape(ticket.closeReason()));
        map.put("assigned", ticket.assignedToName() == null ? "Не назначен" : escape(ticket.assignedToName()));
        map.put("location", location == null ? "Неизвестно" : escape(location.compact()));
        map.put("first_message", first == null ? "" : escape(first.text()));
        map.put("last_message", last == null ? "" : escape(last.text()));
        map.put("messages_count", String.valueOf(ticket.messages().size()));
        return map;
    }

    private void notifyStaff(String messagePath, Map<String, String> placeholders, String permission) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                messages.send(online, messagePath, placeholders);
            }
        }
    }

    private void notifyOwner(Ticket ticket, String onlinePath, String offlinePath) {
        Player owner = Bukkit.getPlayer(ticket.ownerUuid());
        if (owner != null && owner.isOnline()) {
            messages.send(owner, onlinePath, placeholders(ticket));
            return;
        }
        storage.addPendingNotification(ticket.ownerUuid(), messages.text(offlinePath, placeholders(ticket)));
    }

    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return messages.escapeTags(input);
    }
}
