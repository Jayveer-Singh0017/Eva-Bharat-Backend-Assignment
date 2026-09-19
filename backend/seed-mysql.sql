-- MySQL Seed Data for Multi-Window Media Sequencer
USE `media`;

-- Seed Media (M1 - M6)
INSERT INTO `media` (`id`, `label`, `kind`, `url`, `default_duration_seconds`) VALUES
(1, 'M1', 'image', '/media/m1.svg', 8),
(2, 'M2', 'image', '/media/m2.svg', 10),
(3, 'M3', 'image', '/media/m3.svg', 7),
(4, 'M4', 'image', '/media/m4.svg', 9),
(5, 'M5', 'video', '/media/m5.mp4', 15),
(6, 'M6', 'blank', '', 5)
ON DUPLICATE KEY UPDATE `id`=`id`;

-- Seed Windows (1 - 4)
INSERT INTO `windows` (`id`, `name`, `position`) VALUES
(1, 'Window 1', 1),
(2, 'Window 2', 2),
(3, 'Window 3', 3),
(4, 'Window 4', 4)
ON DUPLICATE KEY UPDATE `id`=`id`;

-- Seed Window Playlists
INSERT INTO `window_items` (`id`, `window_id`, `media_id`, `position`, `duration_seconds`) VALUES
(1, 1, 1, 1, 8),
(2, 1, 2, 2, 10),
(3, 1, 3, 3, 7),
(4, 2, 3, 1, 7),
(5, 2, 4, 2, 9),
(6, 2, 5, 3, 15),
(7, 3, 5, 1, 15),
(8, 3, 1, 2, 8),
(9, 3, 6, 3, 5),
(10, 3, 2, 4, 10),
(11, 4, 6, 1, 5),
(12, 4, 4, 2, 9),
(13, 4, 2, 3, 10),
(14, 4, 3, 4, 7)
ON DUPLICATE KEY UPDATE `id`=`id`;

-- Seed Schedule Settings
INSERT INTO `settings` (`key`, `value`) VALUES
('cycle_seconds', '18000'),
('anchor', '2026-01-01T00:00:00Z')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
