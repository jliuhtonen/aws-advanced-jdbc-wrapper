/*
 * AWS JDBC Proxy Driver
 * Copyright Amazon.com Inc. or affiliates.
 * See the LICENSE file in the project root for more information.
 */

package integration.container.standard.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import org.junit.jupiter.api.Test;
import software.aws.rds.jdbc.proxydriver.plugin.ExecutionTimeConnectionPluginFactory;
import software.aws.rds.jdbc.proxydriver.profile.DriverConfigurationProfiles;
import software.aws.rds.jdbc.proxydriver.wrapper.ConnectionWrapper;
import software.aws.rds.jdbc.proxydriver.wrapper.ResultSetWrapper;
import software.aws.rds.jdbc.proxydriver.wrapper.StatementWrapper;

public class StandardMysqlIntegrationTest extends StandardMysqlBaseTest {

  @Test
  public void test_connect() throws SQLException, IOException {
    try (Connection conn = connect()) {
      Statement stmt = conn.createStatement();
      stmt.executeQuery("SELECT 1");
      ResultSet rs = stmt.getResultSet();
      rs.next();
      assertEquals(1, rs.getInt(1));
    }

    try (Connection conn = connectToProxy()) {
      assertTrue(conn.isValid(5));
      containerHelper.disableConnectivity(proxy);
      assertFalse(conn.isValid(5));
      containerHelper.enableConnectivity(proxy);
    }
  }

  @Test
  public void testOpenConnection() throws SQLException {

    Properties props = initDefaultPropsNoTimeouts();
    props.setProperty("proxyDriverPlugins", "executionTime");

    Connection conn = DriverManager.getConnection(getUrl(), props);

    assertTrue(conn instanceof ConnectionWrapper);
    assertTrue(conn.isWrapperFor(com.mysql.cj.jdbc.ConnectionImpl.class));

    assertTrue(conn.isValid(10));

    Statement statement = conn.createStatement();
    assertNotNull(statement);
    assertTrue(statement instanceof StatementWrapper);
    assertTrue(statement.isWrapperFor(com.mysql.cj.jdbc.StatementImpl.class));

    int rnd = new Random().nextInt(100);
    ResultSet resultSet = statement.executeQuery("SELECT " + rnd);
    assertNotNull(resultSet);
    assertTrue(resultSet instanceof ResultSetWrapper);
    assertTrue(resultSet.isWrapperFor(com.mysql.cj.jdbc.result.ResultSetImpl.class));

    resultSet.next();
    int result = resultSet.getInt(1);
    assertEquals(rnd, result);

    conn.close();
  }

  @Test
  public void testOpenConnectionWithUnknownProfile() {

    Properties props = initDefaultPropsNoTimeouts();
    props.setProperty("proxyDriverProfileName", "unknownProfile");

    SQLException actualException = assertThrows(SQLException.class, () -> {
      DriverManager.getConnection(getUrl(), props);
    });

    assertTrue(actualException.getMessage().contains("unknownProfile"));
  }

  @Test
  public void testOpenConnectionWithProfile() throws SQLException {

    Properties props = initDefaultPropsNoTimeouts();
    props.setProperty("proxyDriverProfileName", "testProfile");

    DriverConfigurationProfiles.clear();
    DriverConfigurationProfiles.addOrReplaceProfile("testProfile",
        Arrays.asList(ExecutionTimeConnectionPluginFactory.class));

    Connection conn = DriverManager.getConnection(getUrl(), props);

    assertTrue(conn instanceof ConnectionWrapper);
    assertTrue(conn.isWrapperFor(com.mysql.cj.jdbc.ConnectionImpl.class));

    assertTrue(conn.isValid(10));

    Statement statement = conn.createStatement();
    assertNotNull(statement);
    assertTrue(statement instanceof StatementWrapper);
    assertTrue(statement.isWrapperFor(com.mysql.cj.jdbc.StatementImpl.class));

    int rnd = new Random().nextInt(100);
    ResultSet resultSet = statement.executeQuery("SELECT " + rnd);
    assertNotNull(resultSet);
    assertTrue(resultSet instanceof ResultSetWrapper);
    assertTrue(resultSet.isWrapperFor(com.mysql.cj.jdbc.result.ResultSetImpl.class));

    resultSet.next();
    int result = resultSet.getInt(1);
    assertEquals(rnd, result);

    conn.close();
  }

  @Test
  public void testUnclosedConnection()
      throws SQLException, InterruptedException, UnsupportedEncodingException {

    Logger logger = Logger.getLogger(""); //get root logger
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    logger.addHandler(new StreamHandler(os, new SimpleFormatter()));


    Properties props = initDefaultPropsNoTimeouts();
    props.setProperty("proxyDriverLogUnclosedConnections", "true");

    Connection conn = DriverManager.getConnection(getUrl(), props);

    assertTrue(conn instanceof ConnectionWrapper);
    assertTrue(conn.isWrapperFor(com.mysql.cj.jdbc.ConnectionImpl.class));

    assertTrue(conn.isValid(10));

    Statement statement = conn.createStatement();
    assertNotNull(statement);
    assertTrue(statement instanceof StatementWrapper);
    assertTrue(statement.isWrapperFor(com.mysql.cj.jdbc.StatementImpl.class));

    int rnd = new Random().nextInt(100);
    ResultSet resultSet = statement.executeQuery("SELECT " + rnd);
    assertNotNull(resultSet);
    assertTrue(resultSet instanceof ResultSetWrapper);
    assertTrue(resultSet.isWrapperFor(com.mysql.cj.jdbc.result.ResultSetImpl.class));

    resultSet.next();
    int result = resultSet.getInt(1);
    assertEquals(rnd, result);

    conn = null;

    System.gc();

    Thread.sleep(2000);

    String logMessages = new String(os.toByteArray(), "UTF-8");
    assertTrue(logMessages.contains("Finalizing a connection that was never closed."));
  }

  @Test
  public void testUnclosedConnectionHappyCase()
      throws SQLException, InterruptedException, UnsupportedEncodingException {

    Logger logger = Logger.getLogger(""); //get root logger
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    logger.addHandler(new StreamHandler(os, new SimpleFormatter()));

    Properties props = initDefaultPropsNoTimeouts();
    props.setProperty("proxyDriverLogUnclosedConnections", "true");

    Connection conn = DriverManager.getConnection(getUrl(), props);

    assertTrue(conn instanceof ConnectionWrapper);
    assertTrue(conn.isWrapperFor(com.mysql.cj.jdbc.ConnectionImpl.class));

    assertTrue(conn.isValid(10));

    Statement statement = conn.createStatement();
    assertNotNull(statement);
    assertTrue(statement instanceof StatementWrapper);
    assertTrue(statement.isWrapperFor(com.mysql.cj.jdbc.StatementImpl.class));

    int rnd = new Random().nextInt(100);
    ResultSet resultSet = statement.executeQuery("SELECT " + rnd);
    assertNotNull(resultSet);
    assertTrue(resultSet instanceof ResultSetWrapper);
    assertTrue(resultSet.isWrapperFor(com.mysql.cj.jdbc.result.ResultSetImpl.class));

    resultSet.next();
    int result = resultSet.getInt(1);
    assertEquals(rnd, result);

    conn.close();
    conn = null;

    System.gc();

    Thread.sleep(2000);

    String logMessages = new String(os.toByteArray(), "UTF-8");
    assertFalse(logMessages.contains("Finalizing a connection that was never closed."));
  }
}