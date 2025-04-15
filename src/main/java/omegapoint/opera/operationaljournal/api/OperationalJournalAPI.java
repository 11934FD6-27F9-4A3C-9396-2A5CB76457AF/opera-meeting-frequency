package omegapoint.opera.operationaljournal.api;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import io.vavr.control.Either;
import lombok.NonNull;
import lombok.extern.java.Log;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.ResultMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.api.model.request.*;
import omegapoint.opera.operationaljournal.api.model.response.ParentWebhookStep;
import omegapoint.opera.operationaljournal.api.model.response.RerunMessage;
import omegapoint.opera.operationaljournal.api.model.response.RootRunId;
import omegapoint.opera.operationaljournal.config.OperationalJournalServiceConfig;
import omegapoint.opera.operationaljournal.domain.OperationalJournalService;

import java.util.*;
import java.util.logging.Logger;

import static org.apache.commons.lang3.Validate.notNull;

public class OperationalJournalAPI {

    @FunctionName("operational-journal-start")
    public HttpResponseMessage operationalJournalStart(
            final @HttpTrigger(name = "operationalJournalStartTrigger",
                    methods = HttpMethod.POST,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/start/{runId:maxlength(40)}" // UUIDs have 36 characters
            ) HttpRequestMessage<Optional<String>> request,
            final @BindingName("runId") String runId,
            final ExecutionContext context) {

        Logger logger = context.getLogger();

        logger.info("Start request received for '%s':\n%s".formatted(runId, request.getBody().orElse("<no body>")));

        Either<HttpResponseMessage, HttpResponseMessage>  startResult = null;

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);

