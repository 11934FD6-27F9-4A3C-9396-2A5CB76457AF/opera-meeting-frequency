package omegapoint.opera.operationaljournal.domain.repository;

import io.vavr.control.Either;
import lombok.NonNull;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;

import java.util.UUID;

public interface IntegrationTests {
    Either<RejectMessage, Boolean> isIntegrationTest(final @NonNull UUID runId);
    Either<RejectMessage, Void> sendError(final @NonNull UUID rootRunId,
                                          final @NonNull JournalItem journalItem,
                                          final @NonNull String webhookStep);
}
