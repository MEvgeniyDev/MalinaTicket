package ru.mevgeniy.malinaticket.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mevgeniy.malinaticket.model.Ticket;
import ru.mevgeniy.malinaticket.model.TicketLocation;
import ru.mevgeniy.malinaticket.model.TicketMessage;
import ru.mevgeniy.malinaticket.model.TicketStatus;

public final class TicketStorage {
    private final Logger logger;
    private final YamlFiles yamlFiles;
    private final File ticketsDirectory;
    private final File backupDirectory;
    private final File metaFile;
    private final Map<Integer, Ticket> ticketsById = new HashMap<>();
    private final Map<UUID, String> ownerFileNames = new HashMap<>();
    private final Map<UUID, List<String>> pendingNotifications = new HashMap<>();
    private final Set<UUID> creationBans = new HashSet<>();
    private int nextId = 1;

    public TicketStorage(JavaPlugin plugin) {
        this(plugin.getDataFolder(), plugin.getLogger());
    }

    TicketStorage(File dataFolder, Logger logger) {
        this.logger = logger;
        this.ticketsDirectory = new File(dataFolder, "tickets");
        this.backupDirectory = new File(ticketsDirectory, "backups");
        this.yamlFiles = new YamlFiles(logger, backupDirectory);
        this.metaFile = new File(ticketsDirectory, "_meta.yml");
    }

    public synchronized void loadAll() {
        ticketsDirectory.mkdirs();
        ticketsById.clear();
        ownerFileNames.clear();
        pendingNotifications.clear();
        creationBans.clear();
        nextId = 1;
        loadMeta();

        File[] files = ticketsDirectory.listFiles((directory, name) ->
                name.toLowerCase(Locale.ROOT).endsWith(".yml") && !name.equalsIgnoreCase("_meta.yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            loadPlayerFile(file);
        }
        int highestId = ticketsById.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (nextId <= highestId) {
            nextId = highestId + 1;
            saveMeta();
        }
    }

    public synchronized Ticket createTicket(UUID ownerUuid, String ownerName, Location location, String categoryId, int priority, String text) {
        long now = System.currentTimeMillis();
        Ticket ticket = new Ticket(
                nextId++,
                ownerUuid,
                ownerName,
                TicketStatus.OPEN,
                categoryId,
                priority,
                now,
                now,
                TicketLocation.from(location),
                List.of(new TicketMessage(ownerUuid, ownerName, false, now, text))
        );
        ticketsById.put(ticket.id(), ticket);
        ownerFileNames.put(ownerUuid, fileName(ownerName));
        saveTicket(ticket);
        saveMeta();
        return ticket;
    }

    public synchronized Ticket byId(int id) {
        return ticketsById.get(id);
    }

    public synchronized List<Ticket> allTickets() {
        return ticketsById.values().stream()
                .sorted(Comparator.comparingLong(Ticket::createdAt).reversed())
                .toList();
    }

    public synchronized List<Ticket> ownTickets(UUID ownerUuid, boolean includeClosed, boolean includeDeleted) {
        return ticketsById.values().stream()
                .filter(ticket -> ticket.ownerUuid().equals(ownerUuid))
                .filter(ticket -> includeClosed || ticket.status() == TicketStatus.OPEN)
                .filter(ticket -> includeDeleted || ticket.status() != TicketStatus.DELETED)
                .sorted(Comparator.comparingLong(Ticket::createdAt).reversed())
                .toList();
    }

    public synchronized List<Ticket> staffTickets(TicketStatus status, String categoryId, UUID assignedTo) {
        return ticketsById.values().stream()
                .filter(ticket -> status == null || ticket.status() == status)
                .filter(ticket -> categoryId == null || categoryId.equalsIgnoreCase(ticket.categoryId()))
                .filter(ticket -> assignedTo == null || assignedTo.equals(ticket.assignedToUuid()))
                .sorted(Comparator.comparingLong(Ticket::createdAt).reversed())
                .toList();
    }

    public synchronized long countOpen(UUID ownerUuid) {
        return ticketsById.values().stream()
                .filter(ticket -> ticket.ownerUuid().equals(ownerUuid))
                .filter(ticket -> ticket.status() == TicketStatus.OPEN)
                .count();
    }

    public synchronized long countByStatus(TicketStatus status) {
        return ticketsById.values().stream()
                .filter(ticket -> ticket.status() == status)
                .count();
    }

    public synchronized void saveTicket(Ticket ticket) {
        saveOwnerFile(ticket.ownerUuid());
    }

    public synchronized void purge(Ticket ticket) {
        ticketsById.remove(ticket.id());
        saveOwnerFile(ticket.ownerUuid());
    }

    public synchronized void setCreationBan(UUID uuid, boolean banned) {
        if (banned) {
            creationBans.add(uuid);
        } else {
            creationBans.remove(uuid);
        }
        saveMeta();
    }

    public synchronized boolean isCreationBanned(UUID uuid) {
        return creationBans.contains(uuid);
    }

    public synchronized void addPendingNotification(UUID uuid, String text) {
        pendingNotifications.computeIfAbsent(uuid, ignored -> new ArrayList<>()).add(text);
        saveOwnerFile(uuid);
    }

    public synchronized List<String> drainPendingNotifications(UUID uuid) {
        List<String> result = new ArrayList<>(pendingNotifications.getOrDefault(uuid, List.of()));
        pendingNotifications.remove(uuid);
        saveOwnerFile(uuid);
        return result;
    }

    public synchronized void refreshOwnerName(UUID uuid, String newName) {
        boolean changed = false;
        for (Ticket ticket : ticketsById.values()) {
            if (ticket.ownerUuid().equals(uuid) && !ticket.ownerName().equals(newName)) {
                ticket.ownerName(newName);
                changed = true;
            }
        }
        if (changed || ownerFileNames.containsKey(uuid)) {
            ownerFileNames.put(uuid, fileName(newName));
            saveOwnerFile(uuid);
        }
    }

    private void loadMeta() {
        if (!metaFile.exists()) {
            saveMeta();
            return;
        }
        YamlConfiguration config = yamlFiles.load(metaFile, "meta-файл тикетов");
        if (config == null) {
            return;
        }
        nextId = config.getInt("next-id", 1);
        for (String rawUuid : config.getStringList("creation-bans")) {
            UUID uuid = parseUuid(rawUuid);
            if (uuid != null) {
                creationBans.add(uuid);
            }
        }
    }

    private void saveMeta() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("next-id", nextId);
        config.set("creation-bans", creationBans.stream().map(UUID::toString).sorted().toList());
        yamlFiles.saveSafely(config, metaFile, "tickets/_meta.yml");
    }

