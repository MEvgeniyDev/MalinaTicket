package ru.mevgeniy.malinaticket.config;

import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginSettings {
    private final JavaPlugin plugin;
    private int cooldownSeconds;
    private int maxOpenTickets;
    private int maxMessageLength;
    private int ticketsPerPage;
    private boolean guiClickSound;
    private Sound clickSound;
    private String dateFormat;
    private String timeZone;
    private boolean captureChatInputs;

    public PluginSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        cooldownSeconds = config.getInt("settings.cooldown-seconds", 60);
        maxOpenTickets = config.getInt("settings.max-open-tickets", 3);
        maxMessageLength = config.getInt("settings.max-message-length", 500);
        ticketsPerPage = Math.max(1, Math.min(45, config.getInt("settings.tickets-per-page", 45)));
        guiClickSound = config.getBoolean("settings.gui-click-sound.enabled", true);
        clickSound = parseSound(config.getString("settings.gui-click-sound.sound", "UI_BUTTON_CLICK"));
        dateFormat = config.getString("settings.date-format", "dd.MM.yyyy HH:mm");
        timeZone = config.getString("settings.time-zone", "Europe/Moscow");
        captureChatInputs = config.getBoolean("settings.capture-chat-inputs", true);
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public int maxOpenTickets() {
        return maxOpenTickets;
    }

    public int maxMessageLength() {
        return maxMessageLength;
    }

    public int ticketsPerPage() {
        return ticketsPerPage;
    }

    public boolean guiClickSound() {
        return guiClickSound;
    }

    public Sound clickSound() {
        return clickSound;
    }

    public String dateFormat() {
        return dateFormat;
    }

    public String timeZone() {
        return timeZone;
    }

    public boolean captureChatInputs() {
        return captureChatInputs;
    }

    private Sound parseSound(String soundName) {
        if (soundName == null || soundName.isBlank()) {
            return Sound.UI_BUTTON_CLICK;
        }
        NamespacedKey key = soundKey(soundName);
        Sound sound = key == null ? null : Registry.SOUND_EVENT.get(key);
        return sound == null ? Sound.UI_BUTTON_CLICK : sound;
    }

    private NamespacedKey soundKey(String soundName) {
        String normalized = soundName.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            return NamespacedKey.fromString(normalized);
        }
        if (!normalized.contains(".")) {
            normalized = normalized.replace('_', '.');
        }
        return NamespacedKey.minecraft(normalized);
    }
}
