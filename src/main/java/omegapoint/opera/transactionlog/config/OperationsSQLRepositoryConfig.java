package omegapoint.opera.transactionlog.config;

import omegapoint.opera.transactionlog.infrastructure.OperationsSQLRepository;

import java.util.logging.Logger;

public class OperationsSQLRepositoryConfig {
    public static OperationsSQLRepository createOperationsSQLRepository(final Logger logger) {
        return new OperationsSQLRepository(logger);
    }
}
