package ru.mevgeniy.malinaticket.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import ru.mevgeniy.malinaticket.util.PermissionNodes;

class TicketCommandTest {
    @Test
    void tabCompleteShowsOnlyCommandsAllowedForPlainPlayer() {
        TicketTabCompletion completion = new TicketTabCompletion(null, null);
        CommandSender sender = player(PermissionNodes.CREATE, PermissionNodes.VIEW_OWN, PermissionNodes.COMMENT_OWN);

        var subcommands = completion.visibleSubcommands(sender);

        assertTrue(subcommands.contains("create"));
        assertTrue(subcommands.contains("view"));
        assertTrue(subcommands.contains("comment"));
        assertFalse(subcommands.contains("purge"));
        assertFalse(subcommands.contains("reload"));
        assertFalse(subcommands.contains("ban"));
    }

    @Test
    void tabCompleteShowsAdminCommandsOnlyWithPermission() {
        TicketTabCompletion completion = new TicketTabCompletion(null, null);
        CommandSender sender = player(PermissionNodes.PURGE, PermissionNodes.RELOAD, PermissionNodes.BAN);

        var subcommands = completion.visibleSubcommands(sender);

        assertTrue(subcommands.contains("purge"));
        assertTrue(subcommands.contains("reload"));
        assertTrue(subcommands.contains("ban"));
        assertFalse(subcommands.contains("staff"));
    }

    private CommandSender player(String... permissions) {
        Set<String> allowed = Set.of(permissions);
        return (CommandSender) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> allowed.contains(String.valueOf(args[0]));
                    case "getUniqueId" -> UUID.fromString("11111111-1111-1111-1111-111111111111");
                    case "getName" -> "TestPlayer";
                    case "isOp" -> false;
                    case "toString" -> "TestPlayer";
                    case "hashCode" -> 1;
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }
}
