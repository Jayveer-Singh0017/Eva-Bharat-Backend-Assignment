-- MySQL Schema for Multi-Window Media Sequencer
-- Exact schema matching the original backend structure

CREATE DATABASE IF NOT EXISTS `media` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `media`;

CREATE TABLE IF NOT EXISTS `media` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `label` VARCHAR(255) NOT NULL,
    `kind` VARCHAR(32) NOT NULL CHECK (`kind` IN ('image', 'video', 'blank')),
    `url` VARCHAR(2048) NOT NULL,
    `default_duration_seconds` INT NOT NULL CHECK (`default_duration_seconds` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `windows` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `position` INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `window_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `window_id` BIGINT NOT NULL,
    `media_id` BIGINT NOT NULL,
    `position` INT NOT NULL,
    `duration_seconds` INT NOT NULL CHECK (`duration_seconds` > 0),
    CONSTRAINT `fk_window_items_window` FOREIGN KEY (`window_id`) REFERENCES `windows` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_window_items_media` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE RESTRICT,
    INDEX `idx_window_items_window_position` (`window_id`, `position`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sync_events` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `media_id` BIGINT NOT NULL,
    `start_at` DATETIME(6) NOT NULL,
    `duration_seconds` INT NOT NULL CHECK (`duration_seconds` > 0),
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT `fk_sync_events_media` FOREIGN KEY (`media_id`) REFERENCES `media` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `settings` (
    `key` VARCHAR(255) PRIMARY KEY,
    `value` TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
