CREATE TABLE `history` (
	`id` text NOT NULL,
	`title` text NOT NULL,
	`type` text NOT NULL,
	`url` text NOT NULL,
	`episodeId` text,
	`episodeTitle` text,
	`seasonNumber` integer,
	`episodeNumber` integer,
	`position` text DEFAULT '00:00:00' NOT NULL,
	`duration` text DEFAULT '00:00:00' NOT NULL,
	`percentWatched` real DEFAULT 0 NOT NULL,
	`lastWatched` integer NOT NULL,
	`completed` integer DEFAULT false NOT NULL,
	PRIMARY KEY(`id`, `episodeId`)
);
--> statement-breakpoint
CREATE INDEX `idx_lastWatched` ON `history` (`lastWatched`);--> statement-breakpoint
CREATE INDEX `idx_incomplete` ON `history` (`completed`,`percentWatched`);--> statement-breakpoint
CREATE INDEX `idx_type` ON `history` (`type`);