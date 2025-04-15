package omegapoint.opera.operationaljournal.api.model.response;

import lombok.EqualsAndHashCode;
import omegapoint.opera.operationaljournal.domain.model.WebhookStep;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;
import omegapoint.opera.operationaljournal.domain.model.table.QueueCheckpoint;
import omegapoint.opera.operationaljournal.domain.model.table.RunItem;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
public final class Run {
    private final UUID runID;
    private final UUID parentId;
    private final ZonedDateTime originTimestamp;
    private final WebhookStep step;
    private final String conflictId;
    private final QueueCheckpoint queueCheckpoint;
    private final List<JournalEntry> journal;

    public Run(final UUID runID,
               final UUID parentId,
               final ZonedDateTime originTimestamp,
               final WebhookStep step,
               final String conflictId,
               final QueueCheckpoint queueCheckpoint,
               final List<JournalEntry> journal) {
        this.runID = notNull(runID);
        this.parentId = parentId;
        this.originTimestamp = notNull(originTimestamp);
        this.step = notNull(step);
        this.conflictId = conflictId;
        this.queueCheckpoint = notNull(queueCheckpoint);
        this.journal = journal;
    }

    public static Run fromDomain(RunItem runItem, List<JournalItem> journal) {
        var runId = runItem.runID.toString();
        journal.forEach(j -> {
            if (!j.runID.equals(runId))
                throw new IllegalArgumentException("The runId of journal entry %s does not match the runId of %s".formatted(j, runItem));
        });

        return new Run(runItem.runID,
                runItem.parentId,
                runItem.originTimestamp.atZone(ZoneOffset.UTC),
                runItem.step,
                runItem.conflictId.id,
                runItem.queueCheckpoint,
                journal.stream().map(JournalEntry::fromDomain).toList());
    }
}
