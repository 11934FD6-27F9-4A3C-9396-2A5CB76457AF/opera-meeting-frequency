package omegapoint.opera.transactionlog.infrastructure;

import com.microsoft.azure.functions.OutputBinding;
import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.transactionlog.domain.model.entity.CloseStep;
import omegapoint.opera.transactionlog.domain.model.entity.CloseTransaction;
import omegapoint.opera.transactionlog.domain.model.entity.OpenStep;
import omegapoint.opera.transactionlog.domain.model.entity.OpenTransaction;

import java.util.logging.Logger;

import static org.apache.commons.lang3.Validate.notNull;

public class OperationsSQLRepository {

    final Logger logger;

    public OperationsSQLRepository(Logger logger) {
        this.logger = notNull(logger);
    }

    public Either<RejectMessage, SuccessMessage> openTransaction(final OpenTransaction openTransaction,
                                                                 final OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.OpenTransaction> outputBinding) {
        logger.info("Entered open transaction in infra");
        try {
            outputBinding.setValue(omegapoint.opera.transactionlog.infrastructure.model.OpenTransaction.fromDomain(openTransaction));
            return Either.right(SuccessMessage.of("Everything went fine"));
        } catch (Exception e) {
            logger.warning(e.getMessage());
            return Either.left(RejectMessage.of500("Unable to enter open transaction into database"));
        }
    }


    public Either<RejectMessage, SuccessMessage> closeTransaction(final CloseTransaction closeTransaction,
                                                                  final OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.CloseTransaction> outputBinding) {
        logger.info("Entered close transaction in infra");
        try {
            outputBinding.setValue(omegapoint.opera.transactionlog.infrastructure.model.CloseTransaction.fromDomain(closeTransaction));
            return Either.right(SuccessMessage.of("Everything went fine"));
        } catch (Exception e) { // TODO: 2024-02-15 [tw] catch errors better when we know how we want to deal with it.
            logger.warning(e.getMessage());
            return Either.left(RejectMessage.of500("Unable to enter open transaction into database"));
        }
    }


    public Either<RejectMessage, SuccessMessage> openStep(final OpenStep openStep,
                                                          final OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.OpenStep> outputBinding) {
        logger.info("Entered open step in infra");
        try {
            outputBinding.setValue(omegapoint.opera.transactionlog.infrastructure.model.OpenStep.fromDomain(openStep));
            return Either.right(SuccessMessage.of("Everything went fine"));
        } catch (Exception e) {
            logger.warning(e.getMessage());
            return Either.left(RejectMessage.of500("Unable to enter open transaction into database"));
        }
    }


    public Either<RejectMessage, SuccessMessage> closeStep(final CloseStep closeStep,
                                                           final OutputBinding<omegapoint.opera.transactionlog.infrastructure.model.CloseStep> outputBinding) {
        logger.info("Entered close step in infra");
        try {
            outputBinding.setValue(omegapoint.opera.transactionlog.infrastructure.model.CloseStep.fromDomain(closeStep));
            return Either.right(SuccessMessage.of("Everything went fine"));
        } catch (Exception e) {
            logger.warning(e.getMessage());
            return Either.left(RejectMessage.of500("Unable to enter open transaction into database"));
        }
    }
}
