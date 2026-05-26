package ru.mevgeniy.malinaticket.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mevgeniy.malinaticket.TicketService;
import ru.mevgeniy.malinaticket.config.MessageService;
import ru.mevgeniy.malinaticket.gui.GuiService;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketStatus;
import ru.mevgeniy.malinaticket.session.ChatInputService;
import ru.mevgeniy.malinaticket.storage.TicketStorage;
import ru.mevgeniy.malinaticket.util.PermissionNodes;

public final class TicketCommand implements CommandExecutor, TabCompleter {
    private final TicketStorage storage;
    private final TicketService ticketService;
    private final GuiService guiService;
    private final MessageService messages;
    private final ChatInputService chatInputs;
    private final Runnable reloadAction;
    private final TicketTabCompletion tabCompletion;

    public TicketCommand(TicketStorage storage, TicketService ticketService, GuiService guiService, MessageService messages, ChatInputService chatInputs, Runnable reloadAction) {
        this.storage = storage;
        this.ticketService = ticketService;
        this.guiService = guiService;
        this.messages = messages;
        this.chatInputs = chatInputs;
        this.reloadAction = reloadAction;
        this.tabCompletion = new TicketTabCompletion(storage, ticketService);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                guiService.showPlayerHome(player);
            } else {
                sendHelp(sender, label);
            }
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "create", "new" -> openCreateTicketMenu(sender);
            case "list" -> openTicketListFromCommand(sender, args);
            case "view", "open" -> openTicketDetailsFromCommand(sender, args);
            case "comment", "reply" -> addTicketCommentFromCommand(sender, args);
            case "close" -> closeTicketFromCommand(sender, args);
            case "staff", "admin" -> openStaffDashboardFromCommand(sender);
            case "assign" -> assignTicketToOnlinePlayer(sender, args);
            case "reopen" -> reopenTicketFromCommand(sender, args);
            case "delete" -> softDeleteTicketFromCommand(sender, args);
            case "purge" -> purgeTicketFromCommand(sender, args);
            case "tp", "teleport" -> teleportToTicketLocation(sender, args);
            case "ban" -> banPlayerFromTicketCreation(sender, args);
            case "unban" -> unbanPlayerFromTicketCreation(sender, args);
            case "reload" -> reloadPluginConfigs(sender);
            case "stats" -> sendTicketStats(sender);
            case "cancel" -> cancelChatInput(sender);
            case "help", "?" -> sendHelp(sender, label);
            default -> {
                messages.send(sender, "errors.unknown-command");
                sendHelp(sender, label);
            }
        }
        return true;
    }

    private void openCreateTicketMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        guiService.showTicketCreation(player, null, 3);
    }

    private void openTicketListFromCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendTicketStats(sender);
            return;
        }
        TicketStatus status = args.length > 1 && args[1].equalsIgnoreCase("closed") ? TicketStatus.CLOSED : TicketStatus.OPEN;
        if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
            guiService.showStaffTickets(player, TicketStatus.OPEN, null, false, 0);
            return;
        }
        guiService.showPlayerTickets(player, status, 0);
    }

    private void openTicketDetailsFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/ticket view <id>");
        if (ticket == null) {
            return;
        }
        if (!ticketService.canView(sender, ticket)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (sender instanceof Player player) {
            guiService.showTicketDetails(player, ticket);
        } else {
            messages.send(sender, "ticket.info", ticketService.placeholders(ticket));
        }
    }

    private void addTicketCommentFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/ticket comment <id> <текст>");
        if (ticket == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "errors.usage", Map.of("usage", "/ticket comment <id> <текст>"));
            return;
        }
        ticketService.addComment(sender, ticket, join(args, 2));
    }

    private void closeTicketFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/ticket close <id> [причина]");
        if (ticket == null) {
            return;
        }
        ticketService.close(sender, ticket, args.length >= 3 ? join(args, 2) : "Закрыто без указанной причины");
    }

    private void openStaffDashboardFromCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        guiService.showStaffDashboard(player);
    }

    private void assignTicketToOnlinePlayer(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/ticket assign <id> <ник>");
        if (ticket == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "errors.usage", Map.of("usage", "/ticket assign <id> <ник>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messages.send(sender, "errors.player-not-found");
            return;
        }
        ticketService.assign(sender, ticket, target);
    }

    private void reopenTicketFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/ticket reopen <id>");
        if (ticket != null) {
            ticketService.reopen(sender, ticket);
        }
    }

    private void softDeleteTicketFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/ticket delete <id>");
        if (ticket != null) {
            ticketService.delete(sender, ticket);
        }
    }

    private void purgeTicketFromCommand(CommandSender sender, String[] args) {
        Ticket ticket = requireTicket(sender, args, 1, "/ticket purge <id>");
        if (ticket != null) {
            ticketService.purge(sender, ticket);
        }
    }

    private void teleportToTicketLocation(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        if (!player.hasPermission(PermissionNodes.TELEPORT)) {
            messages.send(player, "errors.no-permission");
            return;
        }
        Ticket ticket = requireTicket(sender, args, 1, "/ticket tp <id>");
        if (ticket == null) {
            return;
        }
        if (!ticketService.canView(player, ticket)) {
            messages.send(player, "errors.no-permission");
            return;
        }
        if (ticket.location() == null) {
            return;
        }
        Location location = ticket.location().toBukkitLocation();
        if (location == null) {
            messages.send(player, "errors.world-not-found");
            return;
        }
        player.teleport(location);
        messages.send(player, "ticket.teleported", ticketService.placeholders(ticket));
    }

    private void banPlayerFromTicketCreation(CommandSender sender, String[] args) {
        setPlayerCreationBan(sender, args, true);
    }

    private void unbanPlayerFromTicketCreation(CommandSender sender, String[] args) {
        setPlayerCreationBan(sender, args, false);
    }

    @SuppressWarnings("deprecation")
    private void setPlayerCreationBan(CommandSender sender, String[] args, boolean banned) {
        String permission = banned ? PermissionNodes.BAN : PermissionNodes.UNBAN;
        if (!sender.hasPermission(permission)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "errors.usage", Map.of("usage", banned ? "/ticket ban <ник>" : "/ticket unban <ник>"));
            return;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
        storage.setCreationBan(offlinePlayer.getUniqueId(), banned);
        messages.send(sender, banned ? "ticket.creation-ban-added" : "ticket.creation-ban-removed", Map.of("player", offlinePlayer.getName() == null ? args[1] : offlinePlayer.getName()));
    }

    private void reloadPluginConfigs(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.RELOAD)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        reloadAction.run();
        messages.send(sender, "plugin.reloaded");
    }

    private void sendTicketStats(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.STATS)) {
            messages.send(sender, "errors.no-permission");
            return;
        }
        messages.send(sender, "ticket.stats", Map.of(
                "open", String.valueOf(storage.countByStatus(TicketStatus.OPEN)),
                "closed", String.valueOf(storage.countByStatus(TicketStatus.CLOSED)),
                "deleted", String.valueOf(storage.countByStatus(TicketStatus.DELETED))
        ));
    }

    private void cancelChatInput(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "errors.only-player");
            return;
        }
        if (chatInputs.cancel(player)) {
            messages.send(player, "input.cancelled");
        } else {
            messages.send(player, "input.nothing-to-cancel");
        }
    }

    private Ticket requireTicket(CommandSender sender, String[] args, int index, String usage) {
        if (args.length <= index) {
            messages.send(sender, "errors.usage", Map.of("usage", usage));
            return null;
        }
        int id;
        try {
            id = Integer.parseInt(args[index]);
        } catch (NumberFormatException exception) {
            messages.send(sender, "errors.invalid-number");
            return null;
        }
        Ticket ticket = storage.byId(id);
        if (ticket == null) {
            messages.send(sender, "errors.ticket-not-found");
        }
        return ticket;
    }

    private void sendHelp(CommandSender sender, String label) {
        String prefix = "<#D94F70><bold>MalinaTicket</bold> <#6B5961>| <#FBE8EE>";
        messages.sendRaw(sender, prefix + "/" + label + " <#F4A6B8>- открыть меню", Map.of());
        messages.sendRaw(sender, prefix + "/" + label + " create <#F4A6B8>- создать тикет", Map.of());
        messages.sendRaw(sender, prefix + "/" + label + " view <id> <#F4A6B8>- открыть тикет", Map.of());
        messages.sendRaw(sender, prefix + "/" + label + " comment <id> <текст> <#F4A6B8>- ответить", Map.of());
        messages.sendRaw(sender, prefix + "/" + label + " close <id> [причина] <#F4A6B8>- закрыть", Map.of());
        messages.sendRaw(sender, prefix + "/" + label + " staff <#F4A6B8>- меню персонала", Map.of());
        messages.sendRaw(sender, prefix + "/" + label + " cancel <#F4A6B8>- отменить ввод через чат", Map.of());
    }

    private String join(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return tabCompletion.complete(sender, args);
    }
}
