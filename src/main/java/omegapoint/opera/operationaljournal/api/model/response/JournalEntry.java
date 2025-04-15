package omegapoint.opera.operationaljournal.api.model.response;

import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

@EqualsAndHashCode
public final class JournalEntry {
    private final String runId;
    private final ZonedDateTime timestamp;
    private final String status;
    private final String message;

    public JournalEntry(final String runId,
                        final ZonedDateTime timestamp,
                        final String status,
                        final String message) {
        this.runId = runId;
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
    }

    public static JournalEntry fromDomain(JournalItem journalItem) {
        return new JournalEntry(journalItem.runID,
                journalItem.timestamp.atZone(ZoneOffset.UTC),
                journalItem.status.toString(),
                journalItem.message);
    }
}
