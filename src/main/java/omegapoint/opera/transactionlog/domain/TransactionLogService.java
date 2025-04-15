package omegapoint.opera.transactionlog.domain;

import com.microsoft.azure.functions.OutputBinding;
import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.transactionlog.domain.model.entity.CloseStep;
import omegapoint.opera.transactionlog.domain.model.entity.CloseTransaction;
import omegapoint.opera.transactionlog.domain.model.entity.OpenStep;
import omegapoint.opera.transactionlog.domain.model.entity.OpenTransaction;
import omegapoint.opera.transactionlog.infrastructure.OperationsSQLRepository;

import java.util.logging.Logger;

public class TransactionLogService {

    private final OperationsSQLRepository operationsSQLRepository;
    private final Logger logger;

    public TransactionLogService(OperationsSQLRepository operationsSQLRepository, Logger logger) {
        this.operationsSQLRepository = operationsSQLRepository;
        this.logger = logger;
    }

    public Either<RejectMessage, SuccessMessage> openTransaction(OpenTransaction openTransaction,
                                                                 OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.OpenTransaction> output) {
        logger.info("entered TransactionLogService open transaction");
        return operationsSQLRepository.openTransaction(openTransaction, output);
    }

    public Either<RejectMessage, SuccessMessage> closeTransaction(CloseTransaction closeTransaction,
                                                                  OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.CloseTransaction> output) {
        logger.info("entered closeTransaction close transaction");
        return operationsSQLRepository.closeTransaction(closeTransaction, output);
    }

    public Either<RejectMessage, SuccessMessage> openStep(OpenStep openStep,
                                                          OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.OpenStep> output) {
        logger.info("entered TransactionLogService open step");
        return operationsSQLRepository.openStep(openStep, output);
    }

    public Either<RejectMessage, SuccessMessage> closeStep(CloseStep closeStep,
                                                           OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.CloseStep> output) {
        logger.info("entered TransactionLogService close step");
        return operationsSQLRepository.closeStep(closeStep, output);
    }
}
