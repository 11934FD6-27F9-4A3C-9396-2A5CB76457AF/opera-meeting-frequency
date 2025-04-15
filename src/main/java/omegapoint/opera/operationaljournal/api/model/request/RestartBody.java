package omegapoint.opera.operationaljournal.api.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.operationaljournal.config.JacksonConfig;
import reactor.util.annotation.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

@EqualsAndHashCode
@Getter @Accessors(fluent = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class RestartBody {
    private final @NonNull ZonedDateTime timestamp;
    private final int attempt;
    private final @Nullable String originFunction;

    @JsonCreator
    public RestartBody(final @NonNull @JsonProperty("timestamp") String timestamp,
                       final @NonNull @JsonProperty("attempt") Integer attempt,
                       final @Nullable @JsonProperty("originFunction") String originFunction) {

        this.timestamp = ZonedDateTime.parse(timestamp);
        this.originFunction = originFunction;

        if (attempt > 1) {
            this.attempt = attempt;
        }
        else {
            throw new IllegalArgumentException("Attempt must be greater than 1. A restart can't be the first attempt.");
        }
    }

    public RestartRequest toRestartRequest(String runId) {
        return new RestartRequest(runId, this);
    }

    public static Either<RejectMessage, RestartBody> deserialize(final String body) {
        try {
            return Either.right(JacksonConfig.objectMapper().readValue(body, RestartBody.class));
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
