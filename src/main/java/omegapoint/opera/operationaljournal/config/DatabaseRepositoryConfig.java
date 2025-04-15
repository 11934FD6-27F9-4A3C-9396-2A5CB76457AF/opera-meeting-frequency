package omegapoint.opera.operationaljournal.config;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import lombok.NonNull;
import omegapoint.opera.operationaljournal.domain.repository.DatabaseRepository;

import java.util.logging.Logger;

import static org.apache.commons.lang3.Validate.notNull;

public class DatabaseRepositoryConfig {

    public static DatabaseRepository getInstance(final @NonNull Logger logger) {
        final String serverName = notNull(System.getenv("OperationalJournalDBServerName"), "DB server name string failed to load.");
        final String databaseName = notNull(System.getenv("OperationalJournalDBDatabaseName"), "DB database name string failed to load.");

        SQLServerDataSource dataSource = new SQLServerDataSource();
        dataSource.setServerName(serverName);
        dataSource.setDatabaseName(databaseName);
        dataSource.setAuthentication("ActiveDirectoryMSI");

        return new omegapoint.opera.operationaljournal.infrastructure.DatabaseRepository(dataSource, logger);
    }
}