            startResult = handleStartRequest(request, runId, logger, operationalJournalService);

        } catch (Throwable e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));

            startResult = Either.left(createResponse(request, RejectMessage.of500("An uncaught exception occurred.")));
        }

        if (startResult.isLeft()) {
            Object errorMessage = startResult.getLeft().getBody();

            logger.severe("""
                    {
                        "title": "Operational Journal /start failed",
                        "endpoint": "/start",
                        "errorMessage": "%s",
                        "request": {
                            "runId": "%s",
                            "body": %s
                        }
                    }
                    """.formatted(errorMessage, runId, request.getBody().orElse("null")));
        }

        // TODO: 2024-05-30 [mbloms, ls] Kanske gör detta snyggare. Fråga Ludvig.
        return startResult.fold(success -> success, error -> error);

    }

    Either<HttpResponseMessage, HttpResponseMessage> handleStartRequest(final HttpRequestMessage<Optional<String>> request,
                                                                        final String runId,
                                                                        final Logger logger,
                                                                        final OperationalJournalService operationalJournalService) {

        return request.getBody()
                .map(Either::<RejectMessage, String>right)
                .orElseGet(() -> {
                    logger.warning("Incoming request has no body, aborting.");
                    return Either.left(RejectMessage.of400("No body sent in request."));
                })
                .flatMap(body -> StartBody
                        .deserialize(body)
                        .mapLeft(left -> {
                            logger.warning("Incoming request could not be deserialized, aborting. Reason: " + left.message());
                            return RejectMessage.of400("Incorrect format of request body: \n\"%s\""
                                    .formatted(left.message()));
                        }))
                .map(body -> body.toStartRequest(runId))
                .flatMap(operationalJournalService::start)
                .peek(successMessage -> logger.info(successMessage.message()))
                .peekLeft(errorMessage -> logger.severe(errorMessage.message()))
                .map(successMessage -> createResponse(request, successMessage))
                .mapLeft(rejectMessage -> createResponse(request, rejectMessage));
    }

    @FunctionName("operational-journal-stop")
    public HttpResponseMessage operationalJournalStop(
            final @HttpTrigger(
                    name = "operationalJournalStopTrigger",
                    methods = HttpMethod.POST,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/stop/{runId:maxlength(40)}" // UUIDs have 36 characters
            ) HttpRequestMessage<Optional<String>> request,
            final @BindingName("runId") String runId,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("Stop request received for '%s':\n%s".formatted(runId, request.getBody().orElse("<no body>")));

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleStopRequest(request, runId, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleStopRequest(final HttpRequestMessage<Optional<String>> request,
                                          final String runId,
                                          final Logger logger,
                                          final OperationalJournalService operationalJournalService) {

        return request.getBody()
                .map(Either::<RejectMessage, String>right)
                .orElseGet(() -> {
                    logger.warning("Incoming request has no body, aborting.");
                    return Either.left(RejectMessage.of400("No body sent in request."));
                })
                .flatMap(body -> StopBody
                        .deserialize(body)
                        .mapLeft(left -> {
                            logger.warning("Incoming request could not be deserialized, aborting. Reason: " + left.message());
                            return RejectMessage.of400("Incorrect format of request body: \n\"%s\""
                                    .formatted(left.message()));
                        }))
                .map(stopBody -> stopBody.toStopRequest(runId))
                .flatMap(StopRequest::toJournalItem)
                .flatMap(operationalJournalService::stop)
                .peek(successMessage -> logger.info(successMessage.message()))
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()))
                .map(successMessage -> createResponse(request, successMessage))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage));
    }

    @FunctionName("operational-journal-restart")
    public HttpResponseMessage operationalJournalRestart(
            final @HttpTrigger(
                    name = "operationalJournalRestartTrigger",
                    methods = HttpMethod.POST,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/restart/{runId:maxlength(40)}" // UUIDs have 36 characters
            ) HttpRequestMessage<Optional<String>> request,
            final @BindingName("runId") String runId,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("Restart request received for '%s':\n%s".formatted(runId, request.getBody().orElse("<no body>")));

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleRestartRequest(request, runId, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleRestartRequest(final HttpRequestMessage<Optional<String>> request,
                                             final String runId,
                                             final Logger logger,
                                             final OperationalJournalService operationalJournalService) {
        return request.getBody()
                .map(Either::<RejectMessage, String>right)
                .orElseGet(() -> {
                    logger.warning("Incoming request has no body, aborting.");
                    return Either.left(RejectMessage.of400("No body sent in request."));
                })
                .flatMap(body -> RestartBody
                        .deserialize(body)
                        .mapLeft(left -> {
                            logger.warning("Incoming request could not be deserialized, aborting. Reason: " + left.message());
                            return RejectMessage.of400("Incorrect format of request body: \n\"%s\""
                                    .formatted(left.message()));
                        }))
                .map(restartBody -> restartBody.toRestartRequest(runId))
                .flatMap(RestartRequest::toJournalItem)
                .flatMap(operationalJournalService::restart)
                .peek(successMessage -> logger.info(successMessage.message()))
                .peekLeft(rejectMessage -> logger.warning(rejectMessage.message()))
                .map(successMessage -> createResponse(request, successMessage))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage));
    }

    @FunctionName("operational-journal-conflict")
    public HttpResponseMessage putConflict(
            final @HttpTrigger(
                    name = "PutConflict",
                    methods = HttpMethod.PUT,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/conflict/{runId:maxlength(40)}"
            ) HttpRequestMessage<Optional<String>> request,
            final @BindingName("runId") String runId,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("Restart request received for '%s':\n%s".formatted(runId, request.getBody().orElse("<no body>")));

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleConflictRequest(request, runId, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleConflictRequest(final HttpRequestMessage<Optional<String>> request,
                                              final String runId,
                                              final Logger logger,
                                              final OperationalJournalService operationalJournalService) {
        return request.getBody()
                .map(Either::<RejectMessage, String>right)
                .orElseGet(() -> {
                    logger.warning("Incoming request has no body, aborting.");
                    return Either.left(RejectMessage.of400("No body sent in request."));
                })
                .flatMap(body -> ConflictBody
                        .deserialize(body)
                        .mapLeft(left -> {
                            logger.warning("Incoming request could not be deserialized, aborting. Reason: " + left.message());
                            return RejectMessage.of400("Incorrect format of request body: \n\"%s\""
                                    .formatted(left.message()));
                        }))
                .map(conflictBody -> conflictBody.toConflictRequest(runId))
                .flatMap(ConflictRequest::toConflictItem)
                .flatMap(operationalJournalService::conflict)
                .peek(successMessage -> logger.info(successMessage.message()))
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()))
                .map(successMessage -> createResponse(request, successMessage))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage));
    }

    @FunctionName("operational-journal-reruns")
    public HttpResponseMessage getReruns(
            final @HttpTrigger(
                    name = "rerunTrigger",
                    methods = HttpMethod.GET,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/reruns/")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("rerunTrigger called");

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleRerunsRequest(request, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleRerunsRequest(final HttpRequestMessage<Optional<String>> request,
                                            final Logger logger,
                                            final OperationalJournalService operationalJournalService) {
        return operationalJournalService.getReruns()
                .map(RerunMessage::fromDomain)
                .map(rerunMessage -> request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(rerunMessage)
                        .build())
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()))
                .peek(responseMessage -> logger.info("Successfully created reruns response."))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage));
    }

    @FunctionName("operational-journal-rerun-info")
    public HttpResponseMessage runInfoFromId(
            final @HttpTrigger(
                    name = "rerunInfoTrigger",
                    methods = HttpMethod.GET,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/rerun-info/{runId:maxlength(40)}"
            ) HttpRequestMessage<Optional<String>> request,
            final @BindingName("runId") String runId,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("rerun info called");

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleRerunInfoRequest(request, runId, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleRerunInfoRequest(final HttpRequestMessage<Optional<String>> request,
                                               final String runId,
                                               final Logger logger,
                                               final OperationalJournalService operationalJournalService) {
        return operationalJournalService.getRerunInfo(runId)
                .map(List::of)
                .map(RerunMessage::fromDomain)
                .map(rerunMessage -> request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(rerunMessage)
                        .build())
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()))
                .peek(responseMessage -> logger.info("Successfully created response message."))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage));
    }

    @FunctionName("operational-journal-rerun-cinode")
    public HttpResponseMessage getCinodeReruns(
            final @HttpTrigger(
                    name = "rerunCinodeTrigger",
                    methods = HttpMethod.GET,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/rerun-cinode/")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("handleCinodeRerunsRequest called");

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleCinodeRerunsRequest(request, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleCinodeRerunsRequest(final HttpRequestMessage<Optional<String>> request,
                                                  final Logger logger,
                                                  final OperationalJournalService operationalJournalService) {
        return operationalJournalService.getCinodeReruns()
                .map(RerunMessage::fromDomain)
                .map(rerunMessage -> request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(rerunMessage)
                        .build())
                .peekLeft(rejectMessage -> logger.severe(rejectMessage.message()))
                .peek(responseMessage -> logger.info("Successfully created reruns response."))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage));
    }

    // Not used by any service. Intended mostly for diagnostic purpose.
    @FunctionName("operational-journal-get-run")
    public HttpResponseMessage getRun(
            final @HttpTrigger(
                    name = "GetRun",
                    methods = HttpMethod.GET,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/run/{runId:maxlength(40)}" // UUIDs have 36 characters
            ) HttpRequestMessage<Optional<String>> request,
            final @BindingName("runId") String runId,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("get run called");

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleRunRequest(request, runId, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleRunRequest(final HttpRequestMessage<Optional<String>> request,
                                         final String runId,
                                         final Logger logger,
                                         final OperationalJournalService operationalJournalService) {
        return operationalJournalService.getRun(runId)
                .map(runItem -> request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(runItem)
                        .build())
                .peek(responseMessage -> logger.info("Successfully created response."))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage));
    }

    @FunctionName("operational-journal-restrict-rerun")
    public HttpResponseMessage restrictRerun(
            final @HttpTrigger(
                    name = "RestrictRerun",
                    methods = HttpMethod.PATCH,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/restrict/{runId:maxlength(40)}"
            ) HttpRequestMessage<Optional<String>> request,
            final @BindingName("runId") String runId,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("Restrict rerun called");

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleRestrictRequest(request, runId, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleRestrictRequest(final HttpRequestMessage<Optional<String>> request,
                                              final String runId,
                                              final Logger logger,
                                              final OperationalJournalService operationalJournalService) {
        return operationalJournalService.restrictRerun(runId)
                .peekLeft(rejectMessage -> logger.warning(rejectMessage.message()))
                .peek(responseMessage -> logger.info("Successfully created response message."))
                .fold(
                        rejectMessage -> createResponse(request, rejectMessage),
                        successMessage -> createResponse(request, successMessage)
                );
    }

    @FunctionName("operational-journal-get-root-runid")
    public HttpResponseMessage getRootRunId(
            final @HttpTrigger(
                    name = "RestrictRerun",
                    methods = HttpMethod.GET,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/rootrunid/{runId:maxlength(40)}"
            ) HttpRequestMessage<Optional<String>> request,
            final @BindingName("runId") String runId,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("Get root run id called for runId: " + runId);

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleRootRunIdRequest(request, runId, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleRootRunIdRequest(final HttpRequestMessage<Optional<String>> request,
                                               final String runId,
                                               final Logger logger,
                                               final OperationalJournalService operationalJournalService) {
        return operationalJournalService.getRootRunId(UUID.fromString(runId))
                .peekLeft(rejectMessage -> logger.warning(rejectMessage.message()))
                .peek(responseMessage -> logger.info("Successfully created response message."))
                .fold(
                        rejectMessage -> createResponse(request, rejectMessage),
                        rootRunId -> request.createResponseBuilder(HttpStatus.OK)
                                .header("Content-Type", "application/json")
                                .body(RootRunId.fromDomain(rootRunId))
                                .build()
                );
    }

    @FunctionName("operational-journal-get-parent-webhookstep")
    public HttpResponseMessage getParentWebhookStep(
            final @HttpTrigger(
                    name = "RestrictRerun",
                    methods = HttpMethod.GET,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "operational-journal/parentwebhookstep/{runId:maxlength(40)}"
            ) HttpRequestMessage<Optional<String>> request,
            final @BindingName("runId") String runId,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("Get parent webhook step called for runId: " + runId);

        if (!System.getenv("ENVIRONMENT").equals("dev")) {
            return request.createResponseBuilder(HttpStatus.NOT_IMPLEMENTED).build();
        }

        try {
            final OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
            return handleParentWebhookStepRequest(request, runId, logger, operationalJournalService);
        } catch (Exception e) {
            logger.severe(e.toString());
            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
            throw new RuntimeException(e);
        }
    }

    HttpResponseMessage handleParentWebhookStepRequest(final @NonNull HttpRequestMessage<Optional<String>> request,
                                                       final @NonNull String runId,
                                                       final @NonNull Logger logger,
                                                       final @NonNull OperationalJournalService operationalJournalService) {
        return operationalJournalService.getParentWebhookStep(UUID.fromString(runId))
                .peekLeft(rejectMessage -> logger.warning(rejectMessage.message()))
                .peek(responseMessage -> logger.info("Successfully created response message."))
                .fold(
                        rejectMessage -> createResponse(request, rejectMessage),
                        parentWebookStep -> request.createResponseBuilder(HttpStatus.OK)
                                .header("Content-Type", "application/json")
                                .body(new ParentWebhookStep(parentWebookStep))
                                .build()
                );
    }

    HttpResponseMessage createResponse(HttpRequestMessage<Optional<String>> request, ResultMessage message) {
        return request.createResponseBuilder(message.status()).body(message.message()).build();
    }
    @FunctionName("daily-alert-trigger")
    public void dailyCinodeTrigger(
            final @TimerTrigger(
                    name = "dailyAlertTrigger",
                    schedule = "0 0 7 * * *"
            ) String timerInfo,
            ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("starting daily slack alert trigger");
        final OperationalJournalService operationalJournal = OperationalJournalServiceConfig.operationalJournalService(logger);
        Either<RejectMessage,SuccessMessage > messageStatus = operationalJournal.sendAlertMessage();

        if (messageStatus.isLeft()) {
            logger.severe("Could not fetch reruns with message: " + messageStatus.getLeft().message());

        }
        if (messageStatus.isRight()) {
            logger.info("Successfully sent alert");
        }
    }

//    @FunctionName("operational-journal-test")
//    public HttpResponseMessage testTrigger(
//            final @HttpTrigger(
//                    name = "TestTrigger",
//                    methods = HttpMethod.GET,
//                    authLevel = AuthorizationLevel.FUNCTION,
//                    route = "operational-journal/test"
//            ) HttpRequestMessage<Optional<String>> request,
//            final ExecutionContext context) {
//        Logger logger = context.getLogger();
//        logger.info("Test trigger called");
//
//        if (Objects.equals(System.getenv("ENVIRONMENT"), "prod")) {
//            logger.info("Exiting because of environment.");
//            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        try {
//            OperationalJournalService operationalJournalService = OperationalJournalServiceConfig.operationalJournalService(logger);
//            return operationalJournalService.test()
//                    .fold(
//                            rejectMessage -> createResponse(request, rejectMessage),
//                            successMessage -> createResponse(request, successMessage)
//                    );
//        } catch (Exception e) {
//            logger.severe(e.toString());
//            Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> logger.severe(stackTraceElement.toString()));
//            throw new RuntimeException(e);
//        }
//    }
}