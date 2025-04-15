package omegapoint.opera.transactionlog.config;

import omegapoint.opera.transactionlog.domain.TransactionLogService;
import omegapoint.opera.transactionlog.infrastructure.OperationsSQLRepository;

import java.util.logging.Logger;

public class TransactionLogServiceConfig {
    
    public static TransactionLogService createTransactionLogService(final OperationsSQLRepository operationsSQLRepository, Logger logger) {
        return new TransactionLogService(operationsSQLRepository, logger);
    }
}

