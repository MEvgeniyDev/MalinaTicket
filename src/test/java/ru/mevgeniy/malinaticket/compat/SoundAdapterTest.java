package ru.mevgeniy.malinaticket.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Sound;
import org.junit.jupiter.api.Test;

class SoundAdapterTest {
    @Test
    void parsesModernAndLegacySoundNames() {
        assertEquals(Sound.UI_BUTTON_CLICK, SoundAdapter.parse("UI_BUTTON_CLICK"));
        assertEquals(Sound.UI_BUTTON_CLICK, SoundAdapter.parse("minecraft:ui.button.click"));
    }

    @Test
    void invalidSoundFallsBackSafely() {
        assertEquals(Sound.UI_BUTTON_CLICK, SoundAdapter.parse("missing.sound"));
    }
}
