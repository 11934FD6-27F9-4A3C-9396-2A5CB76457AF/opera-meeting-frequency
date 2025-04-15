package omegapoint.opera.operationaljournal.api.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.operationaljournal.domain.model.Status;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
public final class StopRequest {
    private final String runId;
    private final StopBody body;

    //NOTE: The absence of JSON-annotations is intentional. This class is not a JSON model.
    public StopRequest(final String runId,
                       final StopBody body) {
        this.runId = notNull(runId);
        this.body = notNull(body);
    }

    public Either<RejectMessage, JournalItem> toJournalItem() {
        var status = body.status.trim().toLowerCase();
        return switch (status) {
            case "success", "finished" ->
                    Either.right(new JournalItem(runId, body.attempt, body.timestamp.toInstant(), Status.SUCCESS, body.message, body.originFunction));
            case "error", "reject" ->
                    Either.right(new JournalItem(runId, body.attempt, body.timestamp.toInstant(), Status.ERROR, body.message, body.originFunction));
            default -> Either.left(RejectMessage.of400(String.format("Expected status to be one of the following:\n%s",
                    Arrays.stream(Status.values()).map(Status::toString).collect(Collectors.joining(",\n")))));
        };
    }
}
