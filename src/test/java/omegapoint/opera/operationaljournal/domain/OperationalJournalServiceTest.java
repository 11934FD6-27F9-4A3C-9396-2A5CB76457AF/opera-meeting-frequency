package omegapoint.opera.operationaljournal.domain;

import com.microsoft.azure.functions.HttpStatus;
import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.api.model.request.Checkpoint;
import omegapoint.opera.operationaljournal.api.model.request.StartBody;
import omegapoint.opera.operationaljournal.api.model.request.StartRequest;
import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import omegapoint.opera.operationaljournal.domain.model.CheckpointType;
import omegapoint.opera.operationaljournal.domain.model.Status;
import omegapoint.opera.operationaljournal.domain.model.table.ConflictItem;
import omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;
import omegapoint.opera.operationaljournal.domain.model.table.Rerun;
import omegapoint.opera.operationaljournal.domain.repository.DatabaseRepository;
import omegapoint.opera.operationaljournal.domain.repository.IntegrationTests;
import omegapoint.opera.operationaljournal.domain.repository.SlackService;
import omegapoint.opera.operationaljournal.infrastructure.model.RunItem;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OperationalJournalServiceTest {

    private OperationalJournalService operationalJournalService;
    private DatabaseRepository databaseRepository = mock(DatabaseRepository.class);
    private Logger logger = mock(Logger.class);
    private SlackService slackService;
    private String environment;

    public void givenService(final Boolean shouldBeProd) {
        slackService = mock(SlackService.class);
        environment = shouldBeProd ? "prod" : "dev";

        this.operationalJournalService = new OperationalJournalService(logger, databaseRepository, null, slackService, environment);
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        if (testInfo.getTags().contains("SkipCleanup")) {
            return;
        }
        verifyNoMoreInteractions(logger);
        verifyNoMoreInteractions(databaseRepository);
    }

    @Test
    void given_dev_should_not_send_alert() {
        givenService(false);
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.sendAlertMessage();

        assertEquals(RejectMessage.of("Not running in dev environment"),result.getLeft());
    }

    @Test
    void given_no_data_should_send_info_alert() {

        final List<JournalErrorItem> infoAlert = List.of(new JournalErrorItem("N/A",
                0,
                "N/A",
                "N/A",
                "No alerts found today!",
                "N/A",
                "N/A",
                "N/A"));
        givenService(true);
        givenDataFromJournalIsEmpty();
        givenAlertResultFromSlack();
        operationalJournalService.sendAlertMessage();

        verify(logger).info(anyString());
        verify(databaseRepository).getRunsWithExhaustedRetriesFromLast24Hours();
        verify(slackService).sendAlert(infoAlert);

    }
    
    @Test
    void given_data_should_send_alert() {
        givenService(true);
        givenDataFromJournal();
        givenAlertResultFromSlack();
        operationalJournalService.sendAlertMessage();
        
        verify(logger).info(anyString());
        verify(databaseRepository).getRunsWithExhaustedRetriesFromLast24Hours();
        verify(slackService).sendAlert(List.of(new JournalErrorItem("1", 1, LocalDateTime.of(2023,1,1,1,1,1).toString(), "ERROR", "message", "origin", "stpe", "2"))
        );

    }

    private void givenAlertResultFromSlack() {
        when(slackService.sendAlert(any())).thenReturn(Either.right(SuccessMessage.of("alert")));
    }

    private void givenDataFromJournal() {
        when(databaseRepository.getRunsWithExhaustedRetriesFromLast24Hours()).thenReturn(
                Either.right(List.of(new JournalErrorItem("1", 1, LocalDateTime.of(2023,1,1,1,1,1).toString(), "ERROR", "message", "origin", "stpe", "2"))
                ));
    }

    private void givenDataFromJournalIsEmpty() {
        when(databaseRepository.getRunsWithExhaustedRetriesFromLast24Hours()).thenReturn(
                Either.right(List.of()));
    }

    @Test
    void start_with_success() {
        givenService(true);
        when(databaseRepository.putStart(any()))
                .thenReturn(Either.right(SuccessMessage.of("Rows successfully inserted")));

        final StartRequest request = getStartRequest();
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.start(request);

        assertTrue(result.isRight());
        assertEquals(SuccessMessage.of("Rows successfully inserted"), result.get());

        verify(databaseRepository).putStart(request);
        verify(logger).info("Successfully wrote start to journal.");
    }

    @Test
    void start_with_existing_run() {
        givenService(true);
        when(databaseRepository.putStart(any()))
                .thenReturn(Either.left(RejectMessage.of500("SQL broke :^(")));

        final StartRequest request = getStartRequest();
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.start(request);

        assertTrue(result.isLeft());
        assertEquals("SQL broke :^(", result.getLeft().message());

        verify(databaseRepository).putStart(request);
        verify(logger).warning("Could not write start to journal.");
    }

    @Test
    void stop_with_success() {
        givenService(true);
        when(databaseRepository.putStop(any()))
                .thenReturn(Either.right(SuccessMessage.of("Rows successfully inserted")));

        final JournalItem journalItem = getJournalItem();
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.stop(journalItem);

        assertTrue(result.isRight());
        assertEquals("Rows successfully inserted", result.get().message());

        verify(logger).info("Rows successfully inserted");
        verify(databaseRepository).putStop(journalItem);
    }

    @Test
    @Tag("SkipCleanup")
    void stop_as_integration_test_happypath() {
        var mockIntegrationTests = mock(IntegrationTests.class);
        this.operationalJournalService = new OperationalJournalService(logger, databaseRepository, mockIntegrationTests, slackService, "dev");

        when(mockIntegrationTests.isIntegrationTest(any()))
                .thenReturn(Either.right(true));
        when(mockIntegrationTests.sendError(any(), any(), any()))
                .thenReturn(Either.right(null));
        when(databaseRepository.getRootRunId(any()))
                .thenReturn(Either.right(Optional.of(UUID.randomUUID())));
        when(databaseRepository.getWebhookStep(any()))
                .thenReturn(Either.right("webhookStep"));
        when(databaseRepository.putStop(any()))
                .thenReturn(Either.right(SuccessMessage.of("Rows successfully inserted")));

        final JournalItem journalItem = new JournalItem(
                UUID.fromString("05dcd415-9de4-430f-95e7-1867a82e43f5").toString(),
                2,
                Instant.now(),
                Status.ERROR,
                "message",
                "webhook-handler-deserializer-orchestrator"
        );
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.stop(journalItem);

        verify(mockIntegrationTests).sendError(any(), any(), any());
    }

    @Test
    @Tag("SkipCleanup")
    void stop_as_not_integration_test_happypath() {
        var mockIntegrationTests = mock(IntegrationTests.class);
        this.operationalJournalService = new OperationalJournalService(logger, databaseRepository, mockIntegrationTests, slackService, "dev");

        when(mockIntegrationTests.isIntegrationTest(any()))
                .thenReturn(Either.right(false));
        when(databaseRepository.getRootRunId(any()))
                .thenReturn(Either.right(Optional.of(UUID.randomUUID())));
        when(databaseRepository.putStop(any()))
                .thenReturn(Either.right(SuccessMessage.of("Rows successfully inserted")));

        final JournalItem journalItem = new JournalItem(
                UUID.fromString("05dcd415-9de4-430f-95e7-1867a82e43f5").toString(),
                2,
                Instant.now(),
                Status.ERROR,
                "message",
                "webhook-handler-deserializer-orchestrator"
        );
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.stop(journalItem);

        verify(mockIntegrationTests, never()).sendError(any(), any(), any());
    }

    @Test
    @Tag("SkipCleanup")
    void stop_as_integration_test_successfull_stop_should_not_be_reported() {
        var mockIntegrationTests = mock(IntegrationTests.class);
        this.operationalJournalService = new OperationalJournalService(logger, databaseRepository, mockIntegrationTests, slackService, "dev");

        when(databaseRepository.putStop(any()))
                .thenReturn(Either.right(SuccessMessage.of("Rows successfully inserted")));

        final JournalItem journalItem = new JournalItem(
                UUID.fromString("05dcd415-9de4-430f-95e7-1867a82e43f5").toString(),
                2,
                Instant.now(),
                Status.SUCCESS,
                "message",
                "webhook-handler-deserializer-orchestrator"
        );
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.stop(journalItem);

        verify(mockIntegrationTests, never()).isIntegrationTest(any());
        verify(mockIntegrationTests, never()).sendError(any(), any(), any());
    }

    @Test
    void stop_with_failure() {
        givenService(true);
        when(databaseRepository.putStop(any()))
                .thenReturn(Either.left(RejectMessage.of500("Something broke!")));

        final JournalItem journalItem = getJournalItem();
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.stop(journalItem);

        assertTrue(result.isLeft());
        assertEquals("Something broke!", result.getLeft().message());

        verify(logger).warning("Something broke!");
        verify(databaseRepository).putStop(journalItem);
    }

    @Test
    void restart_with_success() {
        givenService(true);
        when(databaseRepository.putRestart(any()))
                .thenReturn(Either.right(SuccessMessage.of("Rows successfully inserted")));

        final JournalItem journalItem = getJournalItem();
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.restart(journalItem);

        assertTrue(result.isRight());
        assertEquals("Rows successfully inserted", result.get().message());

        verify(logger).info("Rows successfully inserted");
        verify(databaseRepository).putRestart(journalItem);
    }

    @Test
    void restart_with_failure() {
        givenService(true);
        when(databaseRepository.putRestart(any()))
                .thenReturn(Either.left(RejectMessage.of500("Something broke!")));

        final JournalItem journalItem = getJournalItem();
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.restart(journalItem);

        assertTrue(result.isLeft());
        assertEquals("Something broke!", result.getLeft().message());

        verify(logger).warning("Something broke!");
        verify(databaseRepository).putRestart(journalItem);
    }

    @Test
    void conflict_with_success() {
        givenService(true);
        when(databaseRepository.putConflict(any()))
                .thenReturn(Either.right(SuccessMessage.of("Rows successfully inserted")));

        final ConflictItem conflictItem = new ConflictItem(UUID.randomUUID().toString(), "conflict.id");
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.conflict(conflictItem);

        assertTrue(result.isRight());
        assertEquals("Rows successfully inserted", result.get().message());

        verify(logger).info("Rows successfully inserted");
        verify(databaseRepository).putConflict(conflictItem);
    }

    @Test
    void conflict_with_failure() {
        givenService(true);
        when(databaseRepository.putConflict(any()))
                .thenReturn(Either.left(RejectMessage.of500("Something broke!")));

        final ConflictItem conflictItem = new ConflictItem(UUID.randomUUID().toString(), "conflict.id");
        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.conflict(conflictItem);

        assertTrue(result.isLeft());
        assertEquals("Something broke!", result.getLeft().message());

        verify(logger).warning("Something broke!");
        verify(databaseRepository).putConflict(conflictItem);
    }

    @Test
    void getRerunInfo_with_success() {
        givenService(true);

        final Rerun rerun = getRerun();
        when(databaseRepository.getRerunInfo(any()))
                .thenReturn(Either.right(rerun));

        final Either<RejectMessage, Rerun> result = operationalJournalService.getRerunInfo(rerun.runId().toString());

        assertTrue(result.isRight());
        assertEquals(rerun, result.get());

        verify(logger).info("Successfully fetched rerun.");
        verify(databaseRepository).getRerunInfo(rerun.runId().toString());
    }

    @Test
    void getRerunInfo_with_failure() {
        givenService(true);
        when(databaseRepository.getRerunInfo(any()))
                .thenReturn(Either.left(RejectMessage.of500("Something broke!")));

        final Rerun rerun = getRerun();
        final Either<RejectMessage, Rerun> result = operationalJournalService.getRerunInfo(rerun.runId().toString());

        assertTrue(result.isLeft());
        assertEquals("Something broke!", result.getLeft().message());

        verify(logger).severe("Failed to access database: Something broke!");
        verify(databaseRepository).getRerunInfo(rerun.runId().toString());
    }

    @Test
    void getReruns_with_success() {
        givenService(true);
        final List<Rerun> reruns = List.of(
                new Rerun[]{
                        new Rerun(
                                UUID.randomUUID(),
                                2,
                                new omegapoint.opera.operationaljournal.domain.model.WebhookStep("update_cinode_team"),
                                ZonedDateTime.now(),
                                "QUEUE_NAME",
                                new BlobReference("containerName", "pa/th")
                        ),
                        new Rerun(
                                UUID.randomUUID(),
                                2,
                                new omegapoint.opera.operationaljournal.domain.model.WebhookStep("update_cinode_team"),
                                ZonedDateTime.now(),
                                "QUEUE_NAME",
                                new BlobReference("containerName", "pa/th")
                        )});
        when(databaseRepository.getReruns())
                .thenReturn(Either.right(reruns));

        final Either<RejectMessage, List<Rerun>> result = operationalJournalService.getReruns();

        assertTrue(result.isRight());
        assertEquals(reruns, result.get());

        verify(logger).info("Successfully fetched reruns.");
        verify(databaseRepository).getReruns();
    }

    @Test
    void getReruns_with_failure() {
        givenService(true);
        when(databaseRepository.getReruns())
                .thenReturn(Either.left(RejectMessage.of500("Something broke!")));

        final Either<RejectMessage, List<Rerun>> result = operationalJournalService.getReruns();

        assertTrue(result.isLeft());
        assertEquals("Something broke!", result.getLeft().message());

        verify(logger).severe("Something broke!");
        verify(databaseRepository).getReruns();
    }

    @Test
    void getCinodeReruns_with_success() {
        givenService(true);
        when(databaseRepository.getCinodeReruns())
                .thenReturn(Either.right(getReruns()));

        final Either<RejectMessage, List<omegapoint.opera.operationaljournal.domain.model.table.Rerun>> result = operationalJournalService.getCinodeReruns();

        assertTrue(result.isRight());
        final List<Rerun> expected = Arrays.stream(getReruns()).map(omegapoint.opera.operationaljournal.infrastructure.model.Rerun::toDomain).toList();
        assertEquals(expected, result.get());

        verify(databaseRepository).getCinodeReruns();
    }

    @Test
    void getCinodeReruns_failure() {
        givenService(true);
        when(databaseRepository.getCinodeReruns())
                .thenReturn(Either.left(RejectMessage.of("Error", HttpStatus.BAD_REQUEST)));

        final Either<RejectMessage, List<omegapoint.opera.operationaljournal.domain.model.table.Rerun>> result = operationalJournalService.getCinodeReruns();

        assertTrue(result.isLeft());
        assertEquals(RejectMessage.of("Error", HttpStatus.BAD_REQUEST), result.getLeft());

        verify(databaseRepository).getCinodeReruns();
        verify(logger).severe("Error");
    }

    @Test
    void getRun_with_success() {
        givenService(true);
        when(databaseRepository.getExistingRun(anyString()))
                .thenReturn(Either.right(new RunItem[]{getRunItem()}));

        final Either<RejectMessage, RunItem> result = operationalJournalService.getRun("someRunId");

        assertTrue(result.isRight());
        assertEquals(getRunItem(), result.get());

        verify(databaseRepository).getExistingRun("someRunId");
    }

    @Test
    void getRun_failure_with_zero_RunItem() {
        givenService(true);
        when(databaseRepository.getExistingRun(anyString()))
                .thenReturn(Either.right(new RunItem[]{}));

        final Either<RejectMessage, RunItem> result = operationalJournalService.getRun("someRunId");

        assertTrue(result.isLeft());
        final RejectMessage expected = RejectMessage.of404("The journal does not contain any Run with the RunID 'someRunId'.");
        assertEquals(expected, result.getLeft());

        verify(databaseRepository).getExistingRun("someRunId");
        verify(logger).severe("The journal does not contain any Run with the RunID 'someRunId'.");
    }

    @Test
    void getRun_with_failure() {
        givenService(true);
        when(databaseRepository.getExistingRun(anyString()))
                .thenReturn(Either.left(RejectMessage.of500("Error")));

        final Either<RejectMessage, RunItem> result = operationalJournalService.getRun("someRunId");

        assertTrue(result.isLeft());
        assertEquals("Error", result.getLeft().message());

        verify(databaseRepository).getExistingRun("someRunId");
        verify(logger).severe("Error");
    }


    @Test
    void restrictRerun_with_success() {
        givenService(true);
        when(databaseRepository.restrictRerun(any(JournalItem.class)))
                .thenReturn(Either.right(SuccessMessage.of("Success")));

        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.restrictRerun("05dcd415-9de4-430f-95e7-1867a82e43f5");

        assertTrue(result.isRight());
        assertEquals(SuccessMessage.of("Success"), result.get());

        verifyJournalItem();
        verify(logger).info("Success");
    }

    @Test
    void restrictRerun_with_failure() {
        givenService(true);
        when(databaseRepository.restrictRerun(any(JournalItem.class)))
                .thenReturn(Either.left(RejectMessage.of404("Error")));

        final Either<RejectMessage, SuccessMessage> result = operationalJournalService.restrictRerun("05dcd415-9de4-430f-95e7-1867a82e43f5");

        assertTrue(result.isLeft());
        assertEquals("Error", result.getLeft().message());

        verifyJournalItem();

        verify(logger).warning("Error");
    }

    private omegapoint.opera.operationaljournal.infrastructure.model.Rerun[] getReruns() {
        return new omegapoint.opera.operationaljournal.infrastructure.model.Rerun[]{new omegapoint.opera.operationaljournal.infrastructure.model.Rerun(
                "05dcd415-9de4-430f-95e7-1867a82e43f5",
                1,
                "PREPROCESS_CINODE_ACTION",
                "2024-08-06T12:34:56Z",
                "exampleQueue",
                "exampleContainer",
                "exampleBlobPath"
        )};
    }

    private RunItem getRunItem() {
        return new RunItem("05dcd415-9de4-430f-95e7-1867a82e43f5",
                "parentID",
                "2024-08-06T12:34:56Z",
                "PREPROCESS_CINODE_ACTION",
                "conflictID");
    }

    private StartRequest getStartRequest() {
        return new StartRequest("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42",
                new StartBody(UUID.fromString("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42"),
                        new BlobReference("", ""),
                        "update_cinode_team",
                        ZonedDateTime.now().toString(),
                        ZonedDateTime.now().toString(),
                        "",
                        "",
                        new Checkpoint("", CheckpointType.QUEUE.toString()),
                        "",
                        null));
    }

    private JournalItem getJournalItem() {
        return new JournalItem(
                UUID.fromString("05dcd415-9de4-430f-95e7-1867a82e43f5").toString(),
                2,
                Instant.now(),
                Status.SUCCESS,
                "message",
                "webhook-handler-deserializer-orchestrator"
        );
    }

    private void verifyJournalItem() {
        final ArgumentCaptor<JournalItem> journalItemCaptor = ArgumentCaptor.forClass(JournalItem.class);
        verify(databaseRepository).restrictRerun(journalItemCaptor.capture());
        final JournalItem journalItemCaptorValue = journalItemCaptor.getValue();
        assertEquals("05dcd415-9de4-430f-95e7-1867a82e43f5", journalItemCaptorValue.runID);
        assertNull(journalItemCaptorValue.attempt);
        assertTrue(journalItemCaptorValue.timestamp.toString().startsWith(LocalDate.now().toString()));
        assertEquals("RESTRICTED", journalItemCaptorValue.status.toString());
        assertNull(journalItemCaptorValue.message);
        assertNull(journalItemCaptorValue.originFunction);
    }

    private Rerun getRerun() {
        return new Rerun(
                UUID.randomUUID(),
                2,
                new omegapoint.opera.operationaljournal.domain.model.WebhookStep("update_cinode_team"),
                ZonedDateTime.now(),
                "QUEUE_NAME",
                new BlobReference("containerName", "pa/th")
        );
    }
}