    private void loadPlayerFile(File file) {
        YamlConfiguration config = yamlFiles.load(file, "тикеты игрока");
        if (config == null) {
            return;
        }
        UUID ownerUuid = parseUuid(config.getString("uuid"));
        String ownerName = config.getString("name", file.getName().replace(".yml", ""));
        if (ownerUuid != null) {
            ownerFileNames.put(ownerUuid, file.getName());
            List<String> pending = config.getStringList("pending-notifications");
            if (!pending.isEmpty()) {
                pendingNotifications.put(ownerUuid, new ArrayList<>(pending));
            }
        }

        ConfigurationSection ticketsSection = config.getConfigurationSection("tickets");
        if (ticketsSection == null) {
            return;
        }

        for (String key : ticketsSection.getKeys(false)) {
            String base = "tickets." + key + ".";
            int id = config.getInt(base + "id", parseInt(key, -1));
            UUID ticketOwnerUuid = parseUuid(config.getString(base + "owner-uuid"));
            if (ticketOwnerUuid == null) {
                ticketOwnerUuid = ownerUuid;
            }
            if (id <= 0 || ticketOwnerUuid == null) {
                continue;
            }
            Ticket ticket = new Ticket(
                    id,
                    ticketOwnerUuid,
                    config.getString(base + "owner-name", ownerName),
                    TicketStatus.fromString(config.getString(base + "status")),
                    config.getString(base + "category", "other"),
                    config.getInt(base + "priority", 3),
                    config.getLong(base + "created-at", System.currentTimeMillis()),
                    config.getLong(base + "updated-at", System.currentTimeMillis()),
                    loadLocation(config, base + "location."),
                    loadMessages(config, base + "messages")
            );
            ticket.assign(parseUuid(config.getString(base + "assigned-to-uuid")), config.getString(base + "assigned-to-name"));
            ticket.updatedAt(config.getLong(base + "updated-at", ticket.updatedAt()));
            ticket.closedMeta(
                    parseUuid(config.getString(base + "closed-by-uuid")),
                    config.getString(base + "closed-by-name"),
                    config.getLong(base + "closed-at", 0L),
                    config.getString(base + "close-reason")
            );
            ticket.deletedMeta(
                    parseUuid(config.getString(base + "deleted-by-uuid")),
                    config.getString(base + "deleted-by-name"),
                    config.getLong(base + "deleted-at", 0L)
            );
            ticket.updatedAt(config.getLong(base + "updated-at", ticket.updatedAt()));
            Ticket previous = ticketsById.put(id, ticket);
            if (previous != null) {
                logger.warning("Дубликат ID тикета " + id + " в файле " + file.getName() + ". Предыдущая запись была заменена.");
            }
            ownerFileNames.put(ticket.ownerUuid(), file.getName());
        }
    }

