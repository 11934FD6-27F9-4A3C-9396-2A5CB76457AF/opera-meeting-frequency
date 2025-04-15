package omegapoint.opera.operationaljournal.infrastructure.model.integrationtests;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import omegapoint.opera.operationaljournal.config.JacksonConfig;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;

import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TestResult {

    private final @JsonProperty @Getter @Accessors(fluent = true) UUID testRunId;
    private final @JsonProperty String errorMessage;
    private final @JsonProperty String originFunction;
    private final @JsonProperty String webhookStep;
    private final @JsonProperty String runId;

    public static TestResult fromJournalItem(final @NonNull UUID rootRunId,
                                             final @NonNull JournalItem journalItem,
                                             final @NonNull String webhookStep) {
        return new TestResult(
                rootRunId,
                journalItem.message,
                journalItem.originFunction,
                webhookStep,
                journalItem.runID
        );
    }

    public BinaryData toBinaryData() {
        return Try.of(() -> JacksonConfig.objectMapper().writeValueAsString(this))
                .map(BinaryData::fromString)
                .get();
    }
}
