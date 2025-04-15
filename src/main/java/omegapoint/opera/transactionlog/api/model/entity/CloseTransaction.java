package omegapoint.opera.transactionlog.api.model.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vavr.control.Either;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.transactionlog.config.JacksonConfig;
import omegapoint.opera.transactionlog.domain.model.valueobject.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloseTransaction {
    public final UUID id;
    public final String endTime;
    public final String numberOfRecords;
    public final String blobPath;
    public final String isSuccess;
    public final String reason;

    @JsonCreator
    public CloseTransaction(@JsonProperty("id") final UUID id,
                            @JsonProperty("endTime") final String endTime,
                            @JsonProperty("numberOfRecords") final String numberOfRecords,
                            @JsonProperty("blobPath") final String blobPath,
                            @JsonProperty("isSuccess") final String isSuccess,
                            @JsonProperty("reason") final String reason) {
        this.id = notNull(id);
        this.endTime = notNull(endTime);
        this.numberOfRecords = notNull(numberOfRecords);
        this.blobPath = notNull(blobPath);
        this.isSuccess = notNull(isSuccess);
        this.reason = notNull(reason);
    }

    public static Either<RejectMessage, omegapoint.opera.transactionlog.domain.model.entity.CloseTransaction> toDomain(final CloseTransaction closeTransaction) {
        notNull(closeTransaction);
        
        try {
            // TODO: 2024-02-22 [tw] move this switch logic into a valueobject called isSuccess.
            Boolean isSuccess;
            switch (closeTransaction.isSuccess.toLowerCase()) {
                case "true" -> isSuccess = Boolean.TRUE;
                case "false" -> isSuccess = Boolean.FALSE;
                default -> throw new IllegalArgumentException("isSuccess must be either true or false.");
            }
            return Either.right(new omegapoint.opera.transactionlog.domain.model.entity.CloseTransaction(
                    closeTransaction.id,
                    LocalDateTime.parse(closeTransaction.endTime),
                    new NumberOfRecords(Integer.parseInt(closeTransaction.numberOfRecords)),
                    new BlobPath(closeTransaction.blobPath),
                    isSuccess,
                    Reason.valueOf(closeTransaction.reason)
            ));
        } catch (IllegalArgumentException e) {
            return Either.left(RejectMessage.of400("Unable to turn the given Json into a domain object " + e.getMessage()));
            // TODO: 2024-02-20 [tw] can we figure out on more fine detail what went wrong, maybe distinguish Value obeject creation errors from others.
        } catch (DateTimeParseException e) {
            return Either.left(RejectMessage.of400("Date unable to be parsed"));
        } catch (Exception e) {
            return Either.left(RejectMessage.of400("Something else went wrong" + e.getMessage()));
        }
    }

    public static Either<RejectMessage, CloseTransaction> deserialize(final String jsonString) {
        notNull(jsonString);
        try {
            return Either.right(JacksonConfig.objectMapper().readValue(jsonString, CloseTransaction.class));
        } catch (JsonProcessingException e) {
            if (e.getCause() instanceof DateTimeParseException parseException) {
                return Either.left(RejectMessage.of400(
                        "%s is not a date time with a time zone. Use java.time.Instant or java.time.ZonedDateTime."
                                .formatted(parseException.getParsedString())));
            } else if (e.getCause() instanceof NullPointerException) {
                return Either.left(RejectMessage.of400("Some field is null: "));
            }
            return Either.left(RejectMessage.of400(e.getMessage()));
        }
    }

}
