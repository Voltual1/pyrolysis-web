package me.voltual.pyrolysis.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [LogEntry::class, BrowseHistory::class, NetworkCacheEntry::class, PostDraft::class],
    version = 7,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun browseHistoryDao(): BrowseHistoryDao
    abstract fun networkCacheDao(): NetworkCacheDao
    abstract fun postDraftDao(): PostDraftDao
}

// Room 3.0 KMP 编译器将生成此对象的 actual 实现
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

// 迁移逻辑迁移到 SQLiteConnection
object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `browse_history` (`postId` INTEGER NOT NULL, `title` TEXT NOT NULL, `previewContent` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`postId`))"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `network_cache` (`requestKey` TEXT NOT NULL, `responseJson` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`requestKey`))"
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `post_draft` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `imageUris` TEXT NOT NULL, `imageUrls` TEXT NOT NULL, `subsectionId` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `download_tasks` (`url` TEXT NOT NULL, `fileName` TEXT NOT NULL, `savePath` TEXT NOT NULL, `totalBytes` INTEGER NOT NULL, `downloadedBytes` INTEGER NOT NULL, `status` TEXT NOT NULL, `progress` REAL NOT NULL, PRIMARY KEY(`url`))"
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `download_tasks` ADD COLUMN `speed` TEXT")
            connection.execSQL("ALTER TABLE `download_tasks` ADD COLUMN `errorMessage` TEXT")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DROP TABLE IF EXISTS `download_tasks`")
        }
    }

    val MIGRATION_4_7 = object : Migration(4, 7) {
        override fun migrate(connection: SQLiteConnection) {
            // No-op or equivalent logic
        }
    }

    val MIGRATION_5_7 = object : Migration(5, 7) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DROP TABLE IF EXISTS `download_tasks`")
        }
    }
    
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, 
        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
        MIGRATION_4_7, MIGRATION_5_7
    )
}