    private void saveOwnerFile(UUID ownerUuid) {
        List<Ticket> ownerTickets = ticketsById.values().stream()
                .filter(ticket -> ticket.ownerUuid().equals(ownerUuid))
                .sorted(Comparator.comparingInt(Ticket::id))
                .toList();
        String ownerName = ownerTickets.isEmpty() ? ownerFileNames.getOrDefault(ownerUuid, ownerUuid.toString()) : ownerTickets.get(0).ownerName();
        String newFileName = fileName(ownerName);
        File target = new File(ticketsDirectory, newFileName);
        YamlConfiguration config = new YamlConfiguration();
        config.set("uuid", ownerUuid.toString());
        config.set("name", ownerName);
        List<String> pending = pendingNotifications.getOrDefault(ownerUuid, List.of());
        if (!pending.isEmpty()) {
            config.set("pending-notifications", pending);
        }
        for (Ticket ticket : ownerTickets) {
            saveTicketToConfig(config, ticket);
        }
        if (yamlFiles.saveSafely(config, target, "тикеты игрока " + ownerName)) {
            String oldFileName = ownerFileNames.put(ownerUuid, newFileName);
            if (oldFileName != null && !oldFileName.equalsIgnoreCase(newFileName)) {
                File oldFile = new File(ticketsDirectory, oldFileName);
                if (oldFile.exists() && !oldFile.equals(target)) {
                    boolean deleted = oldFile.delete();
                    if (!deleted) {
                        logger.warning("Не удалось удалить старый файл тикетов: " + oldFile.getPath());
                    }
                }
            }
        }
    }

    private void saveTicketToConfig(YamlConfiguration config, Ticket ticket) {
        String base = "tickets." + ticket.id() + ".";
        config.set(base + "id", ticket.id());
        config.set(base + "owner-uuid", ticket.ownerUuid().toString());
        config.set(base + "owner-name", ticket.ownerName());
        config.set(base + "status", ticket.status().name());
        config.set(base + "category", ticket.categoryId());
        config.set(base + "priority", ticket.priority());
        config.set(base + "created-at", ticket.createdAt());
        config.set(base + "updated-at", ticket.updatedAt());
        if (ticket.location() != null) {
            config.set(base + "location.world", ticket.location().world());
            config.set(base + "location.x", ticket.location().x());
            config.set(base + "location.y", ticket.location().y());
            config.set(base + "location.z", ticket.location().z());
            config.set(base + "location.yaw", ticket.location().yaw());
            config.set(base + "location.pitch", ticket.location().pitch());
        }
        if (ticket.assignedToUuid() != null) {
            config.set(base + "assigned-to-uuid", ticket.assignedToUuid().toString());
            config.set(base + "assigned-to-name", ticket.assignedToName());
        }
        if (ticket.closedByUuid() != null) {
            config.set(base + "closed-by-uuid", ticket.closedByUuid().toString());
            config.set(base + "closed-by-name", ticket.closedByName());
            config.set(base + "closed-at", ticket.closedAt());
            config.set(base + "close-reason", ticket.closeReason());
        }
        if (ticket.deletedByUuid() != null) {
            config.set(base + "deleted-by-uuid", ticket.deletedByUuid().toString());
            config.set(base + "deleted-by-name", ticket.deletedByName());
            config.set(base + "deleted-at", ticket.deletedAt());
        }
        List<Map<String, Object>> messages = ticket.messages().stream()
                .map(message -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("author-uuid", message.authorUuid() == null ? "" : message.authorUuid().toString());
                    map.put("author-name", message.authorName());
                    map.put("staff", message.staff());
                    map.put("timestamp", message.timestamp());
                    map.put("text", message.text());
                    return map;
                })
                .toList();
        config.set(base + "messages", messages);
    }

    private TicketLocation loadLocation(FileConfiguration config, String base) {
        String world = config.getString(base + "world");
        if (world == null || world.isBlank()) {
            return null;
        }
        return new TicketLocation(
                world,
                config.getDouble(base + "x", 0D),
                config.getDouble(base + "y", 0D),
                config.getDouble(base + "z", 0D),
                (float) config.getDouble(base + "yaw", 0D),
                (float) config.getDouble(base + "pitch", 0D)
        );
    }

    private List<TicketMessage> loadMessages(FileConfiguration config, String path) {
        List<TicketMessage> result = new ArrayList<>();
        for (Map<?, ?> map : config.getMapList(path)) {
            UUID uuid = parseUuid(mapString(map, "author-uuid", ""));
            String name = mapString(map, "author-name", "Неизвестно");
            boolean staff = Boolean.parseBoolean(mapString(map, "staff", "false"));
            long timestamp = parseLong(mapString(map, "timestamp", "0"), System.currentTimeMillis());
            String text = mapString(map, "text", "");
            result.add(new TicketMessage(uuid, name, staff, timestamp, text));
        }
        return result;
    }

    private String mapString(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String fileName(String playerName) {
        String cleaned = playerName == null ? "unknown" : playerName.replaceAll("[^A-Za-z0-9_\\-.]", "_");
        if (cleaned.isBlank()) {
            return "unknown.yml";
        }
        return cleaned + ".yml";
    }
}
