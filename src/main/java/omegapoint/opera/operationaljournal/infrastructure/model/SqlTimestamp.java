package omegapoint.opera.operationaljournal.infrastructure.model;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class SqlTimestamp {

    private final Instant instant;

    private SqlTimestamp(final Instant instant) {
        this.instant = instant;
    }

    public ZonedDateTime toUTCZonedDateTime() {
        return instant.atZone(ZoneOffset.UTC);
    }

    public Instant toInstant() {
        return instant;
    }

    public java.sql.Timestamp toSqlTimestamp() {
        return java.sql.Timestamp.from(instant);
    }

    public String toSqlString() {
        return instant.toString();
    }

    @Override
    public String toString() {
        return toSqlString();
    }

    public static SqlTimestamp parse(String serialized) {
        try {
            return new SqlTimestamp(ZonedDateTime.parse(serialized).toInstant());
        } catch (DateTimeException e) {
            return new SqlTimestamp(java.sql.Timestamp.valueOf(serialized).toInstant());
        }
    }

    public static SqlTimestamp fromInstant(Instant instant) {
        return new SqlTimestamp(instant);
    }
}
