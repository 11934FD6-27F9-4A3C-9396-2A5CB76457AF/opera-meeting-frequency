package omegapoint.opera.operationaljournal.infrastructure;

import io.vavr.control.Either;
import omegapoint.opera.logging.RejectMessage;
import omegapoint.opera.logging.SuccessMessage;
import omegapoint.opera.operationaljournal.api.model.request.Checkpoint;
import omegapoint.opera.operationaljournal.api.model.request.StartBody;
import omegapoint.opera.operationaljournal.api.model.request.StartRequest;
import omegapoint.opera.operationaljournal.domain.OperationalJournalService;
import omegapoint.opera.operationaljournal.domain.model.BlobReference;
import omegapoint.opera.operationaljournal.domain.model.CheckpointType;
import omegapoint.opera.operationaljournal.domain.model.Status;
import omegapoint.opera.operationaljournal.domain.model.table.ConflictItem;
import omegapoint.opera.operationaljournal.domain.model.table.JournalErrorItem;
import omegapoint.opera.operationaljournal.domain.model.table.JournalItem;
import omegapoint.opera.operationaljournal.domain.model.table.Rerun;
import omegapoint.opera.operationaljournal.infrastructure.model.RunItem;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DatabaseRepositoryTestNew {

    private DataSource dataSource = mock(DataSource.class);
    private Connection connection = mock(Connection.class);
    private PreparedStatement preparedStatement = mock(PreparedStatement.class);
    private ResultSet resultSet = mock(ResultSet.class);
    private Logger logger = mock(Logger.class);

    private DatabaseRepository databaseRepository;

    @BeforeEach
    void setUp() throws SQLException {
        databaseRepository = new DatabaseRepository(dataSource, logger);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        if(testInfo.getTags().contains("SkipCleanup")) {
            return;
        }

        verifyNoMoreInteractions(dataSource);
        verifyNoMoreInteractions(connection);
        verifyNoMoreInteractions(preparedStatement);
        verifyNoMoreInteractions(resultSet);
        verifyNoMoreInteractions(logger);
    }

    @Test
    void putStart_returns_success() throws SQLException {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("AffectedRows")).thenReturn(1);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putStart(getStartRequest());

        assertTrue(result.isRight());
        assertEquals("Rows successfully inserted.", result.get().message());

        verifyCorrectDataHasBeenRunWhenPutStartCalled();
        verify(resultSet).getInt("AffectedRows");
        verify(logger).info("Rows successfully inserted. Rows affected: 1");
    }

    @Test
    void putStart_returns_reject_when_run_already_started() throws SQLException {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("AffectedRows")).thenReturn(0);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putStart(getStartRequest());

        assertTrue(result.isLeft());
        assertEquals("Run has already been started.", result.getLeft().message());

        verifyCorrectDataHasBeenRunWhenPutStartCalled();
        verify(resultSet).getInt("AffectedRows");
        verify(logger).warning("Condition in SQL were false, no records inserted.");
    }

    @Test
    void putStart_returns_reject_when_next_from_resultSet_is_false() throws SQLException {
        when(resultSet.next()).thenReturn(false);
        when(resultSet.getInt("AffectedRows")).thenReturn(0);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putStart(getStartRequest());

        assertTrue(result.isLeft());
        assertEquals("Database query failed.", result.getLeft().message());

        verifyCorrectDataHasBeenRunWhenPutStartCalled();
    }

    @Test
    void putStart_returns_reject_with_SQLException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putStart(getStartRequest());

        assertTrue(result.isLeft());
        assertEquals("Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
        verify(logger).severe("Database connection error");
    }

    @Test
    void putStop_returns_success() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putStop(getJournalItem());

        assertTrue(result.isRight());
        assertEquals("Row successfully inserted.", result.get().message());

        verifyCorrectDataHasBeenRunWhenPutStopCalled(2);
        verify(resultSet).getInt(1);
    }

    @Test
    void putStop_returns_success_with_duplicate_records() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(2);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putStop(getJournalItem());

        assertTrue(result.isRight());
        assertEquals("Successful insert with duplicate records.", result.get().message());

        verifyCorrectDataHasBeenRunWhenPutStopCalled(2);
        verify(resultSet).getInt(1);
        verify(logger).severe("Found duplicate records of Journal for runId: 1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(logger).warning("Number of records: 2");
    }

    @Test
    void putStop_returns_reject_when_next_from_resultSet_is_false() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(resultSet.next()).thenReturn(false);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putStop(getJournalItem());

        assertTrue(result.isLeft());
        assertEquals("Failed to count existing records.", result.getLeft().message());

        verifyCorrectDataHasBeenRunWhenPutStopCalled(2);
    }

    @Test
    void putStop_returns_reject_when_executeUpdate_affects_more_than_one_row() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(2);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putStop(getJournalItem());

        assertTrue(result.isLeft());
        assertEquals("Insertion failed without SQLException.", result.getLeft().message());

        verifyCorrectDataHasBeenRunWhenPutStopCalled(1);

        verify(logger).severe("Insertion failed without SQLException.");
    }

    @Test
    void putStop_returns_reject_with_SQLException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putStop(getJournalItem());

        assertTrue(result.isLeft());
        assertEquals("Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
        verify(logger).severe("Database connection error");
    }

    @Test
    void putRestart_returns_success() throws SQLException {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("ResultCode")).thenReturn(0);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putRestart(getJournalItem());

        assertTrue(result.isRight());
        assertEquals("Row successfully inserted", result.get().message());

        verifyCorrectDataHasBeenRunWhenPutRestartCalled(1);
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void putRestart_returns_reject(final boolean hasNext, final String message, final int value) throws SQLException {
        when(resultSet.next()).thenReturn(hasNext);
        if (value != 4) {
            when(resultSet.getInt("ResultCode")).thenReturn(value);
        }

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putRestart(getJournalItem());

        assertTrue(result.isLeft());
        assertEquals(message, result.getLeft().message());

        verifyCorrectDataHasBeenRunWhenPutRestartCalled(value);
    }

    @Test
    void putRestart_returns_reject_with_SQLException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putRestart(getJournalItem());

        assertTrue(result.isLeft());
        assertEquals("Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
        verify(logger).severe("Database connection error");
    }

    @Test
    void putRunAndQueueCheckpoint_returns_success() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(1);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putRunAndQueueCheckpoint(getStartRequest().toRunItem());

        assertTrue(result.isRight());
        assertEquals("Rows successfully inserted", result.get().message());

        verify(connection).prepareStatement("INSERT INTO OperationsJournal.QueueCheckpoint (RunID, QueueName, BlobContainerName, BlobPath) VALUES (?, ?, ?, ?)");
        verify(preparedStatement).setString(4, "path");
        verifyCorrectDataHasBeenRunWhenPutRunAndQueueCheckpointCalled(2);
    }

    @Test
    void putRunAndQueueCheckpoint_returns_reject() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(2);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putRunAndQueueCheckpoint(getStartRequest().toRunItem());

        assertTrue(result.isLeft());
        assertEquals("Inserting the database row failed.", result.getLeft().message());

        verifyCorrectDataHasBeenRunWhenPutRunAndQueueCheckpointCalled(1);
    }

    @Test
    void putRunAndQueueCheckpoint_returns_reject_could_write_to_database() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(1).thenReturn(0);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putRunAndQueueCheckpoint(getStartRequest().toRunItem());

        assertTrue(result.isLeft());
        assertEquals("Could not write to database", result.getLeft().message());

        verify(connection).prepareStatement("INSERT INTO OperationsJournal.QueueCheckpoint (RunID, QueueName, BlobContainerName, BlobPath) VALUES (?, ?, ?, ?)");
        verify(preparedStatement).setString(4, "path");
        verifyCorrectDataHasBeenRunWhenPutRunAndQueueCheckpointCalled(2);
    }

    @Test
    void putJournal_returns_success() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(1);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putJournal(getJournalItem());

        assertTrue(result.isRight());
        assertEquals("Row successfully inserted.", result.get().message());

        verifyCorrectDataHasBeenRunWhenPutJournal();
    }

    @Test
    void putJournal_returns_reject() throws SQLException {
        when(preparedStatement.executeUpdate()).thenReturn(2);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putJournal(getJournalItem());

        assertTrue(result.isLeft());
        assertEquals("Inserting the database row failed.", result.getLeft().message());

        verifyCorrectDataHasBeenRunWhenPutJournal();
    }

    @Test
    void putJournal_returns_reject_SQLException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putJournal(getJournalItem());

        assertTrue(result.isLeft());
        assertEquals("Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
    }

    @ParameterizedTest
    @MethodSource("provideParametersForPutConflictSuccess")
    void putConflict_returns_success(final String message, final int value) throws SQLException {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("ResultCode")).thenReturn(value);

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putConflict(getConflictItem());

        assertTrue(result.isRight());
        assertEquals(message, result.get().message());

        verify(dataSource).getConnection();
        verify(connection).prepareStatement(getSqlStatementForPutConflict());

        verifyCorrectDataHasBeenRunWhenPutConflictCalled(1);

        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
        verify(resultSet).getInt("ResultCode");
        verify(connection).close();
    }

    @ParameterizedTest
    @MethodSource("provideParametersForPutConflictReject")
    void putConflict_returns_reject(final boolean hasNext, final String message, final int value) throws SQLException {
        when(resultSet.next()).thenReturn(hasNext);
        if (value != 3) {
            when(resultSet.getInt("ResultCode")).thenReturn(value);
        }

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putConflict(getConflictItem());

        assertTrue(result.isLeft());
        assertEquals(message, result.getLeft().message());

        verify(dataSource).getConnection();
        verify(connection).prepareStatement(getSqlStatementForPutConflict());

        verifyCorrectDataHasBeenRunWhenPutConflictCalled(1);

        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
        verify(connection).close();

        if (value != 3) {
            verify(resultSet).getInt("ResultCode");
        }
        if (value == 2) {
            verify(logger).severe("Unknown error in putConflict.");
        }
    }

    @Test
    void putConflict_returns_reject_with_SQLException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, SuccessMessage> result = databaseRepository.putConflict(getConflictItem());

        assertTrue(result.isLeft());
        assertEquals("Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
    }

    @Test
    void getExistingRun_returns_success() throws SQLException {
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        givenCorrectDataForGetExistingRun();

        final Either<RejectMessage, RunItem[]> result = databaseRepository.getExistingRun("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");

        assertTrue(result.isRight());
        assertEquals(extractedRunItem(), result.get()[0]);

        verify(connection).prepareStatement("""
                SELECT * FROM OperationsJournal.Run
                WHERE RunID = ?
                """);
        verifyCorrectDataForGetExistingRun();
    }

    @Test
    void getExistingRun_returns_reject() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, RunItem[]> result = databaseRepository.getExistingRun("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");

        assertTrue(result.isLeft());
        assertEquals("Failed to check for existing Runs. DatabaseRepository: Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
    }

    @Test
    void getExistingJournal_returns_success() throws SQLException {
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        givenDataForGetExistingJournal();

        final Either<RejectMessage, omegapoint.opera.operationaljournal.infrastructure.model.JournalItem[]> result = databaseRepository.getExistingJournal("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");

        assertTrue(result.isRight());
        assertEquals(expectedJournalItem(), result.get()[0]);

        verifyCorrectDataHasBeenRunForGetExistingJournal();
    }

    @Test
    void getExistingJournal_returns_reject() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, omegapoint.opera.operationaljournal.infrastructure.model.JournalItem[]> result = databaseRepository.getExistingJournal("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");

        assertTrue(result.isLeft());
        assertEquals("Failed to check for existing Journal rows. DatabaseRepository: Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
    }

    @Test
    void getReruns_returns_success() throws SQLException {
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        givenDataFromResultSet(1);

        System.out.println(Instant.now());
        final Either<RejectMessage, List<Rerun>> result = databaseRepository.getReruns();

        assertTrue(result.isRight());
        assertEquals(expectedRerun(), result.get().get(0));

        verify(connection).prepareStatement(getSqlStringForGetReruns());
        verifyData(2);
    }

    @Test
    void getReruns_returns_reject() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, List<Rerun>> result = databaseRepository.getReruns();

        assertTrue(result.isLeft());
        assertEquals("DatabaseRepository: Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
    }

    @Test
    void getRerunInfo_returns_success() throws SQLException {
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        givenDataFromResultSet(1);

        final Either<RejectMessage, Rerun> result = databaseRepository.getRerunInfo("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");

        assertTrue(result.isRight());
        assertEquals(expectedRerun(), result.get());

        verify(connection).prepareStatement(getSqlStringForGetRerunInfo());
        verify(preparedStatement).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verifyData(2);
    }

    @Test
    void getRerunInfo_returns_reject_when_journal_is_empty() throws SQLException {
        when(resultSet.next()).thenReturn(false);

        final Either<RejectMessage, Rerun> result = databaseRepository.getRerunInfo("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");

        assertTrue(result.isLeft());
        assertEquals("The journal does not contain any Run with the RunID '1bfeb4b7-f188-4763-a2dd-a4d65ba85a42'.", result.getLeft().message());

        verify(dataSource).getConnection();
        verify(connection).prepareStatement(getSqlStringForGetRerunInfo());
        verify(preparedStatement).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
        verify(logger).warning("The journal does not contain any Run with the RunID '1bfeb4b7-f188-4763-a2dd-a4d65ba85a42'.");
        verify(connection).close();
    }

    @Test
    void getRerunInfo_returns_reject_when_multiple_runs_has_same_id() throws SQLException {
        when(resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        givenDataFromResultSet(2);

        final Either<RejectMessage, Rerun> result = databaseRepository.getRerunInfo("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");

        assertTrue(result.isLeft());
        assertEquals("Multiple Runs found in journal with runId: '1bfeb4b7-f188-4763-a2dd-a4d65ba85a42'.", result.getLeft().message());

        verify(connection).prepareStatement(getSqlStringForGetRerunInfo());
        verify(preparedStatement).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(logger).warning("Multiple Runs found in journal with runId: '1bfeb4b7-f188-4763-a2dd-a4d65ba85a42'.");

        verifyData(3);
    }


    @Test
    void getCinodeReruns_returns_reject_with_SQLException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, Rerun> result = databaseRepository.getRerunInfo("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");

        assertTrue(result.isLeft());
        assertEquals("DatabaseRepository: Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
    }

    @Test
    void getCinodeReruns_returns_success() throws SQLException {
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        givenDataFromResultSet(1);

        final Either<RejectMessage, omegapoint.opera.operationaljournal.infrastructure.model.Rerun[]> result = databaseRepository.getCinodeReruns();

        assertTrue(result.isRight());
        assertEquals(expectedRerun(), result.get()[0].toDomain());

        verify(connection).prepareStatement(getSqlStringForGetCinodeReruns());
        verifyData(2);
    }

    @Test
    void getCinodeReruns_returns_reject() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        final Either<RejectMessage, omegapoint.opera.operationaljournal.infrastructure.model.Rerun[]> result = databaseRepository.getCinodeReruns();

        assertTrue(result.isLeft());
        assertEquals("DatabaseRepository: Database connection error", result.getLeft().message());

        verify(dataSource).getConnection();
    }

    @Test
    @Tag("SkipCleanup")
    void getRootRunId_happy_case_with_recursion() throws SQLException {
        when(resultSet.next()).thenReturn(true, true, true, true);
        when(resultSet.getString("ParentID"))
                .thenReturn("cca16f71-bee6-4f77-acce-a278f6443cfe",
                        "f875587e-1baf-4a52-82fc-fcf42d0045bb",
                        "d777dd3c-f8b6-480a-bcfb-cc51ee6ae86d",
                        null);

        UUID input = UUID.fromString("e84a10bc-8c16-445d-89ee-1d356bc02da8");
        var result = databaseRepository.getRootRunId(input);

        assertNotNull(result);
        assertTrue(result.isRight());
        assertEquals("d777dd3c-f8b6-480a-bcfb-cc51ee6ae86d", result.get().get().toString());
    }

    @Test
    @Tag("SkipCleanup")
    void getRootRunId_no_runid_found() throws SQLException {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("ParentID"))
                .thenReturn(null);

        UUID input = UUID.fromString("e84a10bc-8c16-445d-89ee-1d356bc02da8");
        var result = databaseRepository.getRootRunId(input);

        assertNotNull(result);
        assertTrue(result.isRight());
        assertTrue(result.get().isEmpty());
    }

    @Test
    @Tag("SkipCleanup")
    void getRootRunId_connection_error() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection error"));

        UUID input = UUID.fromString("e84a10bc-8c16-445d-89ee-1d356bc02da8");
        var result = databaseRepository.getRootRunId(input);

        assertNotNull(result);
        assertTrue(result.isLeft());
    }
    @Test
    void testGetRunsWithExhaustedRetriesFromLast24Hours() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(mockResultSet.getString("RunID")).thenReturn("e6b93799-7471-4218-8cad-075779360cea", "321");
        when(mockResultSet.getInt("Attempt")).thenReturn(6, 6);
        when(mockResultSet.getString("Timestamp")).thenReturn("2024-12-05 06:03:38.3451844", "2024-11-30 05:00:08.7944345");
        when(mockResultSet.getString("Status")).thenReturn("ERROR", "STARTED");
        when(mockResultSet.getString("Message")).thenReturn("Error getUser: RetryExhaustedException Retries exhausted: 2/2", "Timeout occurred");
        when(mockResultSet.getString("OriginFunction")).thenReturn("source-system-function-app", "functionB");
        when(mockResultSet.getString("WebhookStep")).thenReturn("UPDATE_CINODE_ABSENCE_WITH_EMPLOYMENT_START_DATE", "step2");


        Logger logger = mock(Logger.class);
        DatabaseRepository repository = new DatabaseRepository(dataSource, logger);
        Either<RejectMessage, List<JournalErrorItem>> actual = repository.getRunsWithExhaustedRetriesFromLast24Hours();
        List<JournalErrorItem> expected = List.of(new JournalErrorItem(
                "e6b93799-7471-4218-8cad-075779360cea",
                        6,
                        "2024-12-05 06:03:38.3451844",
                        "ERROR",
                        "Error getUser: RetryExhaustedException Retries exhausted: 2/2",
                        "source-system-function-app",
                        "UPDATE_CINODE_ABSENCE_WITH_EMPLOYMENT_START_DATE",
                "87b93799-7471-4218-8cad-075779360cea"),
                new JournalErrorItem("321",
                        6,
                        "2024-11-30 05:00:08.7944345",
                        "STARTED",
                        "Timeout occurred",
                        "functionB",
                        "step2",
                        "87b93799-7471-4218-8cad-075779360cea")

                );
        assertTrue(actual.isRight());
        assertEquals(expected,actual.get());

    }


    private void verifyCorrectDataHasBeenRunForGetExistingJournal() throws SQLException {
        verify(connection).prepareStatement("""
                SELECT * FROM OperationsJournal.Journal
                WHERE RunID = ?
                """);
        verify(dataSource).getConnection();
        verify(preparedStatement).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).executeQuery();
        verify(resultSet, times(2)).next();
        verify(resultSet).getString("RunID");
        verify(resultSet).getInt("Attempt");
        verify(resultSet).getString("Timestamp");
        verify(resultSet).getString("Status");
        verify(resultSet).getString("Message");
        verify(resultSet).getString("OriginFunction");
        verify(connection).close();
    }

    private void verifyData(final int times) throws SQLException {
        verify(dataSource).getConnection();
        verify(preparedStatement).executeQuery();
        verify(resultSet, times(times)).next();
        verify(resultSet, times(times - 1)).getString("RunID");
        verify(resultSet, times(times - 1)).getInt("Attempt");
        verify(resultSet, times(times - 1)).getString("WebhookStep");
        verify(resultSet, times(times - 1)).getString("OriginTimestamp");
        verify(resultSet, times(times - 1)).getString("QueueName");
        verify(resultSet, times(times - 1)).getString("BlobContainerName");
        verify(resultSet, times(times - 1)).getString("BlobPath");
        verify(connection).close();
    }

    private void verifyCorrectDataForGetExistingRun() throws SQLException {
        verify(dataSource).getConnection();
        verify(preparedStatement).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).executeQuery();
        verify(resultSet, times(2)).next();
        verify(resultSet).getString("RunID");
        verify(resultSet).getString("ParentID");
        verify(resultSet).getString("OriginTimestamp");
        verify(resultSet).getString("WebhookStep");
        verify(connection).close();
    }

    private void verifyCorrectDataHasBeenRunWhenPutConflictCalled(final int i) throws SQLException {
        verify(preparedStatement).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(2, "conflict.id");
        verify(preparedStatement).setString(3, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(4, "conflict.id");
    }

    private void verifyCorrectDataHasBeenRunWhenPutJournal() throws SQLException {
        verify(dataSource).getConnection();
        verify(connection).prepareStatement("INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) VALUES (?, ?, ?, ?, ?, ?)");
        verify(preparedStatement).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(eq(2), anyString());
        verify(preparedStatement).setString(eq(3), anyString());
        verify(preparedStatement).setString(4, "message");
        verify(preparedStatement).setString(5, "originFunction");
        verify(preparedStatement).setString(6, "1");
        verify(preparedStatement).executeUpdate();
        verify(connection).close();
    }

    private void verifyCorrectDataHasBeenRunWhenPutRunAndQueueCheckpointCalled(final int times) throws SQLException {
        verify(dataSource, times(times)).getConnection();
        verify(connection).prepareStatement("INSERT INTO OperationsJournal.Run (RunID, ParentID, OriginTimestamp, WebhookStep) VALUES (?, ?, ?, ?)");
        verify(preparedStatement, times(times)).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement, times(times)).setString(eq(2), anyString());
        verify(preparedStatement, times(times)).setString(eq(3), anyString());
        verify(preparedStatement).setString(4, "webhookStep");
        verify(preparedStatement, times(times)).executeUpdate();
        verify(connection, times(times)).close();
    }

    private void verifyCorrectDataHasBeenRunWhenPutRestartCalled(final int value) throws SQLException {
        verify(dataSource).getConnection();
        verify(connection).prepareStatement(getSqlStatementForPutReStart());
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
        verify(preparedStatement).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(2, "1");
        verify(preparedStatement).setString(3, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(4, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(eq(5), anyString());
        verify(preparedStatement).setString(6, "RESTARTED");
        verify(preparedStatement).setString(7, "message");
        verify(preparedStatement).setString(8, "originFunction");
        verify(preparedStatement).setString(9, "1");

        if (value != 4) {
            verify(resultSet).getInt("ResultCode");
        }
        if (value == 3) {
            verify(logger).severe("Unknown error in putRestart.");
        }
        verify(connection).close();
    }

    private void verifyCorrectDataHasBeenRunWhenPutStartCalled() throws SQLException {
        verify(dataSource).getConnection();
        verify(connection).prepareStatement(getSqlStatementForPutStart());
        verify(preparedStatement).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(2, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(3, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(4, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(eq(5), anyString());
        verify(preparedStatement).setString(6, "webhookStep");
        verify(preparedStatement).setString(7, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(eq(8), anyString());
        verify(preparedStatement).setString(9, "STARTED");
        verify(preparedStatement).setString(10, null);
        verify(preparedStatement).setString(11, "originFunction");
        verify(preparedStatement).setString(12, "1");
        verify(preparedStatement).setString(13, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement).setString(14, "checkPointPath");
        verify(preparedStatement).setString(15, "containerName");
        verify(preparedStatement).setString(16, "path");
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
        verify(connection).close();
    }

    private void givenDataForGetExistingJournal() throws SQLException {
        when(resultSet.getString("RunID")).thenReturn("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        when(resultSet.getInt("Attempt")).thenReturn(1);
        when(resultSet.getString("Timestamp")).thenReturn(Instant.parse("2024-08-12T06:27:42.045903Z").toString());
        when(resultSet.getString("Status")).thenReturn(Status.SUCCESS.toString());
        when(resultSet.getString("Message")).thenReturn("message");
        when(resultSet.getString("OriginFunction")).thenReturn("originFunction");
    }

    private void givenDataFromResultSet(final int times) throws SQLException {
        if (times == 2) {
            when(resultSet.getString("RunID"))
                    .thenReturn("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42")
                    .thenReturn("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        } else {
            when(resultSet.getString("RunID")).thenReturn("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        }
        when(resultSet.getInt("Attempt")).thenReturn(1);
        when(resultSet.getString("WebhookStep")).thenReturn("webhookStep");
        when(resultSet.getString("OriginTimestamp")).thenReturn(Instant.parse("2024-08-12T06:27:42.045903Z").toString());
        when(resultSet.getString("QueueName")).thenReturn("queueName");
        when(resultSet.getString("BlobContainerName")).thenReturn("blobContainerName");
        when(resultSet.getString("BlobPath")).thenReturn("blobPath");
    }

    private void givenCorrectDataForGetExistingRun() throws SQLException {
        when(resultSet.getString("RunID")).thenReturn("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        when(resultSet.getString("ParentID")).thenReturn("1bfeb4b7-f188-4763-a2dd-a4d65ba85a33");
        when(resultSet.getString("OriginTimestamp")).thenReturn("2024-08-12T06:27:42.045903Z");
        when(resultSet.getString("WebhookStep")).thenReturn("webhookStep");
        when(resultSet.getString("ConflictID")).thenReturn("conflictID");
    }

    private static String getSqlStringForGetRerunInfo() {
        return """
                SELECT run.RunID, Attempt, WebhookStep, OriginTimestamp, QueueName, BlobContainerName, BlobPath
                FROM OperationsJournal.MostRecentJournal mrj
                         INNER JOIN OperationsJournal.Run run ON run.RunID = mrj.RunID
                         INNER JOIN OperationsJournal.QueueCheckpoint qc ON qc.RunID = run.RunID
                WHERE run.RunID = ?
                """;
    }

    private static String getSqlStringForGetCinodeReruns() {
        return """
                SELECT run.RunID, Attempt, WebhookStep, OriginTimestamp, QueueName, BlobContainerName, BlobPath
                FROM OperationsJournal.MostRecentJournal mrj
                        INNER JOIN OperationsJournal.Run run ON run.RunID = mrj.RunID
                        INNER JOIN OperationsJournal.QueueCheckpoint qc ON qc.RunID = run.RunID
                WHERE WebhookStep IN ('UPDATE_CINODE_ABSENCE_WITH_EMPLOYMENT_START_DATE', 'NEW_EMPLOYEE_ABSENCE_IN_CINODE') AND
                    (Status = 'ERROR' OR
                    (Status IN ('STARTED', 'RESTARTED') AND
                        DATEDIFF(minute, Timestamp, SYSUTCDATETIME()) > 10))
                ORDER BY OriginTimestamp
                """;
    }

    private static String getSqlStringForGetReruns() {
        return """
                SELECT run.RunID, Attempt, WebhookStep, run.OriginTimestamp, QueueName, BlobContainerName, BlobPath
                FROM OperationsJournal.ForRerun_Rejected rej\s
                            INNER JOIN OperationsJournal.Run run ON rej.RunID = run.RunID
                            INNER JOIN OperationsJournal.MostRecentJournal mrj ON rej.RunID = mrj.RunID
                            INNER JOIN OperationsJournal.QueueCheckpoint qc ON rej.RunID = qc.RunID
                """;
    }

    private ConflictItem getConflictItem() {
        return new ConflictItem("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42", "conflict.id");
    }

    private static Stream<Arguments> provideParametersForPutConflictSuccess() {
        return Stream.of(
                Arguments.of("Row successfully inserted", 0),
                Arguments.of("Record already exists", 1)
        );
    }

    private static Stream<Arguments> provideParametersForPutConflictReject() {
        return Stream.of(
                Arguments.of(true, "Unknown error occurred.", 2), //2 can be changed to any value but not 0 or 1, this is necessary for verification
                Arguments.of(false, "Writing to database failed.", 3) //3 can be changed to any value
        );
    }

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
                Arguments.of(true, "Could not insert the row, restart with same attempt already exists.", 1),
                Arguments.of(true, "Run does not exist.", 2),
                Arguments.of(true, "Unknown error occurred.", 3), //3 can be changed to any value but not 1, 2 or 4, this is necessary for verification
                Arguments.of(false, "Writing to database failed.", 4) //4 can be changed to any value but not 1, 2 or 3
        );
    }

    private JournalItem getJournalItem() {
        final JournalItem journalItem = new JournalItem(
                "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42",
                1,
                Instant.now(),
                Status.RESTARTED,
                "message",
                "originFunction");
        return journalItem;
    }

    private void verifyCorrectDataHasBeenRunWhenPutStopCalled(final int num) throws SQLException {
        verify(preparedStatement).executeUpdate();
        verify(preparedStatement, times(num)).setString(1, "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42");
        verify(preparedStatement, times(num)).setString(eq(2), anyString());
        verify(preparedStatement).setString(3, "RESTARTED");
        verify(preparedStatement).setString(4, "message");
        verify(preparedStatement).setString(5, "originFunction");
        verify(preparedStatement).setString(6, "1");

        verify(dataSource).getConnection();
        verify(connection).prepareStatement("""
                INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) \
                VALUES (?, ?, ?, ?, ?, ?);
                """);
        if(num == 2) {
            verify(connection).prepareStatement("SELECT COUNT(*) FROM OperationsJournal.Journal WHERE RunID = ? AND Status = ? AND Attempt = ?;\n");
            verify(preparedStatement).setString(3, "1");
            verify(preparedStatement).executeQuery();
            verify(resultSet).next();
        }
        verify(connection).close();
    }

    private static RunItem extractedRunItem() {
        return new RunItem("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42",
                "1bfeb4b7-f188-4763-a2dd-a4d65ba85a33",
                "2024-08-12T06:27:42.045903Z",
                "webhookStep",
                null);
    }

    private static omegapoint.opera.operationaljournal.infrastructure.model.JournalItem expectedJournalItem() {
        return new omegapoint.opera.operationaljournal.infrastructure.model.JournalItem(
                "1bfeb4b7-f188-4763-a2dd-a4d65ba85a42",
                1,
                "2024-08-12T06:27:42.045903Z",
                "SUCCESS",
                "message",
                "originFunction"
        );
    }

    private static Rerun expectedRerun() {
        return new Rerun(UUID.fromString("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42"),
                1,
                new omegapoint.opera.operationaljournal.domain.model.WebhookStep("webhookStep"),
                ZonedDateTime.parse("2024-08-12T06:27:42.045903Z"),
                "queueName",
                new BlobReference("blobContainerName", "blobPath"));
    }

    private String getSqlStatementForPutConflict() {
        return """
                BEGIN TRANSACTION;
                DECLARE @ResultCode INT = 0;
                IF NOT EXISTS (SELECT * FROM OperationsJournal.Conflict WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ? AND ConflictID = ?)
                BEGIN
                    INSERT INTO OperationsJournal.Conflict (RunID, ConflictID) VALUES (?, ?)
                END
                ELSE
                BEGIN
                    SET @ResultCode = 1; -- Record already exists
                END;
                COMMIT TRANSACTION;
                SELECT @ResultCode AS ResultCode;
                """;
    }

    private String getSqlStatementForPutReStart() {
        return """
                BEGIN TRANSACTION;
                DECLARE @ResultCode INT = 0;
                IF NOT EXISTS (SELECT * FROM OperationsJournal.Journal WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ? AND Attempt = ?)
                BEGIN
                    IF EXISTS (SELECT * FROM OperationsJournal.Run WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ?)
                    BEGIN
                        INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) 
                        VALUES (?, ?, ?, ?, ?, ?);
                    END
                    ELSE
                    BEGIN
                        SET @ResultCode = 2; -- RunID does not exist
                    END;
                END
                ELSE
                BEGIN
                    SET @ResultCode = 1; -- Record already exists
                END;
                COMMIT TRANSACTION;
                SELECT @ResultCode AS ResultCode;
                """;
    }

    private String getSqlStatementForPutStart() {
        return """
                BEGIN TRANSACTION;
                                    
                DECLARE @AffectedRows INT = 0;
                                    
                IF NOT EXISTS (SELECT * FROM OperationsJournal.Journal WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ?)
                AND NOT EXISTS (SELECT * FROM OperationsJournal.Run WITH (UPDLOCK, HOLDLOCK) WHERE RunID = ?)
                BEGIN
                    INSERT INTO OperationsJournal.Run (RunID, ParentID, OriginTimestamp, WebhookStep) VALUES (?, ?, ?, ?);
                    SET @AffectedRows = @AffectedRows + @@ROWCOUNT;
                    
                    INSERT INTO OperationsJournal.Journal (RunID, Timestamp, Status, Message, OriginFunction, Attempt) VALUES (?, ?, ?, ?, ?, ?);
                    SET @AffectedRows = @AffectedRows + @@ROWCOUNT;
                    
                    INSERT INTO OperationsJournal.QueueCheckpoint (RunID, QueueName, BlobContainerName, BlobPath) VALUES (?, ?, ?, ?);
                    SET @AffectedRows = @AffectedRows + @@ROWCOUNT;
                END;
                                    
                COMMIT TRANSACTION;
                                    
                SELECT @AffectedRows AS AffectedRows;
                """;
    }

    private StartRequest getStartRequest() {
        return new StartRequest("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42",
                new StartBody(UUID.fromString("1bfeb4b7-f188-4763-a2dd-a4d65ba85a42"),
                        new BlobReference("containerName", "path"),
                        "webhookStep",
                        ZonedDateTime.now().toString(),
                        ZonedDateTime.now().toString(),
                        "conflictId",
                        "originFunction",
                        new Checkpoint("checkPointPath", CheckpointType.QUEUE.toString()),
                        "operationType",
                        null));
    }
}
