-- ============================================================
-- CityCare - Veritabani Baslangic Script'i
-- MySQL icin hazirlanmistir.
-- ============================================================

-- Veritabani olusturma (henuz yoksa)
CREATE DATABASE IF NOT EXISTS citycare_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_turkish_ci;

USE citycare_db;

-- Tablolari temizle (gelistirme ortami icin)
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE reports;
-- TRUNCATE TABLE users;
-- SET FOREIGN_KEY_CHECKS = 1;

-- ─────────────────────────────────────
-- TABLOLAR (spring.jpa.ddl-auto=update
-- ile otomatik olusturulur, bu script
-- referans amaclıdir)
-- ─────────────────────────────────────

-- Kullanicilar tablosu
CREATE TABLE IF NOT EXISTS users (
    id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL
);

-- Ihbarlar tablosu
CREATE TABLE IF NOT EXISTS reports (
    id                  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(150)  NOT NULL,
    description         TEXT          NOT NULL,
    category            VARCHAR(30)   NOT NULL,
    image_data          LONGBLOB,
    image_content_type  VARCHAR(50),
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACIK',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id             BIGINT        NOT NULL,
    CONSTRAINT fk_reports_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Indeksler (arama performansi icin)
CREATE INDEX IF NOT EXISTS idx_reports_category  ON reports (category);
CREATE INDEX IF NOT EXISTS idx_reports_status    ON reports (status);
CREATE INDEX IF NOT EXISTS idx_reports_user      ON reports (user_id);
CREATE INDEX IF NOT EXISTS idx_reports_created   ON reports (created_at DESC);

-- ─────────────────────────────────────
-- ORNEK VERI (Opsiyonel)
-- Sifre BCrypt hash'i: "admin123"
-- ─────────────────────────────────────

/*
INSERT IGNORE INTO users (username, password, role) VALUES
  ('admin',    '$2a$12$eHGNHOnSXalCc8xz3XbTBumfr3mI.O9aJJL7iN7cXqFkq/kJjXfb2', 'ADMIN'),
  ('vatandas', '$2a$12$K9L1NbJHY3KZ3.bNfZrKAuV1zW5VkDPJoL3ywFpVLaSQPwL9MqVgK', 'USER');

INSERT INTO reports (title, description, category, status, user_id) VALUES
  ('Besiktas - Barbaros Bulvari cukuru',
   'Barbaros Bulvari No:45 onunde buyuk cukur var, araclar hasar gormekte.',
   'YOL', 'ACIK', (SELECT id FROM users WHERE username='vatandas')),

  ('Macka Parki aydinlatma arizasi',
   'Macka Parki girisindeki 3 adet lamba yaklasik 1 haftadir yanmiyor.',
   'AYDINLATMA', 'INCELEMEDE', (SELECT id FROM users WHERE username='vatandas')),

  ('Cihangir cop konteyneri tasiyor',
   'Cihangir Sokak No:12 yanindaki konteyner her gun dolup tasmaktadir.',
   'CEVRE', 'COZULDU', (SELECT id FROM users WHERE username='vatandas'));
*/
