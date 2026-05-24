package ru.mevgeniy.malinaticket.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.mevgeniy.malinaticket.TicketService;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.storage.TicketStorage;
import ru.mevgeniy.malinaticket.util.PermissionNodes;

final class TicketTabCompletion {
    private static final List<String> SUBCOMMANDS = List.of(
            "create", "list", "view", "comment", "close", "staff", "assign", "reopen",
            "delete", "purge", "tp", "ban", "unban", "reload", "stats", "cancel", "help"
    );

    private final TicketStorage storage;
    private final TicketService ticketService;

    TicketTabCompletion(TicketStorage storage, TicketService ticketService) {
        this.storage = storage;
        this.ticketService = ticketService;
    }

    List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(visibleSubcommands(sender), args[0]);
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && completesTicketId(subcommand)) {
            return filter(visibleTicketIds(sender, subcommand), args[1]);
        }
        if (args.length == 3 && subcommand.equals("assign") && sender.hasPermission(PermissionNodes.ASSIGN)) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        if (args.length == 2 && subcommand.equals("ban") && sender.hasPermission(PermissionNodes.BAN)) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 2 && subcommand.equals("unban") && sender.hasPermission(PermissionNodes.UNBAN)) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 2 && subcommand.equals("list")) {
            return filter(visibleListFilters(sender), args[1]);
        }
        return List.of();
    }

    List<String> visibleSubcommands(CommandSender sender) {
        return SUBCOMMANDS.stream()
                .filter(subcommand -> canUseSubcommand(sender, subcommand))
                .toList();
    }

    List<String> visibleTicketIds(CommandSender sender, String rawSubcommand) {
        if (storage == null || ticketService == null) {
            return List.of();
        }
        String subcommand = normalizeTicketSubcommand(rawSubcommand);
        return storage.allTickets().stream()
                .filter(ticket -> canCompleteTicket(sender, subcommand, ticket))
                .map(ticket -> String.valueOf(ticket.id()))
                .toList();
    }

    private boolean canUseSubcommand(CommandSender sender, String subcommand) {
        boolean console = !(sender instanceof Player);
        return switch (subcommand) {
            case "create" -> sender instanceof Player player && player.hasPermission(PermissionNodes.CREATE);
            case "list", "view", "help" -> true;
            case "comment" -> console || hasAny(sender, PermissionNodes.COMMENT_OWN, PermissionNodes.COMMENT_STAFF, PermissionNodes.STAFF);
            case "close" -> console || hasAny(sender, PermissionNodes.CLOSE_OWN, PermissionNodes.CLOSE, PermissionNodes.STAFF);
            case "staff" -> sender instanceof Player && hasAny(sender, PermissionNodes.STAFF, PermissionNodes.STAFF_GUI);
            case "assign" -> sender.hasPermission(PermissionNodes.ASSIGN);
            case "reopen" -> sender.hasPermission(PermissionNodes.REOPEN);
            case "delete" -> sender.hasPermission(PermissionNodes.DELETE);
            case "purge" -> sender.hasPermission(PermissionNodes.PURGE);
            case "tp" -> sender instanceof Player && sender.hasPermission(PermissionNodes.TELEPORT);
            case "ban" -> sender.hasPermission(PermissionNodes.BAN);
            case "unban" -> sender.hasPermission(PermissionNodes.UNBAN);
            case "reload" -> sender.hasPermission(PermissionNodes.RELOAD);
            case "stats" -> sender.hasPermission(PermissionNodes.STATS);
            case "cancel" -> sender instanceof Player;
            default -> false;
        };
    }

    private List<String> visibleListFilters(CommandSender sender) {
        List<String> filters = new ArrayList<>(List.of("open", "closed"));
        if (hasAny(sender, PermissionNodes.VIEW_ALL, PermissionNodes.STAFF)) {
            filters.add("all");
        }
        return filters;
    }

    private boolean completesTicketId(String subcommand) {
        return List.of("view", "open", "comment", "reply", "close", "assign", "reopen", "delete", "purge", "tp", "teleport")
                .contains(subcommand);
    }

    private String normalizeTicketSubcommand(String subcommand) {
        return switch (subcommand.toLowerCase(Locale.ROOT)) {
            case "open" -> "view";
            case "reply" -> "comment";
            case "teleport" -> "tp";
            default -> subcommand.toLowerCase(Locale.ROOT);
        };
    }

    private boolean canCompleteTicket(CommandSender sender, String subcommand, Ticket ticket) {
        return switch (subcommand) {
            case "view" -> ticketService.canView(sender, ticket);
            case "comment" -> ticketService.canComment(sender, ticket);
            case "close" -> ticket.status().canBeClosed() && ticketService.canView(sender, ticket) && ticketService.canClose(sender, ticket);
            case "assign" -> sender.hasPermission(PermissionNodes.ASSIGN) && ticket.status().canBeAssigned() && ticketService.canView(sender, ticket);
            case "reopen" -> sender.hasPermission(PermissionNodes.REOPEN) && ticket.status().canBeReopened() && ticketService.canView(sender, ticket);
            case "delete" -> sender.hasPermission(PermissionNodes.DELETE) && ticket.status().canBeSoftDeleted() && ticketService.canView(sender, ticket);
            case "purge" -> sender.hasPermission(PermissionNodes.PURGE) && ticket.status().canBePurged() && ticketService.canView(sender, ticket);
            case "tp" -> sender instanceof Player && sender.hasPermission(PermissionNodes.TELEPORT) && ticket.location() != null && ticketService.canView(sender, ticket);
            default -> false;
        };
    }

    private boolean hasAny(CommandSender sender, String... permissions) {
        for (String permission : permissions) {
            if (sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private List<String> filter(List<String> values, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
