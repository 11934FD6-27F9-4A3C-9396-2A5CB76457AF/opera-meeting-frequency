package omegapoint.opera.transactionlog.api;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.sql.annotation.CommandType;
import com.microsoft.azure.functions.sql.annotation.SQLInput;
import com.microsoft.azure.functions.sql.annotation.SQLOutput;
import io.vavr.control.Either;
import lombok.NonNull;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.ResultMessage;
import omegapoint.opera.transactionlog.api.model.entity.CloseStep;
import omegapoint.opera.transactionlog.api.model.entity.CloseTransaction;
import omegapoint.opera.transactionlog.api.model.entity.OpenStep;
import omegapoint.opera.transactionlog.api.model.entity.OpenTransaction;
import omegapoint.opera.transactionlog.config.OperationsSQLRepositoryConfig;
import omegapoint.opera.transactionlog.config.TransactionLogServiceConfig;
import omegapoint.opera.transactionlog.domain.TransactionLogService;
import omegapoint.opera.transactionlog.infrastructure.OperationsSQLRepository;
import omegapoint.opera.transactionlog.infrastructure.model.LatestRun;

import java.util.Optional;
import java.util.logging.Logger;

public class TransactionLogAPI {

    @FunctionName("opera-transaction-log-function-open")
    public HttpResponseMessage openTransactionLog(
            @HttpTrigger(
                    name = "openTransactionLogTrigger",
                    methods = HttpMethod.POST,
                    authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<String>> request,
            @SQLOutput(
                    name = "openTransaction",
                    commandText = "TransactionLog.OpenTransaction",
                    connectionStringSetting = "TransactionLogDBConnectionString"
            )
            OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.OpenTransaction> output,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("opening a transaction");

        final OperationsSQLRepository operationsSQLRepository = OperationsSQLRepositoryConfig.createOperationsSQLRepository(logger);
        final TransactionLogService transactionLogService = TransactionLogServiceConfig.createTransactionLogService(operationsSQLRepository, logger);

        return handleOpenTransaction(output, request, logger, transactionLogService);
    }

    @FunctionName("opera-transaction-log-function-close")
    public HttpResponseMessage closeTransactionLog(
            @HttpTrigger(
                    name = "closeTransactionLogTrigger",
                    methods = HttpMethod.POST,
                    authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<String>> request,
            @SQLOutput(
                    name = "closeTransaction",
                    commandText = "TransactionLog.CloseTransaction",
                    connectionStringSetting = "TransactionLogDBConnectionString"
            )
            OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.CloseTransaction> output,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("closing a transaction");

        final OperationsSQLRepository operationsSQLRepository = OperationsSQLRepositoryConfig.createOperationsSQLRepository(logger);
        final TransactionLogService transactionLogService = TransactionLogServiceConfig.createTransactionLogService(operationsSQLRepository, logger);

        return handleCloseTransaction(output, request, logger, transactionLogService);
    }

    @FunctionName("opera-transaction-log-function-open-step")
    public HttpResponseMessage OpenStepTransactionLog(
            @HttpTrigger(
                    name = "openStepTrigger",
                    methods = HttpMethod.POST,
                    authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<String>> request,
            @SQLOutput(
                    name = "openStepTransaction",
                    commandText = "TransactionLog.OpenStep",
                    connectionStringSetting = "TransactionLogDBConnectionString"
            )
            OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.OpenStep> output,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("opening a step");

        final OperationsSQLRepository operationsSQLRepository = OperationsSQLRepositoryConfig.createOperationsSQLRepository(logger);
        final TransactionLogService transactionLogService = TransactionLogServiceConfig.createTransactionLogService(operationsSQLRepository, logger);

        return handleOpenStep(output, request, logger, transactionLogService);
    }

    @FunctionName("opera-transaction-log-function-close-step")
    public HttpResponseMessage CloseStepTransactionLog(
            @HttpTrigger(
                    name = "closeStepTrigger",
                    methods = HttpMethod.POST,
                    authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<String>> request,
            @SQLOutput(
                    name = "closeStepTransaction",
                    commandText = "TransactionLog.CloseStep",
                    connectionStringSetting = "TransactionLogDBConnectionString"
            )
            OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.CloseStep> output,
            final ExecutionContext context) {
        Logger logger = context.getLogger();
        logger.info("closing a step");

        final OperationsSQLRepository operationsSQLRepository = OperationsSQLRepositoryConfig.createOperationsSQLRepository(logger);
        final TransactionLogService transactionLogService = TransactionLogServiceConfig.createTransactionLogService(operationsSQLRepository, logger);

        return handleCloseStep(output, request, logger, transactionLogService);
    }

    @FunctionName("opera-transaction-log-function-get-latest-run")
    public HttpResponseMessage getLatestRun(
            @HttpTrigger(
                    name = "getLatestRunTrigger",
                    methods = HttpMethod.GET,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "transaction-log/get-latest-run/{flowName:maxlength(50)}")
            HttpRequestMessage<Optional<String>> request,
            final @BindingName("flowName") String flowName,
            final @SQLInput(
                    name = "getLatestRun",
                    commandText = """
                            SELECT * FROM TransactionLog.LatestRun
                            WHERE Flow = @Flow""",
                    commandType = CommandType.Text,
                    parameters = "@Flow={flowName}",
                    connectionStringSetting = "TransactionLogDBConnectionString"
            ) LatestRun[] latestRuns,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("Getting latest run for flow name %s".formatted(flowName));

        if (latestRuns.length == 0) {
            logger.info("no latest run for flow-name: %s".formatted(flowName));
            return createOkResponse(request, "null"); // NOTE: This can be deserialized to Optional.empty()
        } else if (latestRuns.length > 1) {
            logger.severe(("Req-Att: LatestRun view has several runs for flow name: %s. " +
                    "This should never happen and probably means that either the SQL View contains incorrect data " +
                    "(more than one per flow) or it means that the SQL expression above in this endpoint is incorrect.")
                    .formatted(flowName));
        }
        return createOkResponse(request, latestRuns[0]); // NOTE: This can be deserialized to Optional.of(latestRuns[0])
    }

    @FunctionName("opera-transaction-log-function-get-latest-runs")
    public HttpResponseMessage getLatestRuns(
            @HttpTrigger(
                    name = "getLatestRunsTrigger",
                    methods = HttpMethod.GET,
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "transaction-log/get-latest-runs")
            HttpRequestMessage<Optional<String>> request,
            final @SQLInput(
                    name = "getLatestRuns",
                    commandText = """
                            SELECT * FROM TransactionLog.LatestRun""",
                    commandType = CommandType.Text,
                    connectionStringSetting = "TransactionLogDBConnectionString"
            ) LatestRun[] latestRuns,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("Getting latest runs");

        if (latestRuns.length == 0) {
            logger.severe("no latest runs");
            return createOkResponse(request, "[]");
        } else {
            return createOkResponse(request, latestRuns);
        }
    }

    
    private static HttpResponseMessage createOkResponse(@NonNull final HttpRequestMessage<Optional<String>> request,
                                                        @NonNull final Object body) {
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(body)
                .build();
    }

    HttpResponseMessage handleOpenTransaction(final OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.OpenTransaction> outputBinding,
                                              final HttpRequestMessage<Optional<String>> request,
                                              final Logger logger,
                                              final TransactionLogService transactionLogService) {

        return request.getBody()
                .map(Either::<RejectMessage, String>right)
                .orElseGet(() -> handleRequestHasNoBody(logger))
                .flatMap(body -> OpenTransaction
                        .deserialize(body)
                        .mapLeft(left -> handleCouldNotDeserialize(logger, left)))
                .flatMap(OpenTransaction::toDomain)
                .flatMap(openTransactionDomain -> transactionLogService.openTransaction(openTransactionDomain, outputBinding))
                .map(successMessage -> createResponse(request, successMessage, logger))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage, logger));
    }

    HttpResponseMessage handleCloseTransaction(final OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.CloseTransaction> outputBinding,
                                               final HttpRequestMessage<Optional<String>> request,
                                               final Logger logger,
                                               final TransactionLogService transactionLogService) {

        return request.getBody()
                .map(Either::<RejectMessage, String>right)
                .orElseGet(() -> handleRequestHasNoBody(logger))
                .flatMap(body -> CloseTransaction
                        .deserialize(body)
                        .mapLeft(left -> handleCouldNotDeserialize(logger, left)))
                .flatMap(closeTransaction -> {
                    if (closeTransaction != null && closeTransaction.blobPath != null && closeTransaction.blobPath.length() > 4000) {
                        logger.warning("Blob path is too large for transaction log to handle, it will be " +
                                "cut at 4000 as this is the max limit of a nvarchar in Azure SQL database. " +
                                "This is probably not actually a single gigantic path, but rather a lot of paths from" +
                                " different chunks concatenated as we see it in frontend.");
                    }
                    return CloseTransaction.toDomain(closeTransaction);
                })
                .flatMap(closeTransactionDomain -> transactionLogService.closeTransaction(closeTransactionDomain, outputBinding))
                .map(successMessage -> createResponse(request, successMessage, logger))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage, logger));
    }

