package omegapoint.opera.operationaljournal.domain.model.table;

import omegapoint.opera.operationaljournal.domain.model.Status;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JournalItemTest {

    @Test
    void create_instance_null_attempt_start1() {
        assertThrows(IllegalArgumentException.class, () -> {
            new JournalItem(
                    UUID.randomUUID().toString(),
                    null,
                    Instant.now(),
                    Status.STARTED,
                    "null",
                    null);
        });
    }

    @Test
    void create_instance_null_attempt_start2() {
        assertThrows(IllegalArgumentException.class, () -> {
            new JournalItem(
                    UUID.randomUUID().toString(),
                    null,
                    Instant.now(),
                    Status.RESTARTED,
                    "null",
                    null);
        });
    }

    @Test
    void create_instance_null_attempt_stop1() {
        assertDoesNotThrow(() -> {
            new JournalItem(
                    UUID.randomUUID().toString(),
                    null,
                    Instant.now(),
                    Status.SUCCESS,
                    "null",
                    null);
        });
    }

    @Test
    void create_instance_null_attempt_stop2() {
        assertDoesNotThrow(() -> {
            new JournalItem(
                    UUID.randomUUID().toString(),
                    null,
                    Instant.now(),
                    Status.ERROR,
                    "null",
                    null);
        });
    }
}