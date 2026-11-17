package ru.mevgeniy.malinaticket.listener;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mevgeniy.malinaticket.TicketService;
import ru.mevgeniy.malinaticket.config.MessageService;
import ru.mevgeniy.malinaticket.config.PluginSettings;
import ru.mevgeniy.malinaticket.session.ChatInput;
import ru.mevgeniy.malinaticket.session.ChatInputService;
import ru.mevgeniy.malinaticket.storage.TicketStorage;

public final class ChatInputListener implements Listener {
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final MessageService messages;
    private final ChatInputService chatInputs;
    private final TicketStorage storage;
    private final TicketService ticketService;

    public ChatInputListener(JavaPlugin plugin, PluginSettings settings, MessageService messages, ChatInputService chatInputs, TicketStorage storage, TicketService ticketService) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.chatInputs = chatInputs;
        this.storage = storage;
        this.ticketService = ticketService;
    }

    @SuppressWarnings("deprecation")
    public void register() {
        PluginManager manager = plugin.getServer().getPluginManager();
        manager.registerEvents(this, plugin);
        try {
            Class<? extends PlayerEvent> eventType = Class.forName("io.papermc.paper.event.player.AsyncChatEvent").asSubclass(PlayerEvent.class);
            Method message = eventType.getMethod("originalMessage");
            Method viewers = eventType.getMethod("viewers");
            Class<?> serializerType = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
            Object serializer = serializerType.getMethod("plainText").invoke(null);
            Method serialize = serializerType.getMethod("serialize", message.getReturnType());
            manager.registerEvent(eventType, this, EventPriority.LOWEST, (listener, event) ->
                    capturePaperChat(eventType.cast(event), message, viewers, serializer, serialize), plugin);
            return;
        } catch (ClassNotFoundException ignored) {
            // На старых ядрах используется Bukkit-событие.
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Не удалось подключить современное событие чата. Используется Bukkit-событие.", exception);
        }
        manager.registerEvent(AsyncPlayerChatEvent.class, this, EventPriority.LOWEST, (listener, event) -> {
            AsyncPlayerChatEvent chat = (AsyncPlayerChatEvent) event;
            Player player = chat.getPlayer();
            captureChat(settings.captureChatInputs(), chatInputs, player, chat, chat::getRecipients, chat::getMessage,
                    (input, text) -> finishOnMainThread(player, input, text), exception -> reportReadFailure(player, exception));
        }, plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatInputs.cancel(event.getPlayer());
    }

    private void capturePaperChat(PlayerEvent event, Method message, Method viewers, Object serializer, Method serialize) {
        Player player = event.getPlayer();
        captureChat(settings.captureChatInputs(), chatInputs, player, (Cancellable) event,
                () -> (Collection<?>) viewers.invoke(event), () -> (String) serialize.invoke(serializer, message.invoke(event)),
                (input, text) -> finishOnMainThread(player, input, text), exception -> reportReadFailure(player, exception));
    }

    static void captureChat(boolean enabled, ChatInputService inputs, Player player, Cancellable event,
                            ChatValue<Collection<?>> recipients, ChatValue<String> text,
                            BiConsumer<ChatInput, String> finish, Consumer<ReflectiveOperationException> onFailure) {
        ChatInput input = enabled ? inputs.consume(player).orElse(null) : null;
        if (input == null) {
            return;
        }
        event.setCancelled(true);
        try {
            clearRecipients(recipients.read());
            finish.accept(input, text.read());
        } catch (ReflectiveOperationException exception) {
            onFailure.accept(exception);
        }
    }

    private static void clearRecipients(Collection<?> recipients) {
        try {
            recipients.clear();
        } catch (UnsupportedOperationException ignored) {
            // Некоторые ядра возвращают неизменяемый набор получателей.
        }
    }

    private void reportReadFailure(Player player, ReflectiveOperationException exception) {
        plugin.getLogger().log(Level.WARNING, "Не удалось прочитать текст для тикета из события чата.", exception);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                messages.send(player, "input.read-failed");
            }
        });
    }

    private void finishOnMainThread(Player player, ChatInput input, String message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !settings.captureChatInputs()) {
                return;
            }
            String text = message.trim();
            if (text.equalsIgnoreCase("отмена") || text.equalsIgnoreCase("cancel")) {
                messages.send(player, "input.cancelled");
                return;
            }
            switch (input.type()) {
                case CREATE -> ticketService.createFromInput(player, input.categoryId(), input.priority(), text);
                case COMMENT -> ticketService.addComment(player, storage.byId(input.ticketId()), text);
                case CLOSE_REASON -> ticketService.close(player, storage.byId(input.ticketId()), text);
            }
        });
    }

    @FunctionalInterface
    interface ChatValue<T> {
        T read() throws ReflectiveOperationException;
    }
}
