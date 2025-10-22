package com.hritwik.avoid.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN filePath TEXT")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN downloadedBytes INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE downloads ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS downloads_new (
                mediaId TEXT NOT NULL,
                requestUri TEXT NOT NULL,
                title TEXT NOT NULL,
                type TEXT NOT NULL,
                progress REAL NOT NULL CHECK(progress >= 0 AND progress <= 100),
                status TEXT NOT NULL,
                downloadedBytes INTEGER NOT NULL DEFAULT 0,
                filePath TEXT,
                audioStreams TEXT NOT NULL,
                subtitleStreams TEXT NOT NULL,
                priority INTEGER NOT NULL DEFAULT 0,
                addedAt INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(mediaId),
                FOREIGN KEY(mediaId) REFERENCES media_items(id) ON DELETE CASCADE
            )
            """
        )
        db.execSQL(
            """
            INSERT INTO downloads_new (
                mediaId, requestUri, title, type, progress, status, downloadedBytes, filePath, audioStreams, subtitleStreams, priority, addedAt
            ) SELECT mediaId, requestUri, title, type, progress, status, downloadedBytes, filePath, audioStreams, subtitleStreams, priority, addedAt FROM downloads
            """
        )
        db.execSQL("DROP TABLE downloads")
        db.execSQL("ALTER TABLE downloads_new RENAME TO downloads")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_downloads_status ON downloads(status)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN subtitleFilePaths TEXT NOT NULL DEFAULT '[]'")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN defaultVideoStream TEXT")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS media_items")
        db.execSQL("DROP TABLE IF EXISTS libraries")
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS downloads")
        db.execSQL("DROP TABLE IF EXISTS playback_logs")
        db.execSQL("DROP TABLE IF EXISTS search_results")
        db.execSQL("DROP TABLE IF EXISTS pending_actions")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN queueIndex INTEGER NOT NULL DEFAULT -1")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN serverUrl TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE downloads ADD COLUMN accessToken TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloads ADD COLUMN quality TEXT NOT NULL DEFAULT 'FHD_1080'")
    }
}
