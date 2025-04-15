package omegapoint.opera.operationaljournal.domain.repository;

import io.vavr.control.Either;
import lombok.NonNull;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.Success;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.domain.model.StartItem;
import omegapoint.opera.operationaljournal.domain.model.table.ConflictItem;
import omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;
import omegapoint.opera.operationaljournal.domain.model.table.RunItem;
import omegapoint.opera.operationaljournal.infrastructure.model.Rerun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatabaseRepository {
    Either<RejectMessage, SuccessMessage> putJournal(final JournalItem journalItem);

    Either<RejectMessage, SuccessMessage> putRunAndQueueCheckpoint(final RunItem runItem);

    Either<RejectMessage, SuccessMessage> putConflict(final ConflictItem conflictItem);

    Either<RejectMessage, omegapoint.opera.operationaljournal.infrastructure.model.RunItem[]> getExistingRun(final String runId);

    Either<RejectMessage, omegapoint.opera.operationaljournal.infrastructure.model.JournalItem[]> getExistingJournal(final String runId);

    Either<RejectMessage, List<omegapoint.opera.operationaljournal.domain.model.table.Rerun>> getReruns();

    Either<RejectMessage, omegapoint.opera.operationaljournal.domain.model.table.Rerun> getRerunInfo(final String runId);

    Either<RejectMessage, Rerun[]> getCinodeReruns();

    Either<RejectMessage, SuccessMessage> putStart(final StartItem startItem);

    Either<RejectMessage, SuccessMessage> putStop(final JournalItem journalItem);

    Either<RejectMessage, SuccessMessage> putRestart(final JournalItem journalItem);

    Either<RejectMessage, SuccessMessage> restrictRerun(final JournalItem journalItem);

    Either<RejectMessage, Optional<UUID>> getRootRunId(final @NonNull UUID runId);

    Either<RejectMessage, String> getWebhookStep(final @NonNull UUID runId);

    Either<RejectMessage, String> getParentWebhookStep(final @NonNull UUID runId);

    Either<RejectMessage, List<JournalErrorItem>> getRunsWithExhaustedRetriesFromLast24Hours();
}
