CREATE TABLE IF NOT EXISTS cars (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    price VARCHAR(100),
    price_value INTEGER,
    location VARCHAR(255),
    url VARCHAR(1000) NOT NULL UNIQUE,
    image_url VARCHAR(1500),
    created_at TIMESTAMP NOT NULL,
    brand VARCHAR(100),
    year INTEGER,
    mileage INTEGER,
    fuel_type VARCHAR(50),
    transmission VARCHAR(50),
    car_type VARCHAR(50),
    owner_chat_id BIGINT,
    seller_username VARCHAR(100),
    seller_contact VARCHAR(255),
    listing_status VARCHAR(50),
    description VARCHAR(1000)
);

ALTER TABLE cars ADD COLUMN IF NOT EXISTS owner_chat_id BIGINT;
ALTER TABLE cars ADD COLUMN IF NOT EXISTS seller_username VARCHAR(100);
ALTER TABLE cars ADD COLUMN IF NOT EXISTS seller_contact VARCHAR(255);
ALTER TABLE cars ADD COLUMN IF NOT EXISTS listing_status VARCHAR(50);
ALTER TABLE cars ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
UPDATE cars SET listing_status = 'ACTIVE' WHERE listing_status IS NULL OR listing_status = '';

CREATE INDEX IF NOT EXISTS idx_car_url ON cars(url);
CREATE INDEX IF NOT EXISTS idx_car_price ON cars(price_value);
CREATE INDEX IF NOT EXISTS idx_car_created ON cars(created_at);
CREATE INDEX IF NOT EXISTS idx_car_source ON cars(source);
CREATE INDEX IF NOT EXISTS idx_car_listing_status ON cars(listing_status);
CREATE INDEX IF NOT EXISTS idx_car_owner_chat ON cars(owner_chat_id);

CREATE TABLE IF NOT EXISTS telegram_subscribers (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT UNIQUE,
    username VARCHAR(255),
    notifications_paused BOOLEAN NOT NULL DEFAULT FALSE,
    daily_notification_limit INTEGER,
    notifications_sent_today INTEGER NOT NULL DEFAULT 0,
    notification_count_date DATE,
    notification_mode VARCHAR(255) DEFAULT 'INSTANT',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE telegram_subscribers ADD COLUMN IF NOT EXISTS notifications_paused BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE telegram_subscribers ADD COLUMN IF NOT EXISTS daily_notification_limit INTEGER;
ALTER TABLE telegram_subscribers ADD COLUMN IF NOT EXISTS notifications_sent_today INTEGER NOT NULL DEFAULT 0;
ALTER TABLE telegram_subscribers ADD COLUMN IF NOT EXISTS notification_count_date DATE;
ALTER TABLE telegram_subscribers ADD COLUMN IF NOT EXISTS notification_mode VARCHAR(255) DEFAULT 'INSTANT';
UPDATE telegram_subscribers SET notifications_paused = FALSE WHERE notifications_paused IS NULL;
UPDATE telegram_subscribers SET notifications_sent_today = 0 WHERE notifications_sent_today IS NULL;
UPDATE telegram_subscribers SET notification_mode = 'INSTANT' WHERE notification_mode IS NULL OR notification_mode = '';

CREATE TABLE IF NOT EXISTS user_filters (
    id BIGSERIAL PRIMARY KEY,
    language_code VARCHAR(10),
    chat_id BIGINT NOT NULL UNIQUE,
    car_type VARCHAR(100),
    brand VARCHAR(300),
    model_query VARCHAR(255),
    max_price INTEGER,
    max_mileage INTEGER,
    location VARCHAR(100),
    fuel_type VARCHAR(50),
    transmission VARCHAR(30),
    year_from INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE user_filters ADD COLUMN IF NOT EXISTS model_query VARCHAR(255);

CREATE TABLE IF NOT EXISTS user_states (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL UNIQUE,
    step VARCHAR(100) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE user_states ALTER COLUMN step TYPE VARCHAR(100);

DO $$
DECLARE
    constraint_record RECORD;
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

CREATE TABLE IF NOT EXISTS favorite_cars (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT,
    car_id BIGINT,
    created_at TIMESTAMP,
    CONSTRAINT uk_favorite_cars_chat_car UNIQUE (chat_id, car_id)
);
