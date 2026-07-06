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
