package ru.mevgeniy.malinaticket.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import org.bukkit.Sound;

public final class SoundAdapter {
    private static final Method LOOKUP = findLookup();

    private SoundAdapter() {
    }

    public static Sound parse(String soundName) {
        if (soundName != null && !soundName.isBlank()) {
            String name = soundName.trim();
            Sound sound = Sound.class.isInterface() ? lookup(name.toLowerCase(Locale.ROOT)) : null;
            if (sound != null) {
                return sound;
            }
            if (!name.contains(":") || name.startsWith("minecraft:")) {
                String legacy = name.substring(name.indexOf(':') + 1).replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
                sound = lookup(legacy);
                if (sound != null) {
                    return sound;
                }
            }
        }
        return lookup("UI_BUTTON_CLICK");
    }

    private static Method findLookup() {
        try {
            return Sound.class.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static Sound lookup(String name) {
        try {
            return LOOKUP == null ? (Sound) Sound.class.getField(name).get(null) : (Sound) LOOKUP.invoke(null, name);
        } catch (ReflectiveOperationException exception) {
            if (exception instanceof InvocationTargetException invocation
                    && !(invocation.getCause() instanceof IllegalArgumentException)) {
                throw new IllegalStateException("Ошибка поиска звука " + name, invocation.getCause());
            }
            return null;
        }
    }
}
