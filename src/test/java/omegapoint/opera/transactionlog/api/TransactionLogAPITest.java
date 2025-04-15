package omegapoint.opera.transactionlog.api;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import io.vavr.control.Either;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.transactionlog.domain.TransactionLogService;
import omegapoint.opera.transactionlog.domain.model.valueobject.*;
import omegapoint.opera.transactionlog.domain.model.valueobject.SourceSystem;
import omegapoint.opera.util.HttpResponseMessageMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

class TransactionLogAPITest {

    TransactionLogAPI transactionLogAPI;
    HttpRequestMessage<Optional<String>> request;
    TransactionLogService transactionLogService;
    Logger logger;
    ExecutionContext context;

    @BeforeEach
    void setUp() {
        transactionLogAPI = new TransactionLogAPI();
        request = mock(HttpRequestMessage.class);
        transactionLogService = mock(TransactionLogService.class);
        logger = mock(Logger.class);
        context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(logger);

        mockRequestResponseBuilder();

    }

    @Nested
    class openTransactionRequest {
        @Test
        void openTransactionRequest_happyCase() {
            String mockId = UUID.randomUUID().toString();
            String startTime = LocalDateTime.now().toString();
            String sourceSystem = SourceSystem.CINODE.toString();
            String targetSystem = SourceSystem.TEAMTAILOR.toString();
            String flow = "CUSTOMER_TIME";
            String database = Database.BOTH.toString();
            String trigger = Trigger.AUTOMATIC.toString();
            String operationsType = OperationsType.BATCH.toString();
            Boolean isRelevantForOptic = true;
            mockRequestBody(Optional.of("""
                    {
                        "id": "%s",
                        "startTime": "%s",
                        "sourceSystem": "%s",
                        "targetSystem": "%s",
                        "flow": "%s",
                        "database": "%s",
                        "trigger": "%s",
                        "operationsType": "%s",
                        "isRelevantForOptic": "%s"
                    }
                    """.formatted(mockId, startTime, sourceSystem, targetSystem, flow, database, trigger, operationsType, isRelevantForOptic)));

            when(transactionLogService.openTransaction(any(), any())).thenReturn(Either.right(SuccessMessage.of("Success")));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleOpenTransaction(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.OK, httpResponseMessage.getStatus());
        }


