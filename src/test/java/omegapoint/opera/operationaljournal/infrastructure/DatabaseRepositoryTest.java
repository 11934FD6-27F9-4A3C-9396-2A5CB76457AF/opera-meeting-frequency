package omegapoint.opera.operationaljournal.infrastructure;

import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.domain.model.*;
import omegapoint.opera.operationaljournal.domain.model.table.*;
import omegapoint.opera.operationaljournal.domain.repository.DatabaseRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseRepositoryTest {

    private Connection connection;
    private DatabaseRepository databaseRepository;

    private DataSource mockDataSource;
    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private DatabaseRepository mockDatabaseRepository;

    private Instant testInstant = Instant.now();

    @Before
    public void setUp() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        connection = dataSource.getConnection();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS OperationsJournal");
            stmt.execute("CREATE TABLE OperationsJournal.Run (RunID VARCHAR(255) PRIMARY KEY, ParentID VARCHAR(255), OriginTimestamp VARCHAR(255), WebhookStep VARCHAR(255), ConflictID VARCHAR(255))");
        }
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO OperationsJournal.Run (RunID, ParentID, OriginTimestamp, WebhookStep) VALUES (?, ?, ?, ?)");) {
            stmt.setString(1, "48be9e3e-e64d-4a37-b185-bcf7277a6990");
            stmt.setString(2, "99e2f058-3899-419a-9477-8ce72ed28e6c");
            stmt.setString(3, testInstant.toString());
            stmt.setString(4, "Webhook-data-processing-orchestrator-source-system-deserializer");
            stmt.execute();
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE OperationsJournal.Conflict (RunID VARCHAR(255), ConflictID VARCHAR(255))");
        }
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO OperationsJournal.Conflict (RunID, ConflictID) VALUES (?, ?)");) {
            stmt.setString(1, "48be9e3e-e64d-4a37-b185-bcf7277a6990");
            stmt.setString(2, "test.conflict.id");
            stmt.execute();
        }
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO OperationsJournal.Conflict (RunID, ConflictID) VALUES (?, ?)");) {
            stmt.setString(1, "2dd8138b-bf4a-4f58-b040-c6b75f22ef4d");
            stmt.setString(2, "test.conflict.id");
            stmt.execute();
        }
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO OperationsJournal.Conflict (RunID, ConflictID) VALUES (?, ?)");) {
            stmt.setString(1, "48be9e3e-e64d-4a37-b185-bcf7277a6990");
            stmt.setString(2, "another.test.conflict.id");
            stmt.execute();
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE OperationsJournal.Journal (RunID VARCHAR(255) PRIMARY KEY, Timestamp VARCHAR(255), Status VARCHAR(255), Message VARCHAR(255), OriginFunction VARCHAR(255), Attempt VARCHAR(255))");
        }
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) VALUES (?, ?, ?, ?, ?, ?)");) {
            stmt.setString(1, "48be9e3e-e64d-4a37-b185-bcf7277a6990");
            stmt.setString(2, testInstant.toString());
            stmt.setString(3, "RESTARTED");
            stmt.setString(4, "message");
            stmt.setString(5, "Webhook-data-processing-orchestrator-source-system-deserializer");
            stmt.setInt(6, 1);
            stmt.execute();
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE OperationsJournal.QueueCheckpoint (RunID VARCHAR(255) PRIMARY KEY, QueueName VARCHAR(255), BlobContainerName VARCHAR(255), BlobPath VARCHAR(255))");
        }

        this.databaseRepository = new omegapoint.opera.operationaljournal.infrastructure.DatabaseRepository(dataSource, mock(Logger.class));

        this.mockDataSource = mock(DataSource.class);
        this.mockConnection = mock(Connection.class);
        this.mockStatement = mock(PreparedStatement.class);
        this.mockDatabaseRepository = new omegapoint.opera.operationaljournal.infrastructure.DatabaseRepository(mockDataSource, mock(Logger.class));
    }

    @After
    public void tearDown() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE OperationsJournal.Conflict");
            stmt.execute("DROP TABLE OperationsJournal.Run");
            stmt.execute("DROP TABLE OperationsJournal.QueueCheckpoint");
            stmt.execute("DROP TABLE OperationsJournal.Journal");
            stmt.execute("DROP SCHEMA OperationsJournal");
        }
        connection.close();
    }

    // Disabled since special azure sql stuff breaks H2 mock database.
    public void insertConflict() {
        ConflictItem conflictItem = new ConflictItem(UUID.randomUUID().toString(), "test.conflict.id");

        Either<RejectMessage, SuccessMessage> result = databaseRepository.putConflict(conflictItem);
        result.peekLeft(rejectMessage -> System.out.println(rejectMessage.message()));
        assertTrue(result.isRight());
    }

    @Test
    public void insertRunAndQueueCheckpoint() {
        RunItem runItem = new RunItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                new WebhookStep("preprocess_sage_employee_creation"),
                new ConflictId("sage.employee"),
                new QueueCheckpoint(new CheckpointPath("opera-queue-test"),
                        new BlobReference("containerName", "pa/th"))
        );

        Either<RejectMessage, SuccessMessage> result = databaseRepository.putRunAndQueueCheckpoint(runItem);
        result.peekLeft(rejectMessage -> System.out.println(rejectMessage.message()));
        assertTrue(result.isRight());
    }

    @Test
    public void insertJournal() {
        JournalItem journalItem = new JournalItem(
                UUID.randomUUID().toString(),
                1,
                Instant.now(),
                Status.RESTARTED,
                "This is a dummy message.",
                "Webhook-data-processing-orchestrator-source-system-deserializer"
        );

        Either<RejectMessage, SuccessMessage> result = databaseRepository.putJournal(journalItem);
        result.peekLeft(rejectMessage -> System.out.println(rejectMessage.message()));
        assertTrue(result.isRight());
    }

    @Test
    public void insertConflict_with_failure() throws SQLException {
        when(mockConnection.prepareStatement(any()))
                .thenReturn(mockStatement);
        when(mockDataSource.getConnection())
                .thenReturn(mockConnection);
        when(mockStatement.executeQuery())
                .thenThrow(new SQLException("TestSQL exception"));

        ConflictItem conflictItem = new ConflictItem(UUID.randomUUID().toString(), "test.conflict.id");

        Either<RejectMessage, SuccessMessage> result = mockDatabaseRepository.putConflict(conflictItem);
        assertTrue(result.isLeft());
        assertEquals("TestSQL exception", result.getLeft().message());
    }

    @Test
    public void insertRunAndQueueCheckpoint_with_failure() throws SQLException {
        when(mockDataSource.getConnection())
                .thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any()))
                .thenReturn(mockStatement);
        when(mockStatement.executeUpdate())
                .thenThrow(new SQLException("TestSQL exception"));

        RunItem runItem = new RunItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                new WebhookStep("preprocess_sage_employee_creation"),
                new ConflictId("sage.employee"),
                new QueueCheckpoint(new CheckpointPath("opera-queue-test"),
                        new BlobReference("containerName", "pa/th"))
        );

        Either<RejectMessage, SuccessMessage> result = mockDatabaseRepository.putRunAndQueueCheckpoint(runItem);
        result.peekLeft(rejectMessage -> System.out.println(rejectMessage.message()));
        assertTrue(result.isLeft());
        assertEquals("TestSQL exception", result.getLeft().message());
    }

    @Test
    public void insertJournal_with_failure() throws SQLException {
        when(mockDataSource.getConnection())
                .thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any()))
                .thenReturn(mockStatement);
        when(mockStatement.executeUpdate())
                .thenThrow(new SQLException("TestSQL exception"));

        JournalItem journalItem = new JournalItem(
                UUID.randomUUID().toString(),
                1,
                Instant.now(),
                Status.RESTARTED,
                "This is a dummy message.",
                "Webhook-data-processing-orchestrator-source-system-deserializer"
        );

        Either<RejectMessage, SuccessMessage> result = mockDatabaseRepository.putJournal(journalItem);
        result.peekLeft(rejectMessage -> System.out.println(rejectMessage.message()));
        assertTrue(result.isLeft());
        assertEquals("TestSQL exception", result.getLeft().message());
    }

    @Test
    public void getExistingRun_with_success() throws SQLException {
        omegapoint.opera.operationaljournal.infrastructure.model.RunItem[] runItems = databaseRepository.getExistingRun("48be9e3e-e64d-4a37-b185-bcf7277a6990")
                .peekLeft(rejectMessage -> System.out.println(rejectMessage.message()))
                .get();

        assertEquals(1, runItems.length);
        assertEquals("48be9e3e-e64d-4a37-b185-bcf7277a6990", runItems[0].RunID);
        assertEquals("99e2f058-3899-419a-9477-8ce72ed28e6c", runItems[0].ParentID);
        assertEquals(testInstant.toString(), runItems[0].OriginTimestamp);
        assertEquals("Webhook-data-processing-orchestrator-source-system-deserializer", runItems[0].WebhookStep);
    }

    @Test
    public void getExistingRun_with_no_runs() throws SQLException {
        omegapoint.opera.operationaljournal.infrastructure.model.RunItem[] runItems = databaseRepository.getExistingRun("illegal_run_id")
                .peekLeft(rejectMessage -> System.out.println(rejectMessage.message()))
                .get();

        assertEquals(0, runItems.length);
    }

    @Test
    public void getExistingJournal_with_success() throws SQLException {
        omegapoint.opera.operationaljournal.infrastructure.model.JournalItem[] journalItems = databaseRepository.getExistingJournal("48be9e3e-e64d-4a37-b185-bcf7277a6990")
                .peekLeft(rejectMessage -> System.out.println(rejectMessage.message()))
                .get();

        assertEquals(1, journalItems.length);
        assertEquals("48be9e3e-e64d-4a37-b185-bcf7277a6990", journalItems[0].RunID);
        assertEquals("RESTARTED", journalItems[0].Status);
        assertEquals("message", journalItems[0].Message);
        assertEquals("Webhook-data-processing-orchestrator-source-system-deserializer", journalItems[0].OriginFunction);
        assertEquals(0, journalItems[0].Attempt.compareTo(1));
        assertEquals(testInstant.toString(), journalItems[0].Timestamp);
    }

    @Test
    public void getExistingJournal_with_no_items() throws SQLException {
        omegapoint.opera.operationaljournal.infrastructure.model.JournalItem[] journalItems = databaseRepository.getExistingJournal("illegal_run_id")
                .peekLeft(rejectMessage -> System.out.println(rejectMessage.message()))
                .get();

        assertEquals(0, journalItems.length);
    }

    @Test
    public void getRerunInfo_with_success() throws SQLException {
        ResultSet mockResultSet = mock(ResultSet.class);
        Instant instant = Instant.now();
        String runId = "5554a24a-d75b-4a9c-9a97-c861d47e9c6f";

        when(mockDataSource.getConnection())
                .thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any()))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery())
                .thenReturn(mockResultSet);
        when(mockResultSet.next())
                .thenReturn(true)
                .thenReturn(false);
        when(mockResultSet.getString("RunID"))
                .thenReturn(runId);
        when(mockResultSet.getInt("Attempt"))
                .thenReturn(2);
        when(mockResultSet.getString("WebhookStep"))
                .thenReturn("update_cinode_team");
        when(mockResultSet.getString("OriginTimestamp"))
                .thenReturn(instant.toString());
        when(mockResultSet.getString("QueueName"))
                .thenReturn("QUEUE_NAME");
        when(mockResultSet.getString("BlobContainerName"))
                .thenReturn("containerName");
        when(mockResultSet.getString("BlobPath"))
                .thenReturn("pa/th");

        Either<RejectMessage, Rerun> result = mockDatabaseRepository.getRerunInfo(runId);

        assertTrue(result.isRight());
        assertEquals(runId, result.get().runId().toString());
        assertEquals(2, result.get().attempt());
        assertEquals(new WebhookStep("update_cinode_team"), result.get().webhookStep());
        assertEquals(ZonedDateTime.parse(instant.toString()), result.get().originTimestamp());
        assertEquals("QUEUE_NAME", result.get().queueName());
        assertEquals(new BlobReference("containerName", "pa/th"), result.get().blobReference());
    }

    @Test
    public void getRerunInfo_with_no_records() throws SQLException {
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockDataSource.getConnection())
                .thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any()))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery())
                .thenReturn(mockResultSet);
        when(mockResultSet.next())
                .thenReturn(false);

        Either<RejectMessage, Rerun> result = mockDatabaseRepository.getRerunInfo(UUID.randomUUID().toString());

        assertTrue(result.isLeft());
    }

    @Test
    public void getRerunInfo_with_multiple_records() throws SQLException {
        ResultSet mockResultSet = mock(ResultSet.class);
        Instant instant = Instant.now();
        String runId = "5554a24a-d75b-4a9c-9a97-c861d47e9c6f";

        when(mockDataSource.getConnection())
                .thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any()))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery())
                .thenReturn(mockResultSet);
        when(mockResultSet.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(mockResultSet.getString("RunID"))
                .thenReturn(runId);
        when(mockResultSet.getInt("Attempt"))
                .thenReturn(2);
        when(mockResultSet.getString("WebhookStep"))
                .thenReturn("UPDATE_CINODE_TEAM");
        when(mockResultSet.getString("OriginTimestamp"))
                .thenReturn(instant.toString());
        when(mockResultSet.getString("QueueName"))
                .thenReturn("QUEUE_NAME");
        when(mockResultSet.getString("BlobContainerName"))
                .thenReturn("containerName");
        when(mockResultSet.getString("BlobPath"))
                .thenReturn("pa/th");

        Either<RejectMessage, Rerun> result = mockDatabaseRepository.getRerunInfo(runId);

        assertTrue(result.isLeft());
    }

    @Test
    public void getRerunInfo_throws() throws SQLException {
        when(mockDataSource.getConnection())
                .thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any()))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery())
                .thenThrow(new SQLException("Oh no SQLException!"));

        Either<RejectMessage, Rerun> result = mockDatabaseRepository.getRerunInfo(UUID.randomUUID().toString());

        assertTrue(result.isLeft());
    }

//    @Test
//    public void restrictRerun_success() throws SQLException {
//        String runId = UUID.randomUUID().toString();
//        JournalItem journalItem = new JournalItem(runId, null, Instant.now(), Status.RESTRICTED, null, null);
//
//        Either<RejectMessage, SuccessMessage> result = databaseRepository.restrictRerun(journalItem);
//
//        assertTrue(result.isRight());
//    }
}
