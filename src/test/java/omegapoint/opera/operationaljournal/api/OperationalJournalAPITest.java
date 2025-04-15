package omegapoint.opera.operationaljournal.api;

import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.api.model.response.RerunMessage;
import omegapoint.opera.operationaljournal.domain.OperationalJournalService;
import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import omegapoint.opera.operationaljournal.domain.model.WebhookStep;
import omegapoint.opera.operationaljournal.domain.model.table.Rerun;
import omegapoint.opera.operationaljournal.infrastructure.model.RunItem;
import omegapoint.opera.util.HttpResponseMessageMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OperationalJournalAPITest {


    OperationalJournalAPI operationalJournalAPI;
    HttpRequestMessage<Optional<String>> request;
    OperationalJournalService operationalJournalService;
    Logger logger;
    HttpResponseMessage.Builder mockBuilder;
    HttpResponseMessage mockResponse;

    @BeforeEach
    void setUp() {
        operationalJournalAPI = new OperationalJournalAPI();
        request = mock(HttpRequestMessage.class);
        operationalJournalService = mock(OperationalJournalService.class);
        logger = mock(Logger.class);
        mockBuilder = mock(HttpResponseMessage.Builder.class);
        mockResponse = mock(HttpResponseMessage.class);

        mockRequestResponseBuilder();

    }

    @Nested
    class handleStopRequest {
        @Test
        void handleStopRequest_happyCase() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":\"2024-02-14T13:01:22.519958Z\", \"status\":\"FINISHED\", \"message\":\"Successful webhook\"}"));
            when(operationalJournalService.stop(any())).thenReturn(Either.right(SuccessMessage.of("success:)")));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStopRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.OK, httpResponseMessage.getStatus());
            assertEquals("success:)", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleStopRequest_emptyBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.empty());

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStopRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("No body sent in request.", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleStopRequest_timeFormatInLocalDateTime_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":\"" + LocalDateTime.now() + "\", \"status\":\"FINISHED\", \"message\":\"Successful webhook\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStopRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body:"));
            assertTrue(httpResponseMessage.getBody().toString().endsWith("Use java.time.Instant or java.time.ZonedDateTime.\""));
        }

        @Test
        void handleStopRequest_badTimeFormat_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":\"dåligtid\", \"status\":\"FINISHED\", \"message\":\"Successful webhook\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStopRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body:"));
            assertTrue(httpResponseMessage.getBody().toString().endsWith("Use java.time.Instant or java.time.ZonedDateTime.\""));
        }

        @Test
        void handleStopRequest_nullFieldInBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":null, \"status\":\"FINISHED\", \"message\":\"Successful webhook\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStopRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body: \n\"Some field is null: "));
        }

        @Test
        void handleStopRequest_missingFieldInBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"status\":\"FINISHED\", \"message\":\"Successful webhook\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStopRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body: \n\"Some field is null: "));
        }

        @Test
        void handleStopRequest_wrongStatus_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":\"2024-02-14T13:01:22.519958Z\", \"status\":\"notastatus\", \"message\":\"Successful webhook\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStopRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("""
                    Expected status to be one of the following:
                    STARTED,
                    RESTARTED,
                    ERROR,
                    SUCCESS,
                    RESTRICTED""", httpResponseMessage.getBody().toString());
        }
    }

    @Nested
    class handleRestartRequest {
        @Test
        void handleRestartRequest_happyCase() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":\"2024-02-14T13:01:22.519958Z\", \"attempt\":2, \"originFunction\":\"origin\"}"));
            when(operationalJournalService.restart(any())).thenReturn(Either.right(SuccessMessage.of("success:)")));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRestartRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.OK, httpResponseMessage.getStatus());
            assertEquals("success:)", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleRestartRequest_emptyBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.empty());

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRestartRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("No body sent in request.", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleRestartRequest_timeFormatInLocalDateTime_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":\"" + LocalDateTime.now() + "\", \"attempt\":2, \"originFunction\":\"origin\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRestartRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body:"));
            assertTrue(httpResponseMessage.getBody().toString().endsWith("Use java.time.Instant or java.time.ZonedDateTime.\""));
        }

        @Test
        void handleRestartRequest_badTimeFormat_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":\"dåligtid\", \"attempt\":2, \"originFunction\":\"origin\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRestartRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body:"));
            assertTrue(httpResponseMessage.getBody().toString().endsWith("Use java.time.Instant or java.time.ZonedDateTime.\""));
        }

        @Test
        void handleRestartRequest_nullFieldInBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":null, \"attempt\":2, \"originFunction\":\"origin\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRestartRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body: \n\"Some field is null: "));
        }

        @Test
        void handleRestartRequest_missingFieldInBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"attempt\":2, \"originFunction\":\"origin\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRestartRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body: \n\"Some field is null: "));
        }

        @Test
        void handleRestartRequest_illegalAttempt_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"timestamp\":\"2024-02-14T13:01:22.519958Z\", \"attempt\":-1, \"originFunction\":\"origin\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRestartRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("""
                    Incorrect format of request body:\s
                    "Cannot construct instance of `omegapoint.opera.operationaljournal.api.model.request.RestartBody`, problem: Attempt must be greater than 1. A restart can't be the first attempt.
                     at [Source: (String)"{"timestamp":"2024-02-14T13:01:22.519958Z", "attempt":-1, "originFunction":"origin"}"; line: 1, column: 84]\"""",
                    httpResponseMessage.getBody().toString());
        }
    }

    @Nested
    class handleStartRequest {
        @Test
        void handleStartRequest_happyCase() {
            String runId = "123";
            mockRequestBody(Optional.of("""
                    {
                        "parentRunId" : "f17a4df2-52dd-4b8b-be96-875688d1ca40",
                        "blobReference": {"path": "/sage/request.json", "containerName": "webhook-inbox"},
                        "flow" : "SAGE_TEAM_NOTIFICATIONS",
                        "originTimestamp": "2024-02-14T13:01:22.519958Z",
                        "runStartTime": "2024-02-14T13:01:22.519958Z",
                        "checkpoint" : {
                          "checkpointPath" : "team-queue",
                          "checkpointType" : "QUEUE"
                        },
                        "entityId" : "sage",
                        "operationType": "LIVE_UPDATE"
                    }
                    """));
            when(operationalJournalService.start(any())).thenReturn(Either.right(SuccessMessage.of("success:)")));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStartRequest(request, runId, logger, operationalJournalService)
                    .fold(success -> success, error -> error);

            System.err.println(httpResponseMessage.getBody());

            assertEquals(HttpStatus.OK, httpResponseMessage.getStatus());
            assertEquals("success:)", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleStartRequest_emptyBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.empty());

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStartRequest(request, runId, logger, operationalJournalService)
                    .fold(success -> success, error -> error);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("No body sent in request.", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleStartRequest_timeFormatInLocalDateTime_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of(
                    """
                    {
                        "parentRunId" : "f17a4df2-52dd-4b8b-be96-875688d1ca40",
                        "blobReference": {"path": "/sage/request.json", "containerName": "webhook-inbox"},
                        "flow" : "SAGE_TEAM_NOTIFICATIONS",
                        "originTimestamp": "%s",
                        "runStartTime": "%s",
                        "checkpoint" : {
                          "checkpointPath" : "team-queue",
                          "checkpointType" : "QUEUE"
                        },
                        "entityId" : "sage",
                        "operationType": "LIVE_UPDATE"
                    }
                    """.formatted(LocalDateTime.now(), LocalDateTime.now())));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStartRequest(request, runId, logger, operationalJournalService)
                    .fold(success -> success, error -> error);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body:"));
            assertTrue(httpResponseMessage.getBody().toString().endsWith("Use java.time.Instant or java.time.ZonedDateTime.\""));
        }

        @Test
        void handleStartRequest_badTimeFormat_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of(
                    """
                    {
                        "parentRunId" : "f17a4df2-52dd-4b8b-be96-875688d1ca40",
                        "blobReference": {"path": "/sage/request.json", "containerName": "webhook-inbox"},
                        "flow" : "SAGE_TEAM_NOTIFICATIONS",
                        "originTimestamp": "dåligtid",
                        "runStartTime": "dåligtid",
                        "checkpoint" : {
                          "checkpointPath" : "team-queue",
                          "checkpointType" : "QUEUE"
                        },
                        "entityId" : "sage",
                        "operationType": "LIVE_UPDATE"
                    }
                    """));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStartRequest(request, runId, logger, operationalJournalService)
                    .fold(success -> success, error -> error);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body:"));
            assertTrue(httpResponseMessage.getBody().toString().endsWith("Use java.time.Instant or java.time.ZonedDateTime.\""));
        }

        @Test
        void handleStartRequest_nullFieldInBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of(
                    """
                    {
                        "parentRunId" : "f17a4df2-52dd-4b8b-be96-875688d1ca40",
                        "blobReference": {"path": "/sage/request.json", "containerName": "webhook-inbox"},
                        "flow" : "SAGE_TEAM_NOTIFICATIONS",
                        "originTimestamp": null,
                        "runStartTime": null,
                        "checkpoint" : {
                          "checkpointPath" : "team-queue",
                          "checkpointType" : "QUEUE"
                        },
                        "entityId" : "sage",
                        "operationType": "LIVE_UPDATE"
                    }
                    """));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStartRequest(request, runId, logger, operationalJournalService)
                    .fold(success -> success, error -> error);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            System.err.println(httpResponseMessage.getBody().toString());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body: \n\"Some field is null: "));
        }

        @Test
        void handleStartRequest_missingFieldInBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"status\":\"FINISHED\", \"message\":\"Successful webhook\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleStartRequest(request, runId, logger, operationalJournalService)
                    .fold(success -> success, error -> error);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body: \n\"Some field is null: "));
        }
    }

    @Nested
    class handleConflictRequest {
        @Test
        void handleConflictRequest_happyCase() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"conflictID\":\"conflict.id\"}"));
            when(operationalJournalService.conflict(any())).thenReturn(Either.right(SuccessMessage.of("success:)")));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleConflictRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.OK, httpResponseMessage.getStatus());
            assertEquals("success:)", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleConflictRequest_emptyBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.empty());

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleConflictRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("No body sent in request.", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleConflictRequest_nullFieldInBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"conflictID\":null}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleConflictRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body: \n\"Some field is null."));
        }

        @Test
        void handleConflictRequest_missingFieldInBody_returnReject() {
            String runId = "123";
            mockRequestBody(Optional.of("{\"test\":\"test\"}"));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleConflictRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertTrue(httpResponseMessage.getBody().toString().startsWith("Incorrect format of request body: \n\"Some field is null."));
        }
    }

    @Nested
    class handleRerunsRequest {
        @Test
        void handleRerunsRequest_happyCase() {
            when(operationalJournalService.getReruns())
                    .thenReturn(Either.right(List.of(new Rerun(
                            UUID.randomUUID(),
                            2,
                            new WebhookStep("update_cinode_team"),
                            ZonedDateTime.now(),
                            "queue-name",
                            new BlobReference("containerName", "pa/th")
                    ))));
            when(request.createResponseBuilder(HttpStatus.OK))
                    .thenReturn(mockBuilder);
            when(mockBuilder.header(any(), any()))
                    .thenReturn(mockBuilder);
            when(mockBuilder.body(any()))
                    .thenReturn(mockBuilder);
            when(mockBuilder.build())
                    .thenReturn(mockResponse);

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRerunsRequest(request, logger, operationalJournalService);

            assertEquals(mockResponse, httpResponseMessage);
        }

        @Test
        void handleRerunsRequest_sadCase() {
            when(operationalJournalService.getReruns())
                    .thenReturn(Either.left(RejectMessage.of500("rejection")));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRerunsRequest(request, logger, operationalJournalService);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpResponseMessage.getStatus());
            assertEquals("rejection", httpResponseMessage.getBody().toString());
        }
    }

    @Nested
    class handleRerunInfoRequest {
        @Test
        void handleRerunInfoRequest_happyCase() {
            String runId = UUID.randomUUID().toString();
            Rerun rerun = new Rerun(
                    UUID.fromString(runId),
                    2,
                    new WebhookStep("update_cinode_team"),
                    ZonedDateTime.now(),
                    "queue-name",
                    new BlobReference("containerName", "pa/th")
            );
            when(operationalJournalService.getRerunInfo(any()))
                    .thenReturn(Either.right(rerun));
            RerunMessage expected = RerunMessage.fromDomain(List.of(rerun));

            when(request.createResponseBuilder(HttpStatus.OK))
                    .thenReturn(mockBuilder);
            when(mockBuilder.header(any(), any()))
                    .thenReturn(mockBuilder);
            when(mockBuilder.body(expected))
                    .thenReturn(mockBuilder);
            when(mockBuilder.build())
                    .thenReturn(mockResponse);

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRerunInfoRequest(request, runId, logger, operationalJournalService);

            assertEquals(mockResponse, httpResponseMessage);
        }

        @Test
        void handleRerunInfoRequest_sadCase() {
            String runId = UUID.randomUUID().toString();
            when(operationalJournalService.getRerunInfo(any()))
                    .thenReturn(Either.left(RejectMessage.of500("rejection")));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRerunInfoRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpResponseMessage.getStatus());
            assertEquals("rejection", httpResponseMessage.getBody().toString());
        }
    }

    @Nested
    class handleCinodeRerunsRequest {
        @Test
        void handleCinodeRerunsRequest_happyCase() {
            when(operationalJournalService.getCinodeReruns())
                    .thenReturn(Either.right(List.of(new Rerun(
                            UUID.randomUUID(),
                            2,
                            new WebhookStep("update_cinode_team"),
                            ZonedDateTime.now(),
                            "queue-name",
                            new BlobReference("containerName", "pa/th")
                    ))));
            when(request.createResponseBuilder(HttpStatus.OK))
                    .thenReturn(mockBuilder);
            when(mockBuilder.header(any(), any()))
                    .thenReturn(mockBuilder);
            when(mockBuilder.body(any()))
                    .thenReturn(mockBuilder);
            when(mockBuilder.build())
                    .thenReturn(mockResponse);

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleCinodeRerunsRequest(request, logger, operationalJournalService);

            assertEquals(mockResponse, httpResponseMessage);
        }

        @Test
        void handleCinodeRerunsRequest_sadCase() {
            when(operationalJournalService.getCinodeReruns())
                    .thenReturn(Either.left(RejectMessage.of500("rejection")));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleCinodeRerunsRequest(request, logger, operationalJournalService);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpResponseMessage.getStatus());
            assertEquals("rejection", httpResponseMessage.getBody().toString());
        }
    }

    @Nested
    class handleRunRequest {
        @Test
        void handleRunRequest_happyCase() {
            String runId = UUID.randomUUID().toString();
            RunItem runItem = new RunItem(
                    runId,
                    UUID.randomUUID().toString(),
                    ZonedDateTime.now().toString(),
                    "webhook-step",
                    "conflict.id"
            );
            when(operationalJournalService.getRun(any()))
                    .thenReturn(Either.right(runItem));

            when(request.createResponseBuilder(HttpStatus.OK))
                    .thenReturn(mockBuilder);
            when(mockBuilder.header(any(), any()))
                    .thenReturn(mockBuilder);
            when(mockBuilder.body(runItem))
                    .thenReturn(mockBuilder);
            when(mockBuilder.build())
                    .thenReturn(mockResponse);

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRunRequest(request, runId, logger, operationalJournalService);

            assertEquals(mockResponse, httpResponseMessage);
        }

        @Test
        void handleRunRequest_sadCase() {
            String runId = UUID.randomUUID().toString();
            when(operationalJournalService.getRun(any()))
                    .thenReturn(Either.left(RejectMessage.of500("rejection")));

            final HttpResponseMessage httpResponseMessage = operationalJournalAPI.handleRunRequest(request, runId, logger, operationalJournalService);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpResponseMessage.getStatus());
            assertEquals("rejection", httpResponseMessage.getBody().toString());
        }
    }

    // Helpers
    void mockRequestResponseBuilder() {
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));
    }

    void mockRequestBody(Optional<String> body) {
        doReturn(body).when(request).getBody();
    }
}