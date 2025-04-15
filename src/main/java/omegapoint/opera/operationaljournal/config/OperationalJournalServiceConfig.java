package omegapoint.opera.operationaljournal.config;

import omegapoint.opera.operationaljournal.domain.OperationalJournalService;
import omegapoint.opera.operationaljournal.domain.repository.DatabaseRepository;

import java.util.logging.Logger;

import static org.apache.commons.lang3.Validate.notNull;

public class OperationalJournalServiceConfig {
    public static OperationalJournalService operationalJournalService(final Logger logger) {
        return new OperationalJournalService(
                logger,
                DatabaseRepositoryConfig.getInstance(logger),
                System.getenv("ENVIRONMENT").equals("dev") ? IntegrationTestServiceConfig.getInstance(logger) : null,
                System.getenv("ENVIRONMENT").equals("prod") ? SlackServiceConfig.slackService() : null,
                System.getenv("ENVIRONMENT")
        );
    }
}
