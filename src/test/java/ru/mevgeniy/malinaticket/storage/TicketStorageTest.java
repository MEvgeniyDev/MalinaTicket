package ru.mevgeniy.malinaticket.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketMessage;
import ru.mevgeniy.malinaticket.model.TicketStatus;

class TicketStorageTest {
    @TempDir
    Path dataFolder;

    @Test
    void persistsTicketsMetaAndPendingNotifications() {
        UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        TicketStorage storage = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        storage.loadAll();

        Ticket ticket = storage.createTicket(owner, "TestPlayer", new Location(null, 10.2D, 64D, -5.8D, 90F, 0F), "bug", 4, "Первый текст");
        ticket.addMessage(new TicketMessage(null, "Console", true, 1_700_000_000_000L, "Ответ персонала"));
        storage.saveTicket(ticket);
        storage.addPendingNotification(owner, "Есть ответ по тикету");
        storage.setCreationBan(owner, true);

        Path backups = dataFolder.resolve("tickets").resolve("backups");
        assertTrue(Files.exists(backups.resolve("TestPlayer.yml.bak")));
        assertTrue(Files.exists(backups.resolve("_meta.yml.bak")));

        TicketStorage reloaded = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        reloaded.loadAll();

        Ticket loaded = reloaded.byId(ticket.id());
        assertNotNull(loaded);
        assertEquals("TestPlayer", loaded.ownerName());
        assertEquals(TicketStatus.OPEN, loaded.status());
        assertEquals("bug", loaded.categoryId());
        assertEquals(4, loaded.priority());
        assertEquals(2, loaded.messages().size());
        assertEquals("Ответ персонала", loaded.lastMessage().text());
        assertTrue(reloaded.isCreationBanned(owner));
        assertEquals(List.of("Есть ответ по тикету"), reloaded.drainPendingNotifications(owner));
        assertEquals(List.of(), reloaded.drainPendingNotifications(owner));
    }

    @Test
    void skipsBrokenYamlWithoutStoppingStorageLoad() throws Exception {
        Path tickets = dataFolder.resolve("tickets");
        Files.createDirectories(tickets);
        Files.writeString(tickets.resolve("Broken.yml"), "uuid: [broken");

        TicketStorage storage = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        storage.loadAll();

        assertEquals(List.of(), storage.allTickets());
    }

    @Test
    void ignoresBackupDirectoryDuringLoad() throws Exception {
        UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Path backups = dataFolder.resolve("tickets").resolve("backups");
        Files.createDirectories(backups);
        Files.writeString(backups.resolve("BackupPlayer.yml"), """
                uuid: "%s"
                name: "BackupPlayer"
                tickets:
                  99:
                    id: 99
                    owner-uuid: "%s"
                    owner-name: "BackupPlayer"
                    status: "OPEN"
                    category: "bug"
                    priority: 3
                    created-at: 100
                    updated-at: 100
                    messages:
                      - author-uuid: "%s"
                        author-name: "BackupPlayer"
                        staff: false
                        timestamp: 100
                        text: "backup"
                """.formatted(owner, owner, owner));

        TicketStorage storage = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        storage.loadAll();

        assertNull(storage.byId(99));
        assertEquals(List.of(), storage.allTickets());
    }

    @Test
    void loadsMissingOrBlankLocationWorldAsNull() throws Exception {
        UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Path tickets = dataFolder.resolve("tickets");
        Files.createDirectories(tickets);
        Files.writeString(tickets.resolve("Player.yml"), """
                uuid: "%s"
                name: "Player"
                tickets:
                  1:
                    id: 1
                    owner-uuid: "%s"
                    owner-name: "Player"
                    status: "OPEN"
                    category: "bug"
                    priority: 3
                    created-at: 100
                    updated-at: 100
                    location:
                      world: ""
                    messages:
                      - author-uuid: "%s"
                        author-name: "Player"
                        staff: false
                        timestamp: 100
                        text: "Текст"
                """.formatted(owner, owner, owner));

        TicketStorage storage = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        storage.loadAll();

        Ticket loaded = storage.byId(1);
        assertNotNull(loaded);
        assertNull(loaded.location());
    }
}
