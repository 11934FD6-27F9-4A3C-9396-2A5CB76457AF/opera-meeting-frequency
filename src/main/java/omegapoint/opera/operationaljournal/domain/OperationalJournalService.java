package omegapoint.opera.operationaljournal.domain;

import io.vavr.control.Either;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.domain.model.StartItem;
import omegapoint.opera.operationaljournal.domain.model.Status;
import omegapoint.opera.operationaljournal.domain.model.table.ConflictItem;
import omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;
import omegapoint.opera.operationaljournal.domain.repository.DatabaseRepository;
import omegapoint.opera.operationaljournal.domain.repository.IntegrationTests;
import omegapoint.opera.operationaljournal.domain.repository.SlackService;
import omegapoint.opera.operationaljournal.infrastructure.model.Rerun;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;

public class OperationalJournalService {
    private final Logger logger;
    private final DatabaseRepository databaseRepository;
    private final IntegrationTests integrationTests;
    private final SlackService slackService;
    private final String environment;

    public OperationalJournalService(final Logger logger,
                                     final DatabaseRepository databaseRepository,
                                     final IntegrationTests integrationTests,
                                     final SlackService slackService,
                                     final String environment) {
        this.logger = notNull(logger);
        this.databaseRepository = notNull(databaseRepository);
        this.integrationTests = integrationTests;
        this.slackService = slackService;
        this.environment = notNull(environment);
    }

    public Either<RejectMessage, SuccessMessage> start(StartItem startItem) {
        return databaseRepository.putStart(startItem)
                .peekLeft(rejectMessage -> logger.warning("Could not write start to journal."))
                .peek(success -> logger.info("Successfully wrote start to journal."));
    }

    public Either<RejectMessage, SuccessMessage> stop(@NonNull JournalItem journalItem) {
        // TODO: 2024-05-30 [mbloms, ls] Starta rerun service efter att vi har utfört en /stop.
        //                               Använd QueueTrigger härifrån eller en SqlTrigger.
        if (integrationTests != null && journalItem.status.equals(Status.ERROR)) {
            getRootRunId(UUID.fromString(journalItem.runID))
                    .map(rootRunIdOptional -> rootRunIdOptional
                            .map(rootRunId -> integrationTests.isIntegrationTest(rootRunId)
                                    .map(isTest -> {
                                        if (isTest) return getWebhookStep(UUID.fromString(journalItem.runID))
                                                .map(webhookStep -> integrationTests.sendError(rootRunId, journalItem, webhookStep)
                                                        .peekLeft(rejectMessage -> logger.severe("Could not send error to integration tests")));
                                        else return null;
                                    })));
        }
        return databaseRepository.putStop(journalItem)
                .peek(success -> logger.info(success.message()))
                .peekLeft(reject -> logger.warning(reject.message()));
    }

    public Either<RejectMessage, SuccessMessage> restart(@NonNull JournalItem journalItem) {
        return databaseRepository.putRestart(journalItem)
                .peek(success -> logger.info(success.message()))
                .peekLeft(reject -> logger.warning(reject.message()));
    }

    public Either<RejectMessage, SuccessMessage> conflict(ConflictItem conflictItem) {
        /* TODO: 2024-05-30 [mbloms, ls]
                 Feature: Efter att ha lagt in konflikten i databasen, kolla efter
                 existerande konflikter och returnera 4xx "Stalled" om körningen
                 borde vänta tills något annat kört klart */
        return databaseRepository.putConflict(conflictItem)
                .peek(success -> logger.info(success.message()))
                .peekLeft(reject -> logger.warning(reject.message()));
    }

    public Either<RejectMessage, omegapoint.opera.operationaljournal.domain.model.table.Rerun> getRerunInfo(final @NonNull String runId) {
        return databaseRepository.getRerunInfo(runId)
                .peekLeft(rejectMessage -> logger.severe("Failed to access database: " + rejectMessage.message()))
                .peek(rerun -> logger.info("Successfully fetched rerun."))
                .fold(
                        Either::left,
                        Either::right
                );
    }

    public Either<RejectMessage, List<omegapoint.opera.operationaljournal.domain.model.table.Rerun>> getReruns() {
        return databaseRepository.getReruns()
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()))
                .peek(rerun -> logger.info("Successfully fetched reruns."))
                .fold(
                        Either::left,
                        Either::right
                );
    }

    /**
     * Gets all Cinode Runs that should rerun, ignoring the attempt column.
     */
    public Either<RejectMessage, List<omegapoint.opera.operationaljournal.domain.model.table.Rerun>> getCinodeReruns() {
        // TODO: 2024-05-30 [mbloms, ls] Se ovan.
        // TODO: 2024-05-30 [mbloms, ls] Generalisera getReruns()?
        return databaseRepository.getCinodeReruns()
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()))
                .map(List::of)
                .map(reruns -> reruns.stream().map(Rerun::toDomain).collect(Collectors.toList()));
    }

    // Currently not used by any other services
    public Either<RejectMessage, omegapoint.opera.operationaljournal.infrastructure.model.RunItem> getRun(final @NonNull String runId) {
        return databaseRepository.getExistingRun(runId)
                .flatMap(runItems -> {
                    if (runItems.length == 0) {
                        return Either.left(RejectMessage.of404("The journal does not contain any Run with the RunID '%s'.".formatted(runId)));
                    } else {
                        return Either.right(runItems[0]);
                    }
                })
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()));
    }

    public Either<RejectMessage, SuccessMessage> restrictRerun(final @NonNull String runId) {
        return databaseRepository.restrictRerun(new JournalItem(runId, null, Instant.now(), Status.RESTRICTED, null, null))
                .peek(success -> logger.info(success.message()))
                .peekLeft(reject -> logger.warning(reject.message()));
    }

    public Either<RejectMessage, Optional<UUID>> getRootRunId(final @NonNull UUID runId) {
        return databaseRepository.getRootRunId(runId)
                .peek(success ->
                        success.ifPresentOrElse(uuid -> logger.info("Found root run id: " + uuid),
                                () -> logger.info("Root run id not found."))
                )
                .peekLeft(reject -> logger.warning(reject.message()));
    }

    private Either<RejectMessage, String> getWebhookStep(final @NonNull UUID runId) {
        return databaseRepository.getWebhookStep(runId)
                .peek(webhookStep -> logger.info("Found webhook step: " + webhookStep))
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()));
    }

    public Either<RejectMessage, String> getParentWebhookStep(final @NonNull UUID runId) {
        return databaseRepository.getParentWebhookStep(runId)
                .peek(webhookStep -> logger.info("Found parent webhook step: " + webhookStep))
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()));
    }

    public Either<RejectMessage, SuccessMessage> sendAlertMessage() {

        if (environment.equals("prod")) {

            final List<JournalErrorItem> infoAlert = List.of(new JournalErrorItem("N/A",
                    0,
                    "N/A",
                    "N/A",
                    "No alerts found today!",
                    "N/A",
                    "N/A",
                    "N/A"));

            return databaseRepository.getRunsWithExhaustedRetriesFromLast24Hours()
                    .flatMap(journalErrorItems -> {
                        Either<RejectMessage, SuccessMessage> alertResult = slackService.sendAlert(
                                journalErrorItems == null || journalErrorItems.isEmpty() ?
                                        infoAlert : journalErrorItems
                        );
                        alertResult.peek(successMessage -> logger.info(successMessage.message()))
                                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()));
                        return alertResult;
                    })
                    .peekLeft(rejectMessage -> logger.severe("Database fetch failed: " + rejectMessage.message()));
        } else {
            return Either.left(RejectMessage.of("Not running in dev environment"));

        }
    }


}