        @Test
        void handleOpenTransaction_emptyBody_returnReject() {
            mockRequestBody(Optional.empty());

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleOpenTransaction(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("No body sent in request.", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleOpenTransaction_badTimeFormat_returnReject() {
            String mockId = UUID.randomUUID().toString();
            String startTime = "bad time";
            String sourceSystem = SourceSystem.CINODE.toString();
            String targetSystem = SourceSystem.TEAMTAILOR.toString();
            String flow = "CUSTOMER_TIME";
            String database = Database.BOTH.toString();
            String trigger = Trigger.AUTOMATIC.toString();
            String operationsType = OperationsType.BATCH.toString();
            mockRequestBody(Optional.of("""
                    {
                        "id": "%s",
                        "startTime": "%s",
                        "sourceSystem": "%s",
                        "targetSystem": "%s",
                        "flow": "%s",
                        "database": "%s",
                        "trigger": "%s",
                        "operationsType": "%s"
                    }
                    """.formatted(mockId, startTime, sourceSystem, targetSystem, flow, database, trigger, operationsType)));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleOpenTransaction(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("Date unable to be parsed", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleOpenTransaction_nullFieldInBody_returnReject() {
            String mockId = UUID.randomUUID().toString();
            String startTime = LocalDateTime.now().toString();
            String sourceSystem = SourceSystem.CINODE.toString();
            String targetSystem = "null";
            String flow = "CUSTOMER_TIME";
            String database = Database.BOTH.toString();
            String trigger = Trigger.AUTOMATIC.toString();
            String operationsType = OperationsType.BATCH.toString();
            mockRequestBody(Optional.of("""
                    {
                        "id": "%s",
                        "startTime": "%s",
                        "sourceSystem": "%s",
                        "targetSystem": %s,
                        "flow": "%s",
                        "database": "%s",
                        "trigger": "%s",
                        "operationsType": "%s"
                    }
                    """.formatted(mockId, startTime, sourceSystem, targetSystem, flow, database, trigger, operationsType)));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleOpenTransaction(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("Incorrect format of request body: \n\"Some field is null: \"",
                    httpResponseMessage.getBody().toString());
        }
    }

    @Nested
    class closeTransactionRequest {
        @Test
        void closeTransactionRequest_happyCase() {
            String mockId = UUID.randomUUID().toString();
            String endTime = LocalDateTime.now().toString();
            String numberOfRecords = Integer.toString(new NumberOfRecords(10).value);
            String blobPath = new BlobPath("somePath").value;
            String isSuccess = Boolean.toString(false);
            String reason = Reason.HTTP_ERROR.toString();
            mockRequestBody(Optional.of("""
                    {
                        "id": "%s",
                        "endTime": "%s",
                        "numberOfRecords": "%s",
                        "blobPath": "%s",
                        "isSuccess": "%s",
                        "reason": "%s"
                    }
                    """.formatted(mockId, endTime, numberOfRecords, blobPath, isSuccess, reason)));

            when(transactionLogService.closeTransaction(any(), any())).thenReturn(Either.right(SuccessMessage.of("Success")));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleCloseTransaction(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.OK, httpResponseMessage.getStatus());
        }

        @Test
        void handleCloseTransaction_emptyBody_returnReject() {
            mockRequestBody(Optional.empty());

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleCloseTransaction(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("No body sent in request.", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleCloseTransaction_badTimeFormat_returnReject() {
            String mockId = UUID.randomUUID().toString();
            String endTime = "bad time";
            String numberOfRecords = Integer.toString(new NumberOfRecords(10).value);
            String blobPath = new BlobPath("somePath").value;
            String isSuccess = Boolean.toString(false);
            String reason = Reason.HTTP_ERROR.toString();
            mockRequestBody(Optional.of("""
                    {
                        "id": "%s",
                        "endTime": "%s",
                        "numberOfRecords": "%s",
                        "blobPath": "%s",
                        "isSuccess": "%s",
                        "reason": "%s"
                    }
                    """.formatted(mockId, endTime, numberOfRecords, blobPath, isSuccess, reason)));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleCloseTransaction(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("Date unable to be parsed", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleCloseTransaction_nullFieldInBody_returnReject() {
            String mockId = UUID.randomUUID().toString();
            String endTime = LocalDateTime.now().toString();
            String numberOfRecords = Integer.toString(new NumberOfRecords(10).value);
            String blobPath = "null";
            String isSuccess = Boolean.toString(false);
            String reason = Reason.HTTP_ERROR.toString();
            mockRequestBody(Optional.of("""
                    {
                        "id": "%s",
                        "endTime": "%s",
                        "numberOfRecords": "%s",
                        "blobPath": %s,
                        "isSuccess": "%s",
                        "reason": "%s"
                    }
                    """.formatted(mockId, endTime, numberOfRecords, blobPath, isSuccess, reason)));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleCloseTransaction(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("Incorrect format of request body: \n\"Some field is null: \"",
                    httpResponseMessage.getBody().toString());
        }

        @Test
        void handleCloseTransaction_isSuccessNotTrueOrFalse_returnReject() {
            String mockId = UUID.randomUUID().toString();
            String endTime = LocalDateTime.now().toString();
            String numberOfRecords = Integer.toString(new NumberOfRecords(10).value);
            String blobPath = "something";
            String isSuccess = "TrUeeee";
            String reason = Reason.HTTP_ERROR.toString();
            mockRequestBody(Optional.of("""
                    {
                        "id": "%s",
                        "endTime": "%s",
                        "numberOfRecords": "%s",
                        "blobPath": "%s",
                        "isSuccess": "%s",
                        "reason": "%s"
                    }
                    """.formatted(mockId, endTime, numberOfRecords, blobPath, isSuccess, reason)));

            when(transactionLogService.closeTransaction(any(), any())).thenReturn(Either.right(SuccessMessage.of("Success")));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleCloseTransaction(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("Unable to turn the given Json into a domain object isSuccess must be either true or false.",
                    httpResponseMessage.getBody().toString());
        }
    }


    @Nested
    class openStepRequest {
        @Test
        void openStepRequest_happyCase() {
            String mockId = UUID.randomUUID().toString();
            String startTime = LocalDateTime.now().toString();
            String parentId = UUID.randomUUID().toString();
            String name = "anyName";
            mockRequestBody(Optional.of("""
                    {
                        "transactionId": "%s",
                        "startTime": "%s",
                        "parentId": "%s",
                        "name": "%s"
                    }
                    """.formatted(mockId, startTime, parentId, name)));

            when(transactionLogService.openStep(any(), any())).thenReturn(Either.right(SuccessMessage.of("Success")));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleOpenStep(null, request, logger, transactionLogService);

            System.out.println(httpResponseMessage.getBody());
            assertEquals(HttpStatus.OK, httpResponseMessage.getStatus());
        }

        @Test
        void handleOpenStep_emptyBody_returnReject() {
            mockRequestBody(Optional.empty());

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleOpenStep(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("No body sent in request.", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleOpenStep_badTimeFormat_returnReject() {
            String mockId = UUID.randomUUID().toString();
            String startTime = "badTime";
            String name = "anyName";
            mockRequestBody(Optional.of("""
                    {
                        "transactionId": "%s",
                        "startTime": "%s",
                        "parentId": "%s",
                        "name": "%s"
                    }
                    """.formatted(mockId, startTime, mockId, name)));

            when(transactionLogService.openStep(any(), any())).thenReturn(Either.right(SuccessMessage.of("Success")));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleOpenStep(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("Date unable to be parsed", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleOpenStep_nullFieldInBody_returnReject() {
            String mockId = UUID.randomUUID().toString();
            String startTime = "null";
            String name = "anyName";
            mockRequestBody(Optional.of("""
                    {
                        "transactionId": "%s",
                        "startTime": %s,
                        "name": "%s"
                    }
                    """.formatted(mockId, startTime, mockId, name)));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleOpenStep(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("Incorrect format of request body: \n\"Some field is null: \"",
                    httpResponseMessage.getBody().toString());
        }
    }

    @Nested
    class closeStepRequest {
        @Test
        void openCloseRequest_happyCase() {
            String mockId = UUID.randomUUID().toString();
            String endTime = LocalDateTime.now().toString();
            String name = "anyName";
            mockRequestBody(Optional.of("""
                    {
                        "transactionId": "%s",
                        "endTime": "%s",
                        "parentId": "%s",
                        "name": "%s"
                    }
                    """.formatted(mockId, endTime, mockId, name)));
            when(transactionLogService.closeStep(any(), any())).thenReturn(Either.right(SuccessMessage.of("Success")));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleCloseStep(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.OK, httpResponseMessage.getStatus());
        }

        @Test
        void handleCloseStep_emptyBody_returnReject() {
            mockRequestBody(Optional.empty());

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleCloseStep(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("No body sent in request.", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleCloseStep_badTimeFormat_returnReject() {
            String mockId = UUID.randomUUID().toString();
            String endTime = "5";
            String name = "anyName";
            mockRequestBody(Optional.of("""
                    {
                        "transactionId": "%s",
                        "parentId": "%s",
                        "endTime": "%s",
                        "name": "%s"
                    }
                    """.formatted(mockId, mockId, endTime, name)));
            when(transactionLogService.closeStep(any(), any())).thenReturn(Either.right(SuccessMessage.of("Success")));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleCloseStep(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("Date unable to be parsed", httpResponseMessage.getBody().toString());
        }

        @Test
        void handleCloseStep_nullFieldInBody_returnReject() {
            String mockId = UUID.randomUUID().toString();
            String endTime = LocalDateTime.now().toString();
            String name = "null";
            mockRequestBody(Optional.of("""
                    {
                        "transactionId": "%s",
                        "parentId": "%s",
                        "endTime": "%s",
                        "name": %s
                    }
                    """.formatted(mockId, mockId, endTime, name)));
            when(transactionLogService.closeStep(any(), any())).thenReturn(Either.right(SuccessMessage.of("Success")));

            final HttpResponseMessage httpResponseMessage = transactionLogAPI.handleCloseStep(null, request, logger, transactionLogService);

            assertEquals(HttpStatus.BAD_REQUEST, httpResponseMessage.getStatus());
            assertEquals("Incorrect format of request body: \n\"Some field is null: \"",
                    httpResponseMessage.getBody().toString());
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