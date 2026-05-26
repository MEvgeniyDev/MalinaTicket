package ru.mevgeniy.malinaticket.listener;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.EventExecutor;
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
    private static final String PAPER_ASYNC_CHAT_EVENT = "io.papermc.paper.event.player.AsyncChatEvent";
    private static final String PAPER_PLAIN_SERIALIZER = "net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer";

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
        registerPaperAsyncChat();
    }

    @SuppressWarnings("deprecation")
    @org.bukkit.event.EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
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
    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void keepLegacyChatHidden(AsyncPlayerChatEvent event) {
        suppressIfNeeded(event);
    }

    private void registerPaperAsyncChat() {
        try {
            Class<?> rawClass = Class.forName(PAPER_ASYNC_CHAT_EVENT);
            if (!Event.class.isAssignableFrom(rawClass)) {
                return;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) rawClass;
            EventExecutor capture = (listener, event) -> onPaperAsyncChat(event);
            EventExecutor suppress = (listener, event) -> suppressPaperAsyncChat(event);
            plugin.getServer().getPluginManager().registerEvent(eventClass, this, EventPriority.LOWEST, capture, plugin, false);
            plugin.getServer().getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR, suppress, plugin, false);
        } catch (ClassNotFoundException ignored) {
            // Paper 1.16.5 uses AsyncPlayerChatEvent only.
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Не удалось подключить Paper AsyncChatEvent. Будет использован legacy chat event.", exception);
        }
    }

    private void onPaperAsyncChat(Event event) {
        Player player = playerFrom(event);
        if (player == null) {
            return;
        }
        if (!shouldCapture(player)) {
            suppressIfNeeded(event, player);
            return;
        }

        hide(event);
        ChatInput input = chatInputs.consume(player).orElse(null);
        if (input == null) {
            return;
        }
        markSuppressed(player);
        String text = messageFrom(event).trim();
        Bukkit.getScheduler().runTask(plugin, () -> finishChatInput(player, input, text));
    }

    private void suppressPaperAsyncChat(Event event) {
        Player player = playerFrom(event);
        if (player != null) {
            suppressIfNeeded(event, player);
        }
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

    @SuppressWarnings("deprecation")
    private void hide(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        event.getRecipients().clear();
    }

    private void hide(Event event) {
        if (event instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
        try {
            Object viewers = event.getClass().getMethod("viewers").invoke(event);
            if (viewers instanceof Collection<?> collection) {
                collection.clear();
            }
        } catch (ReflectiveOperationException ignored) {
            // Some server builds only need cancellation.
        }
    }

    @SuppressWarnings("deprecation")
    private void suppressIfNeeded(AsyncPlayerChatEvent event) {
        if (isSuppressed(event.getPlayer())) {
            hide(event);
        }
    }

    private void suppressIfNeeded(Event event, Player player) {
        if (isSuppressed(player)) {
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

    private Player playerFrom(Event event) {
        try {
            Object result = event.getClass().getMethod("getPlayer").invoke(event);
            return result instanceof Player player ? player : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private String messageFrom(Event event) {
        try {
            Object component = event.getClass().getMethod("message").invoke(event);
            return plainText(component);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Не удалось прочитать сообщение из Paper AsyncChatEvent.", exception);
            return "";
        }
    }

    private String plainText(Object component) {
        if (component == null) {
            return "";
        }
        try {
            Class<?> serializerClass = Class.forName(PAPER_PLAIN_SERIALIZER);
            Object serializer = serializerClass.getMethod("plainText").invoke(null);
            Method serialize = null;
            for (Method method : serializerClass.getMethods()) {
                if (method.getName().equals("serialize") && method.getParameterCount() == 1) {
                    serialize = method;
                    break;
                }
            }
            if (serialize == null) {
                return String.valueOf(component);
            }
            return String.valueOf(serialize.invoke(serializer, component));
        } catch (ReflectiveOperationException exception) {
            return String.valueOf(component);
        }
    }
}
