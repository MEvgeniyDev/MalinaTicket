package ru.mevgeniy.malinaticket.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertNotNull(storage.updateTicket(ticket, changed ->
                changed.addMessage(new TicketMessage(null, "Console", true, 1_700_000_000_000L, "Ответ персонала"))));
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
        UUID owner = UUID.randomUUID();
        assertNotNull(storage.createTicket(owner, "Broken", null, "bug", 3, "Текст"));
        assertEquals("uuid: [broken", Files.readString(tickets.resolve("Broken.yml")));
        assertTrue(Files.exists(tickets.resolve(owner + ".yml")));
    }

    @Test
    void keepsBothOwnersWhenPlayerRenamesToAnOccupiedName() throws Exception {
        TicketStorage storage = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        storage.loadAll();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Ticket original = storage.createTicket(first, "Original", null, "bug", 3, "Первый");
        Ticket occupied = storage.createTicket(second, "Occupied", null, "bug", 3, "Второй");
        Path directory = dataFolder.resolve("tickets");
        String occupiedYaml = Files.readString(directory.resolve("Occupied.yml"));

        storage.refreshOwnerName(first, "Occupied");
        assertTrue(Files.exists(directory.resolve(first + ".yml")));
        assertFalse(Files.exists(directory.resolve("Original.yml")));
        assertEquals(occupiedYaml, Files.readString(directory.resolve("Occupied.yml")));
        storage.loadAll();
        assertEquals(first, storage.byId(original.id()).ownerUuid());
        assertEquals(second, storage.byId(occupied.id()).ownerUuid());
        assertEquals("Occupied", storage.byId(original.id()).ownerName());
    }

    @Test
    void rollsBackTicketChangesWhenBackupCannotBeWritten() throws Exception {
        TicketStorage storage = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        storage.loadAll();
        UUID owner = UUID.randomUUID();
        Ticket ticket = storage.createTicket(owner, "TestPlayer", null, "bug", 3, "Текст");
        Path backup = dataFolder.resolve("tickets/backups/TestPlayer.yml.bak");
        Files.createDirectory(backup);
        Files.writeString(backup.resolve("keep"), "block copy");

        assertNull(storage.updateTicket(ticket, changed -> changed.close(null, "Консоль", "Причина")));
        assertFalse(storage.purge(ticket));
        assertEquals(TicketStatus.OPEN, storage.byId(ticket.id()).status());
        storage.loadAll();
        assertEquals(TicketStatus.OPEN, storage.byId(ticket.id()).status());
    }

    @Test
    void keepsConsoleCloseReasonAfterRestart() {
        TicketStorage storage = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        storage.loadAll();
        Ticket ticket = storage.createTicket(UUID.randomUUID(), "TestPlayer", null, "bug", 3, "Текст");
        assertNotNull(storage.updateTicket(ticket, changed -> changed.close(null, "Консоль", "Решено")));

        storage.loadAll();
        assertEquals("Решено", storage.byId(ticket.id()).closeReason());
        assertEquals("Консоль", storage.byId(ticket.id()).closedByName());
    }

    @Test
    void newerEmptyLegacyUuidFileDoesNotHidePlayerTickets() throws Exception {
        TicketStorage storage = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        storage.loadAll();
        UUID owner = UUID.randomUUID();
        Ticket ticket = storage.createTicket(owner, "TestPlayer", null, "bug", 3, "Тикет игрока");
        assertNotNull(ticket);
        Path directory = dataFolder.resolve("tickets");
        Files.setLastModifiedTime(directory.resolve("TestPlayer.yml"), java.nio.file.attribute.FileTime.fromMillis(1_000L));
        Files.writeString(directory.resolve(owner + ".yml"), "uuid: " + owner + "\nname: " + owner + "\n");

        storage.loadAll();

        Ticket loaded = storage.byId(ticket.id());
        assertNotNull(loaded);
        assertEquals(owner, loaded.ownerUuid());
        assertEquals("TestPlayer", loaded.ownerName());
    }

    @Test
    void olderFileLeftAfterRenameCannotRestorePurgedTicket() throws Exception {
        TicketStorage storage = new TicketStorage(dataFolder.toFile(), Logger.getLogger("MalinaTicketTest"));
        storage.loadAll();
        UUID owner = UUID.randomUUID();
        Ticket ticket = storage.createTicket(owner, "OldName", null, "bug", 3, "Текст");
        Path oldFile = dataFolder.resolve("tickets/OldName.yml");
        String oldYaml = Files.readString(oldFile);
        storage.refreshOwnerName(owner, "NewName");
        assertTrue(storage.purge(storage.byId(ticket.id())));
        Files.writeString(oldFile, oldYaml);
        Files.setLastModifiedTime(oldFile, java.nio.file.attribute.FileTime.fromMillis(1_000L));

        storage.loadAll();
        assertNull(storage.byId(ticket.id()));
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
