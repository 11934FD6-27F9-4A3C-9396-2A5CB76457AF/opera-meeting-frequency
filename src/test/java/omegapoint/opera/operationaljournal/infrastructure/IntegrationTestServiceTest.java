package omegapoint.opera.operationaljournal.infrastructure;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.QueueServiceClientBuilder;
import io.vavr.control.Either;
import omegapoint.opera.operationaljournal.azurite.AzuriteCredentials;
import omegapoint.opera.operationaljournal.azurite.Host;
import omegapoint.opera.operationaljournal.azurite.Port;
import omegapoint.opera.operationaljournal.domain.model.Status;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;
import omegapoint.opera.operationaljournal.infrastructure.IntegrationTestService;
import omegapoint.opera.operationaljournal.infrastructure.model.integrationtests.TestResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class IntegrationTestServiceTest {

    private static final int TS_PORT = 10002;
    private static final String TABLE_NAME = "TestRuns";
    private AzuriteCredentials tableCredentials;

    private static final int QS_PORT = 10001;
    private static final String QUEUE_NAME = "test-results";
    private AzuriteCredentials queueCredentials;

    @Container
    public static GenericContainer<?> azurite = new GenericContainer<>("mcr.microsoft.com/azure-storage/azurite:3.31.0")
            .withExposedPorts(TS_PORT, QS_PORT);

    private TableClient tableClient;
    private QueueClient queueClient;
    private Logger mockLogger;

    private IntegrationTestService integrationTestService;

    private final static UUID runId = UUID.randomUUID();
    private final static UUID rootRunId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tableCredentials = new AzuriteCredentials(
                Host.fromDockerIP(azurite.getHost()),
                Port.fromInteger(azurite.getMappedPort(TS_PORT))
        );
        queueCredentials = new AzuriteCredentials(
                Host.fromDockerIP(azurite.getHost()),
                Port.fromInteger(azurite.getMappedPort(QS_PORT))
        );

        tableClient = new TableServiceClientBuilder()
                .connectionString(tableCredentials.tableConnectionString())
                .buildClient()
                .createTableIfNotExists(TABLE_NAME);
        queueClient = new QueueServiceClientBuilder()
                .connectionString(queueCredentials.queueConnectionString())
                .buildClient()
                .createQueue(QUEUE_NAME);
        mockLogger = mock(Logger.class);

        integrationTestService = new IntegrationTestService(
                tableClient,
                queueClient,
                mockLogger
        );
    }

    @AfterEach
    void tearDown() {
        tableClient.deleteTable();
    }

    @Test
    void isIntegrationTest_happypath_is_test() {
        TableEntity entity = new TableEntity(rootRunId.toString(), rootRunId.toString());
        entity.addProperty("expected", "expectedValue");
        entity.addProperty("commitHash", "commitHashValue");
        tableClient.createEntity(entity);

        var result = integrationTestService.isIntegrationTest(rootRunId);

        assertNotNull(result);
        assertTrue(result.isRight());
        assertTrue(result.get());
    }

    @Test
    void isIntegrationTest_happypath_is_not_test() {

        var result = integrationTestService.isIntegrationTest(runId);

        assertNotNull(result);
        assertTrue(result.isRight());
        assertFalse(result.get());
    }

    @Test
    void isIntegrationTest_tableClient_fails_with_TableServiceException() {
        TableClient faultyTableClient = new TableClientBuilder()
                .connectionString(tableCredentials.tableConnectionString())
                .tableName("i_do_not_exist")
                .buildClient();

        var integrationTestServiceWithFaultyTableClient = new IntegrationTestService(
                faultyTableClient,
                queueClient,
                mock(Logger.class)
        );

        var result = integrationTestServiceWithFaultyTableClient.isIntegrationTest(runId);

        assertNotNull(result);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().message().contains("Status code 400"));
    }

    @Test
    void sendError_happypath() {

        var result = integrationTestService.sendError(rootRunId, new JournalItem(
                runId.toString(),
                1,
                Instant.now(),
                Status.ERROR,
                "message",
                "data-processing"),
                "webhookStep");

        assertNotNull(result);
        assertTrue(result.isRight());
        var message = queueClient.receiveMessage().getBody().toString();
        assertTrue(message.contains("message"));
        assertTrue(message.contains("data-processing"));
        assertTrue(message.contains(rootRunId.toString()));
    }

    @Test
    void sendTestResult_queueClient_fails() {
        QueueClient faultyQueueClient = new QueueClientBuilder()
                .connectionString(queueCredentials.queueConnectionString())
                .queueName("i_do_not_exist")
                .buildClient();

        var integrationTestServiceWithFaultyQueueClient = new IntegrationTestService(
                tableClient,
                faultyQueueClient,
                mock(Logger.class)
        );

        var result = integrationTestServiceWithFaultyQueueClient.sendError(rootRunId, new JournalItem(
                runId.toString(),
                1,
                Instant.now(),
                Status.ERROR,
                "message",
                "data-processing"),
                "webhookStep");

        assertNotNull(result);
        assertTrue(result.isLeft());
        assertTrue(result.getLeft().message().contains("Status code 400"));
    }
}