    HttpResponseMessage handleOpenStep(final OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.OpenStep> outputBinding,
                                       final HttpRequestMessage<Optional<String>> request,
                                       final Logger logger,
                                       final TransactionLogService transactionLogService) {

        return request.getBody()
                .map(Either::<RejectMessage, String>right)
                .orElseGet(() -> handleRequestHasNoBody(logger))
                .flatMap(body -> OpenStep
                        .deserialize(body)
                        .mapLeft(left -> handleCouldNotDeserialize(logger, left)))
                .flatMap(OpenStep::toDomain)
                .flatMap(openStepDomain -> transactionLogService.openStep(openStepDomain, outputBinding))
                .map(successMessage -> createResponse(request, successMessage, logger))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage, logger));
    }


    HttpResponseMessage handleCloseStep(final OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.CloseStep> outputBinding,
                                        final HttpRequestMessage<Optional<String>> request,
                                        final Logger logger,
                                        final TransactionLogService transactionLogService) {

        return request.getBody()
                .map(Either::<RejectMessage, String>right)
                .orElseGet(() -> handleRequestHasNoBody(logger))
                .flatMap(body -> CloseStep
                        .deserialize(body)
                        .mapLeft(left -> handleCouldNotDeserialize(logger, left)))
                .flatMap(CloseStep::toDomain)
                .flatMap(closeStepDomain -> transactionLogService.closeStep(closeStepDomain, outputBinding))
                .map(successMessage -> createResponse(request, successMessage, logger))
                .getOrElseGet(rejectMessage -> createResponse(request, rejectMessage, logger));
    }

    private static Either<RejectMessage, String> handleRequestHasNoBody(final Logger logger) {
        logger.warning("Incoming request has no body, aborting.");
        return Either.left(RejectMessage.of400("No body sent in request."));
    }
    private static RejectMessage handleCouldNotDeserialize(final Logger logger, final RejectMessage left) {
        logger.warning("Incoming request could not be deserialized, aborting. Reason: " + left.message());
        return RejectMessage.of400("Incorrect format of request body: \n\"%s\"".formatted(left.message()));
    }

    HttpResponseMessage createResponse(final HttpRequestMessage<Optional<String>> request,
                                       final ResultMessage message,
                                       final Logger logger) {

        if (message.message().equals("Everything went fine")) {
            logger.info("Creating response " + message.message());

        } else {
            logger.warning("Creating response " + message.message());
        }
        return request.createResponseBuilder(message.status()).body(message.message()).build();
    }


}