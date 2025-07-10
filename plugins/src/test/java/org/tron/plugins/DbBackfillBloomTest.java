package org.tron.plugins;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.tron.common.utils.ByteArray;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DbTool;
import picocli.CommandLine;

public class DbBackfillBloomTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private String databaseDirectory;
  private CommandLine cli;
  private ByteArrayOutputStream outputStream;
  private ByteArrayOutputStream errorStream;
  private PrintStream originalOut;
  private PrintStream originalErr;
  private MockedStatic<DbTool> dbToolMock;

  @Before
  public void setUp() throws IOException {
    // Create temporary database directory
    databaseDirectory = temporaryFolder.newFolder().toString();

    // Create the command line interface using Toolkit as the root command
    cli = new CommandLine(new Toolkit());

    // Capture output streams
    outputStream = new ByteArrayOutputStream();
    errorStream = new ByteArrayOutputStream();
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(new PrintStream(outputStream));
    System.setErr(new PrintStream(errorStream));

    // Mock DbTool static methods
    dbToolMock = mockStatic(DbTool.class);
  }

  @After
  public void tearDown() {
    // Restore original streams
    System.setOut(originalOut);
    System.setErr(originalErr);

    // Close static mock
    if (dbToolMock != null) {
      dbToolMock.close();
    }
  }

  @Test
  public void testHelp() {
    String[] args = new String[] { "db", "backfill-bloom", "-h" };
    assertEquals(0, cli.execute(args));
  }

  @Test
  public void testValidParametersWithMockedDatabase() throws Exception {
    // Mock database interfaces
    DBInterface transactionRetDb = mock(DBInterface.class);
    DBInterface sectionBloomDb = mock(DBInterface.class);
    DBInterface blockIndexDb = mock(DBInterface.class);

    // Mock DbTool.getDB calls
    dbToolMock.when(() -> DbTool.getDB(anyString(), anyString()))
        .thenAnswer(invocation -> {
          String dbName = invocation.getArgument(1);
          switch (dbName) {
            case "transactionRetStore":
              return transactionRetDb;
            case "section-bloom":
              return sectionBloomDb;
            case "block_index":
              return blockIndexDb;
            default:
              return mock(DBInterface.class);
          }
        });

    // Mock latest block number
    when(blockIndexDb.get(any(byte[].class)))
        .thenReturn(ByteArray.fromLong(1000L));

    // Mock empty transaction data (no transactions to process)
    when(transactionRetDb.get(any(byte[].class)))
        .thenReturn(null);

    String[] args = new String[] {
        "db", "backfill-bloom",
        "-d", databaseDirectory,
        "-s", "100",
        "-e", "200",
        "-c", "2"
    };

    assertEquals(0, cli.execute(args));
  }

  @Test
  public void testInvalidStartBlock() {
    String[] args = new String[] {
        "db", "backfill-bloom",
        "-d", databaseDirectory,
        "-s", "-1"
    };
    assertEquals(1, cli.execute(args));
  }

  @Test
  public void testInvalidConcurrency() {
    String[] args = new String[] {
        "db", "backfill-bloom",
        "-d", databaseDirectory,
        "-s", "100",
        "-c", "0"
    };
    assertEquals(1, cli.execute(args));
  }

  @Test
  public void testInvalidConcurrencyTooHigh() {
    String[] args = new String[] {
        "db", "backfill-bloom",
        "-d", databaseDirectory,
        "-s", "100",
        "-c", "200"
    };
    assertEquals(1, cli.execute(args));
  }

  @Test
  public void testNonExistentDatabaseDirectory() {
    String nonExistentDir = databaseDirectory + File.separator + UUID.randomUUID();
    String[] args = new String[] {
        "db", "backfill-bloom",
        "-d", nonExistentDir,
        "-s", "100"
    };
    assertEquals(1, cli.execute(args));
  }

  @Test
  public void testEndBlockLessThanStartBlock() throws Exception {
    // Mock database interfaces
    DBInterface transactionRetDb = mock(DBInterface.class);
    DBInterface sectionBloomDb = mock(DBInterface.class);

    dbToolMock.when(() -> DbTool.getDB(anyString(), anyString()))
        .thenAnswer(invocation -> {
          String dbName = invocation.getArgument(1);
          switch (dbName) {
            case "transactionRetStore":
              return transactionRetDb;
            case "section-bloom":
              return sectionBloomDb;
            default:
              return mock(DBInterface.class);
          }
        });

    String[] args = new String[] {
        "db", "backfill-bloom",
        "-d", databaseDirectory,
        "-s", "200",
        "-e", "100"
    };
    assertEquals(1, cli.execute(args));
  }

  @Test
  public void testDatabaseInitializationFailure() {
    // Mock DbTool to throw exception
    dbToolMock.when(() -> DbTool.getDB(anyString(), anyString()))
        .thenThrow(new RuntimeException("Database connection failed"));

    String[] args = new String[] {
        "db", "backfill-bloom",
        "-d", databaseDirectory,
        "-s", "100"
    };
    assertEquals(1, cli.execute(args));
  }

  @Test
  public void testAutoDetectEndBlock() throws Exception {
    // Mock database interfaces
    DBInterface transactionRetDb = mock(DBInterface.class);
    DBInterface sectionBloomDb = mock(DBInterface.class);
    DBInterface blockIndexDb = mock(DBInterface.class);

    dbToolMock.when(() -> DbTool.getDB(anyString(), anyString()))
        .thenAnswer(invocation -> {
          String dbName = invocation.getArgument(1);
          switch (dbName) {
            case "transactionRetStore":
              return transactionRetDb;
            case "section-bloom":
              return sectionBloomDb;
            case "block_index":
              return blockIndexDb;
            default:
              return mock(DBInterface.class);
          }
        });

    // Mock latest block number
    when(blockIndexDb.get(any(byte[].class)))
        .thenReturn(ByteArray.fromLong(5000L));

    // Mock empty transaction data
    when(transactionRetDb.get(any(byte[].class)))
        .thenReturn(null);

    String[] args = new String[] {
        "db", "backfill-bloom",
        "-d", databaseDirectory,
        "-s", "100"
        // No end block specified - should auto-detect
    };

    assertEquals(0, cli.execute(args));
  }

  @Test
  public void testFailedToGetLatestBlockNumber() throws Exception {
    // Mock database interfaces
    DBInterface transactionRetDb = mock(DBInterface.class);
    DBInterface sectionBloomDb = mock(DBInterface.class);
    DBInterface blockIndexDb = mock(DBInterface.class);

    dbToolMock.when(() -> DbTool.getDB(anyString(), anyString()))
        .thenAnswer(invocation -> {
          String dbName = invocation.getArgument(1);
          switch (dbName) {
            case "transactionRetStore":
              return transactionRetDb;
            case "section-bloom":
              return sectionBloomDb;
            case "block_index":
              return blockIndexDb;
            default:
              return mock(DBInterface.class);
          }
        });

    // Mock null latest block number (failed to get)
    when(blockIndexDb.get(any(byte[].class)))
        .thenReturn(null);

    String[] args = new String[] {
        "db", "backfill-bloom",
        "-d", databaseDirectory,
        "-s", "100"
        // No end block specified - should fail to auto-detect
    };

    assertEquals(1, cli.execute(args));
  }

  @Test
  public void testDefaultParameters() throws Exception {
    // Mock database interfaces
    DBInterface transactionRetDb = mock(DBInterface.class);
    DBInterface sectionBloomDb = mock(DBInterface.class);
    DBInterface blockIndexDb = mock(DBInterface.class);

    dbToolMock.when(() -> DbTool.getDB(anyString(), anyString()))
        .thenAnswer(invocation -> {
          String dbName = invocation.getArgument(1);
          switch (dbName) {
            case "transactionRetStore":
              return transactionRetDb;
            case "section-bloom":
              return sectionBloomDb;
            case "block_index":
              return blockIndexDb;
            default:
              return mock(DBInterface.class);
          }
        });

    // Mock latest block number
    when(blockIndexDb.get(any(byte[].class)))
        .thenReturn(ByteArray.fromLong(1000L));

    // Mock empty transaction data
    when(transactionRetDb.get(any(byte[].class)))
        .thenReturn(null);

    // Test with default database directory
    String[] args = new String[] {
        "db", "backfill-bloom",
        "-s", "100",
        "-e", "200"
    };

    // This should fail because default directory doesn't exist
    assertEquals(1, cli.execute(args));
  }
}
