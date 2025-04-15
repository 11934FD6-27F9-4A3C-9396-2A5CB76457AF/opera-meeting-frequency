package omegapoint.opera.operationaljournal.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.QueueMessageEncoding;
import lombok.NonNull;
import omegapoint.opera.operationaljournal.domain.repository.IntegrationTests;
import omegapoint.opera.operationaljournal.infrastructure.IntegrationTestService;

import java.util.logging.Logger;

import static org.apache.commons.lang3.Validate.notNull;

public class IntegrationTestServiceConfig {

    private static final @NonNull String TABLE_NAME = "TestRuns";
    private static final @NonNull String QUEUE_NAME = "test-results-sadpath";

    public static IntegrationTests getInstance(final @NonNull Logger logger) {
        return new IntegrationTestService(
                getTableClient(),
                getQueueClient(),
                logger
        );
    }

    private static @NonNull TableClient getTableClient() {
        var tableServiceClient =  new TableServiceClientBuilder()
                .endpoint(tableServiceUrl())
                .credential(new ManagedIdentityCredentialBuilder().build())
                .buildClient();
        tableServiceClient.createTableIfNotExists(TABLE_NAME);
        return tableServiceClient.getTableClient(TABLE_NAME);
    }

    private static String tableServiceUrl() {
        return String.format("https://%s.table.core.windows.net/",
                notNull(System.getenv("STORAGE_ACCOUNT_NAME"), "Could not get storage account name"));
    }

    private static QueueClient getQueueClient() {
        return new QueueClientBuilder()
                .endpoint(notNull(System.getenv("STORAGE_ACCOUNT_QUEUE_ENDPOINT"),
                        "Could not get storage account queue endpoint"))
                .credential(new ManagedIdentityCredentialBuilder().build())
                .messageEncoding(QueueMessageEncoding.BASE64)
                .queueName(QUEUE_NAME)
                .buildClient();
    }
}
