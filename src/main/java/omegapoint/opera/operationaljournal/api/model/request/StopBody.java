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

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import static org.apache.commons.lang3.Validate.notNull;

@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StopBody {
    public final Integer attempt;
    public final ZonedDateTime timestamp;
    public final String status;
    public final String message;
    public final String originFunction;

    @JsonCreator
    public StopBody(@JsonProperty("attempt") final Integer attempt,
                    @JsonProperty("timestamp") final String timestamp,
                    @JsonProperty("status") final String status,
                    @JsonProperty("message") final String message,
                    @JsonProperty("originFunction") final String originFunction) {

        // TODO: 2024-04-29 [mbloms] Make attempt mandatory

        this.attempt = attempt;
        this.timestamp = deserializeTimestamp(timestamp);
        this.status = notNull(status);
        this.message = message;
        this.originFunction = originFunction;
    }

    /**
     * Deserialize string to ZonedDateTime, fall back on null.
     * The purpose of this is that /stop needs to be more forgiving.
     */
    private static ZonedDateTime deserializeTimestamp(@NonNull String timestamp) {
        try {
            return ZonedDateTime.parse(notNull(timestamp));
        }
        catch (DateTimeParseException parseException) {
            parseException.printStackTrace(System.err);
            try {
                return JacksonConfig.objectMapper().readValue(timestamp, ZonedDateTime.class);
            } catch (JsonProcessingException jsonProcessingException) {
                jsonProcessingException.printStackTrace(System.err);
                throw parseException;
            }
        }
    }

    public StopRequest toStopRequest(String runId) {
        return new StopRequest(runId, this);
    }

    public static Either<RejectMessage, StopBody> deserialize(final String body) {
        try {
            return Either.right(JacksonConfig.objectMapper().readValue(body, StopBody.class));
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
