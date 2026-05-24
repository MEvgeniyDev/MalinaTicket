package ru.mevgeniy.malinaticket.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeFormatterTest {
    @Test
    void formatsPositiveTimestampInConfiguredZone() {
        TimeFormatter formatter = new TimeFormatter("yyyy-MM-dd HH:mm", "UTC");

        assertEquals("2023-11-14 22:13", formatter.format(1_700_000_000_000L));
    }

    @Test
    void formatsMissingTimestampAsDash() {
        TimeFormatter formatter = new TimeFormatter("dd.MM.yyyy HH:mm", "Europe/Moscow");

        assertEquals("-", formatter.format(0L));
        assertEquals("-", formatter.format(-1L));
    }

    @Test
    void fallsBackWhenPatternOrZoneIsInvalid() {
        TimeFormatter formatter = new TimeFormatter("bad pattern [", "bad/zone");

        assertEquals("15.11.2023 01:13", formatter.format(1_700_000_000_000L));
    }
}
