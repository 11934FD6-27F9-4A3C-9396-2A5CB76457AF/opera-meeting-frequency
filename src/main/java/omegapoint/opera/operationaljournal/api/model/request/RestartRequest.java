package omegapoint.opera.operationaljournal.api.model.request;

import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.operationaljournal.domain.model.Status;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;

@EqualsAndHashCode
public final class RestartRequest {
    private final String runId;
    private final RestartBody body;

    //NOTE: The absence of JSON-annotations is intentional. This class is not a JSON model.
    public RestartRequest(final String runId,
                          final RestartBody body) {
        this.runId = runId;
        this.body = body;
    }

    public Either<RejectMessage, JournalItem> toJournalItem() {
        try {
            return Either.right(new JournalItem(runId, body.attempt(), body.timestamp().toInstant(), Status.RESTARTED, null, body.originFunction()));
        } catch (Exception e) {
            return Either.left(RejectMessage.of400("Faulty rerun request"));
        }
    }
}
