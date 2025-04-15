package omegapoint.opera.util;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Time {
    @Test
    void localTime() {
        System.out.println(LocalDateTime.now());

        assertThrows(java.time.format.DateTimeParseException.class,
                () -> LocalDateTime.parse(Instant.now().toString()));
    }

    @Test
    void zonedTime() {

        ZonedDateTime zonedNow = ZonedDateTime.now();
        Instant now = zonedNow.toInstant();

        System.out.println(now);
        System.out.println(zonedNow);

        // Local dates won't work
        assertThrows(java.time.format.DateTimeParseException.class,
                () -> ZonedDateTime.parse(LocalDateTime.now().toString()));

        // an Instant can be parsed as ZonedDateTime.
        ZonedDateTime zoned2 = ZonedDateTime.parse(now.toString());
        System.out.println(zoned2);
        assertEquals(now.atZone(zoned2.getZone()), zoned2);
    }

    @Test
    void instant() {
        System.out.println(Instant.now());

        // a ZonedDateTime can't be parsed as an instant.
        assertThrows(java.time.format.DateTimeParseException.class,
                () -> Instant.parse(ZonedDateTime.now().toString()));

        System.out.println(ZonedDateTime.parse(OffsetDateTime.now().toString()).toInstant());
    }

    @Test
    void timestamp() {
        System.out.println(Timestamp.from(Instant.now()));
    }

    @Test
    void demo() {
        System.out.println("lokal tid:\t\t" + LocalDateTime.now());

        System.out.println("zonad tid:\t\t" + ZonedDateTime.now());

        System.out.println("ögonblick:\t\t" + Instant.now());

        System.out.println("sql timestamp:\t" + Timestamp.from(Instant.now()));
    }
}
