package com.yourapp.carbot.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaMigrationService.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        migrateTelegramSubscribers();
        migrateUserStateSteps();
        migrateCarsUserListings();
    }

    private void migrateTelegramSubscribers() {
        jdbcTemplate.execute("""
                ALTER TABLE IF EXISTS telegram_subscribers
                    ADD COLUMN IF NOT EXISTS notifications_paused boolean NOT NULL DEFAULT false,
                    ADD COLUMN IF NOT EXISTS daily_notification_limit integer,
                    ADD COLUMN IF NOT EXISTS notifications_sent_today integer NOT NULL DEFAULT 0,
                    ADD COLUMN IF NOT EXISTS notification_count_date date,
                    ADD COLUMN IF NOT EXISTS notification_mode varchar(50) DEFAULT 'INSTANT'
                """);

        jdbcTemplate.execute("""
                UPDATE telegram_subscribers
                SET notifications_paused = false
                WHERE notifications_paused IS NULL
                """);

        jdbcTemplate.execute("""
                UPDATE telegram_subscribers
                SET notifications_sent_today = 0
                WHERE notifications_sent_today IS NULL
                """);

        jdbcTemplate.execute("""
                UPDATE telegram_subscribers
                SET notification_mode = 'INSTANT'
                WHERE notification_mode IS NULL OR notification_mode = ''
                """);

        log.info("Database schema migration checked telegram_subscribers notification columns");
    }

    private void migrateUserStateSteps() {
        jdbcTemplate.execute("""
                ALTER TABLE IF EXISTS user_states
                    ALTER COLUMN step TYPE varchar(100)
                """);

        jdbcTemplate.execute("""
                DO $$
                DECLARE
                    constraint_record record;
                BEGIN
                    FOR constraint_record IN
                        SELECT namespace.nspname AS schema_name,
                               relation.relname AS table_name,
                               constraint_info.conname AS constraint_name
                        FROM pg_constraint constraint_info
                        JOIN pg_class relation ON relation.oid = constraint_info.conrelid
                        JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                        WHERE relation.relname = 'user_states'
                          AND constraint_info.contype = 'c'
                          AND pg_get_constraintdef(constraint_info.oid) ILIKE '%step%'
                    LOOP
                        EXECUTE format(
                            'ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
                            constraint_record.schema_name,
                            constraint_record.table_name,
                            constraint_record.constraint_name
                        );
                    END LOOP;
                END $$;
                """);

        log.info("Database schema migration checked user_states step column");
    }

    private void migrateCarsUserListings() {
        jdbcTemplate.execute("""
                ALTER TABLE IF EXISTS cars
                    ADD COLUMN IF NOT EXISTS owner_chat_id bigint,
                    ADD COLUMN IF NOT EXISTS seller_username varchar(100),
                    ADD COLUMN IF NOT EXISTS seller_contact varchar(255),
                    ADD COLUMN IF NOT EXISTS listing_status varchar(50),
                    ADD COLUMN IF NOT EXISTS description varchar(1000)
                """);

        jdbcTemplate.execute("""
                UPDATE cars
                SET listing_status = 'ACTIVE'
                WHERE listing_status IS NULL OR listing_status = ''
                """);

        log.info("Database schema migration checked cars user listing columns");
    }
}
