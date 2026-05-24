package ru.mevgeniy.malinaticket.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mevgeniy.malinaticket.TicketService;
import ru.mevgeniy.malinaticket.config.MessageService;
import ru.mevgeniy.malinaticket.config.PluginSettings;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.session.ChatInput;
import ru.mevgeniy.malinaticket.session.ChatInputService;
import ru.mevgeniy.malinaticket.storage.TicketStorage;

public final class ChatInputListener implements Listener {
    private static final long SUPPRESS_CHAT_MILLIS = 1_500L;

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final MessageService messages;
    private final ChatInputService chatInputs;
    private final TicketStorage storage;
    private final TicketService ticketService;
    private final Map<UUID, Long> suppressedChat = new ConcurrentHashMap<>();

    public ChatInputListener(JavaPlugin plugin, PluginSettings settings, MessageService messages, ChatInputService chatInputs, TicketStorage storage, TicketService ticketService) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.chatInputs = chatInputs;
        this.storage = storage;
        this.ticketService = ticketService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!shouldCapture(player)) {
            suppressIfNeeded(event);
            return;
        }

        hide(event);
        ChatInput input = chatInputs.consume(player).orElse(null);
        if (input == null) {
            return;
        }
        markSuppressed(player);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> finishChatInput(player, input, text));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void keepAsyncChatHidden(AsyncChatEvent event) {
        suppressIfNeeded(event);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!shouldCapture(player)) {
            suppressIfNeeded(event);
            return;
        }

        hide(event);
        ChatInput input = chatInputs.consume(player).orElse(null);
        if (input == null) {
            return;
        }
        markSuppressed(player);
        String text = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> finishChatInput(player, input, text));
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void keepLegacyChatHidden(AsyncPlayerChatEvent event) {
        suppressIfNeeded(event);
    }

    private void finishChatInput(Player player, ChatInput input, String text) {
        if (text.equalsIgnoreCase("отмена") || text.equalsIgnoreCase("cancel")) {
            messages.send(player, "input.cancelled");
            return;
        }
        switch (input.type()) {
            case CREATE -> ticketService.createFromInput(player, input.categoryId(), input.priority(), text);
            case COMMENT -> {
                Ticket ticket = storage.byId(input.ticketId());
                ticketService.addComment(player, ticket, text);
            }
            case CLOSE_REASON -> {
                Ticket ticket = storage.byId(input.ticketId());
                ticketService.close(player, ticket, text);
            }
        }
    }

    private boolean shouldCapture(Player player) {
        return settings.captureChatInputs() && chatInputs.hasInput(player);
    }

    private void hide(AsyncChatEvent event) {
        event.setCancelled(true);
        event.viewers().clear();
    }

    @SuppressWarnings("deprecation")
    private void hide(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        event.getRecipients().clear();
    }

    private void suppressIfNeeded(AsyncChatEvent event) {
        if (isSuppressed(event.getPlayer())) {
            hide(event);
        }
    }

    @SuppressWarnings("deprecation")
    private void suppressIfNeeded(AsyncPlayerChatEvent event) {
        if (isSuppressed(event.getPlayer())) {
            hide(event);
        }
    }

    private void markSuppressed(Player player) {
        suppressedChat.put(player.getUniqueId(), System.currentTimeMillis() + SUPPRESS_CHAT_MILLIS);
    }

    private boolean isSuppressed(Player player) {
        Long until = suppressedChat.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            suppressedChat.remove(player.getUniqueId(), until);
            return false;
        }
        return true;
    }
}
