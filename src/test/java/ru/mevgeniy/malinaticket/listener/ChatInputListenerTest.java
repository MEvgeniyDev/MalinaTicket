package ru.mevgeniy.malinaticket.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.junit.jupiter.api.Test;
import ru.mevgeniy.malinaticket.session.ChatInputService;

@SuppressWarnings("deprecation")
class ChatInputListenerTest {
    @Test
    void onlyCancelsTheMessageConsumedByTheTicketSession() {
        UUID uuid = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> method.getName().equals("getUniqueId") ? uuid : null);
        ChatInputService inputs = new ChatInputService();
        inputs.awaitComment(player, 7);
        AsyncPlayerChatEvent first = new AsyncPlayerChatEvent(true, player, "Ответ в тикет", new java.util.HashSet<>());
        AsyncPlayerChatEvent second = new AsyncPlayerChatEvent(true, player, "Сообщение в чат", new java.util.HashSet<>());
        List<String> captured = new ArrayList<>();

        ChatInputListener.captureChat(true, inputs, player, first, first::getRecipients, first::getMessage,
                (input, text) -> captured.add(input.ticketId() + ":" + text), exception -> fail(exception));
        ChatInputListener.captureChat(true, inputs, player, second, () -> fail("Неожиданное обращение за получателями"),
                second::getMessage, (input, text) -> fail("Обычное сообщение перехвачено"), exception -> fail(exception));

        assertTrue(first.isCancelled());
        assertFalse(second.isCancelled());
        assertEquals(List.of("7:Ответ в тикет"), captured);
    }
}
