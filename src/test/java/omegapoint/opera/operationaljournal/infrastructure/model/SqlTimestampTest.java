package omegapoint.opera.operationaljournal.infrastructure.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SqlTimestampTest {

    @Test
    void toSqlString() {
        final String exampleString = "2024-05-22 10:50:27.0241083";

        final java.sql.Timestamp example = java.sql.Timestamp.valueOf(exampleString);
        final SqlTimestamp deserialized = SqlTimestamp.parse(exampleString);

        assertEquals(example.toInstant().toString(), deserialized.toSqlString());
    }

    @Test
    void parse_ZonedDateTime() {
        final ZonedDateTime now = ZonedDateTime.now();
        final SqlTimestamp deserialized = SqlTimestamp.parse(now.toString());

        assertEquals(now.toInstant(), deserialized.toInstant());
    }

    @Test
    void parse_SqlTimestamp() {
        final java.sql.Timestamp now = java.sql.Timestamp.from(Instant.now());
        final SqlTimestamp deserialized = SqlTimestamp.parse(now.toString());

        assertEquals(now, deserialized.toSqlTimestamp());
    }

    @Test
    void parse_SqlTimestamp_example() {
        final String exampleString = "2024-05-22 10:50:27.0241083";

        final java.sql.Timestamp example = java.sql.Timestamp.valueOf(exampleString);
        final SqlTimestamp deserialized = SqlTimestamp.parse(exampleString);

        assertEquals(example, deserialized.toSqlTimestamp());
    }
}