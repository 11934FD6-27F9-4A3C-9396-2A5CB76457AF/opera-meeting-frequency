package omegapoint.opera.operationaljournal.infrastructure;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableServiceException;
import com.azure.storage.queue.QueueClient;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;
import omegapoint.opera.operationaljournal.domain.repository.IntegrationTests;
import omegapoint.opera.operationaljournal.infrastructure.model.integrationtests.TestResult;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@AllArgsConstructor
public class IntegrationTestService implements IntegrationTests {

    private final @NonNull TableClient testsTableClient;
    private final @NonNull QueueClient testCrashQueueClient;
    private final @NonNull Logger logger;

    @Override
    public Either<RejectMessage, Boolean> isIntegrationTest(final @NonNull UUID rootRunId) {
        return Try.of(() -> testsTableClient.getEntity(rootRunId.toString(), rootRunId.toString()))
                .map(testExists -> Either.<RejectMessage, Boolean>right(true))
                .getOrElseGet(throwable -> {
                    if (throwable instanceof TableServiceException tableServiceException) {
                        if (tableServiceException.getResponse().getStatusCode() == 404) {
                            return Either.right(false);
                        }
                    }
                    return Either.left(RejectMessage.of500(throwable.getMessage()));
                });
    }

    @Override
    public Either<RejectMessage, Void> sendError(final @NonNull UUID rootRunId,
                                                 final @NonNull JournalItem journalItem,
                                                 final @NonNull String webhookStep) {
        TestResult testResult = TestResult.fromJournalItem(rootRunId, journalItem, webhookStep);
        logger.info("Sending testresult for: " + testResult.testRunId());
        return Try.of(() -> {
                    testCrashQueueClient.sendMessage(testResult.toBinaryData());
                    return Either.<RejectMessage, Void>right(null);
                })
                .getOrElseGet(throwable -> Either.left(RejectMessage.of500(throwable.getMessage())));
    }
}
