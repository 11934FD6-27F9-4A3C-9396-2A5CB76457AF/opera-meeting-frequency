package omegapoint.opera.operationaljournal.api.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.operationaljournal.config.JacksonConfig;
import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import reactor.util.annotation.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StartBody {
    public final UUID parentRunId;
    public final BlobReference blobReference;
    public final WebhookStep webhookStep;
    public final ZonedDateTime originTimestamp;
    public final ZonedDateTime runStartTime;
    public final String conflictId;
    public final String originFunction;
    public final Checkpoint checkpoint;
    public final String operationType;
    public final String message;

    @JsonCreator
    public StartBody(@JsonProperty("parentRunId") final UUID parentRunId,
                     @JsonProperty("blobReference") final BlobReference blobReference,
                     @JsonProperty("webhookStep") final String webhookStep,
                     @JsonProperty("originTimestamp") final String originTimestamp,
                     @JsonProperty("runStartTime") final String runStartTime,
                     @JsonProperty("conflictId") final String conflictId,
                     @JsonProperty("originFunction") final String originFunction,
                     @JsonProperty("checkpoint") final Checkpoint checkpoint,
                     // Live eller batch. Utgå från live nu?
                     @JsonProperty("operationType") final String operationType,
                     @JsonProperty("message") final String message) {
        this.parentRunId = parentRunId;
        this.blobReference = notNull(blobReference);
        this.webhookStep = new WebhookStep(webhookStep);
        this.originTimestamp = ZonedDateTime.parse(notNull(originTimestamp));
        this.runStartTime = ZonedDateTime.parse(notNull(runStartTime));
        this.conflictId = conflictId;
        this.originFunction = originFunction;
        this.checkpoint = notNull(checkpoint);
        this.operationType = operationType;
        this.message = message;
    }

    public StartRequest toStartRequest(String runId) {
        return new StartRequest(runId, this);
    }

    public static Either<RejectMessage, StartBody> deserialize(String body) {
        try {
            return Either.right(JacksonConfig.objectMapper().readValue(body, StartBody.class));
        } catch (JsonProcessingException e) {

            if (e.getCause() instanceof DateTimeParseException parseException) {
                return Either.left(RejectMessage.of400(
                        "%s is not a date time with a time zone. Use java.time.Instant or java.time.ZonedDateTime."
                                .formatted(parseException.getParsedString())));
            } else if (e.getCause() instanceof NullPointerException) {
                return Either.left(RejectMessage.of400("Some field is null: " + e.getMessage()));
            }
            return Either.left(RejectMessage.of400(e.getMessage()));
        }
    }
}
