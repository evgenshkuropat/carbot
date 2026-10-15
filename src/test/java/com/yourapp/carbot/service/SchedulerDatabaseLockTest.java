package com.yourapp.carbot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerDatabaseLockTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement acquireStatement;

    @Mock
    private PreparedStatement releaseStatement;

    @Mock
    private ResultSet acquireResult;

    @Mock
    private ResultSet releaseResult;

    @Test
    void keepsTheSameConnectionLockedUntilTheRunFinishes() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT pg_try_advisory_lock(?)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResult);
        when(acquireResult.next()).thenReturn(true);
        when(acquireResult.getBoolean(1)).thenReturn(true);
        when(connection.prepareStatement("SELECT pg_advisory_unlock(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenReturn(releaseResult);

        SchedulerDatabaseLock lock = new SchedulerDatabaseLock(dataSource);
        SchedulerDatabaseLock.LockHandle handle = lock.tryAcquire();

        assertThat(handle).isNotNull();
        handle.close();

        verify(acquireStatement).setLong(1, SchedulerDatabaseLock.LOCK_KEY);
        verify(releaseStatement).setLong(1, SchedulerDatabaseLock.LOCK_KEY);
        verify(connection).close();
    }
}
