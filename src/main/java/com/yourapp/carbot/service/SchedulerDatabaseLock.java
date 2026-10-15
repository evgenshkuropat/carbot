package com.yourapp.carbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class SchedulerDatabaseLock {

    private static final Logger log = LoggerFactory.getLogger(SchedulerDatabaseLock.class);
    static final long LOCK_KEY = 4_326_721_905L;

    private final DataSource dataSource;

    public SchedulerDatabaseLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public LockHandle tryAcquire() {
        Connection connection = null;

        try {
            connection = dataSource.getConnection();

            try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                statement.setLong(1, LOCK_KEY);

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next() && result.getBoolean(1)) {
                        return new LockHandle(connection);
                    }
                }
            }

            closeQuietly(connection);
            return null;
        } catch (SQLException e) {
            closeQuietly(connection);
            throw new IllegalStateException("Could not acquire scheduler database lock", e);
        }
    }

    public static final class LockHandle implements AutoCloseable {
        private final Connection connection;
        private boolean closed;

        private LockHandle(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;

            try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                statement.setLong(1, LOCK_KEY);
                statement.executeQuery();
            } catch (SQLException e) {
                log.error("Could not release scheduler database lock", e);
            } finally {
                closeQuietly(connection);
            }
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("Could not close scheduler lock connection", e);
        }
    }
}
