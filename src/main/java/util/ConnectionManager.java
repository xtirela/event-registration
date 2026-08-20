package util;

import exception.SQLEventRegException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import lombok.SneakyThrows;

public class ConnectionManager {
  private static final String DB_URL =
      System.getProperty(
          "db.url",
          System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/eventreg_db"));
  private static final String DB_USERNAME =
      System.getProperty("db.user", System.getenv().getOrDefault("DB_USER", "postgres"));
  private static final String DB_PASSWORD =
      System.getProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", "postgres"));
  private static final int DEFAULT_POOL_SIZE = 12;
  private static BlockingQueue<Connection> pool;

  static {
    initConnectionPool();
  }

  private static void initConnectionPool() {
    pool = new ArrayBlockingQueue<>(DEFAULT_POOL_SIZE);
    for (int i = 0; i < DEFAULT_POOL_SIZE; i++) {
      Connection connection = open();
      var proxyConnection =
          (Connection)
              Proxy.newProxyInstance(
                  ConnectionManager.class.getClassLoader(),
                  new Class[] {Connection.class},
                  (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                      resetBeforeReturn(connection);
                      pool.add((Connection) proxy);
                      return null;
                    }
                    return method.invoke(connection, args);
                  });

      pool.add(proxyConnection);
    }
  }

  @SneakyThrows
  public static void closeAll() {
    for (int i = 0; i < DEFAULT_POOL_SIZE; i++) {
      Objects.requireNonNull(pool.poll()).close();
    }
  }

  private static void resetBeforeReturn(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException exception) {
      // ignore, nothing to roll back
    }
    try {
      connection.setAutoCommit(true);
    } catch (SQLException exception) {
      // ignore
    }
  }

  public static Connection get() {
    try {
      return pool.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SQLEventRegException(
          "Interrupted while acquiring a connection from the pool", "ConnectionManager.get", e);
    }
  }

  private static Connection open() {
    try {
      return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to open connection ", "ConnectionManager.open", exception);
    }
  }

  private ConnectionManager() {}
}
