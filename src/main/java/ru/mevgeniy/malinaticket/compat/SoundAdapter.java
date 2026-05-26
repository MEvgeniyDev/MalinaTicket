package ru.mevgeniy.malinaticket.compat;

import java.util.Locale;
import org.bukkit.Sound;

public final class SoundAdapter {
    private SoundAdapter() {
    }

    public static Sound parse(String soundName) {
        if (soundName == null || soundName.isBlank()) {
            return fallback();
        }
        String normalized = soundName.trim();
        int namespaceIndex = normalized.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex < normalized.length() - 1) {
            normalized = normalized.substring(namespaceIndex + 1);
        }
        normalized = normalized.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return Sound.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return fallback();
        }
    }

    private static Sound fallback() {
        for (String name : new String[]{"UI_BUTTON_CLICK", "BLOCK_NOTE_BLOCK_PLING", "CLICK"}) {
            try {
                return Sound.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Sound.values()[0];
    }
}
