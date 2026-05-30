// shared/src/androidMain/kotlin/me/voltual/pyrolysis/core/database/Database.android.kt
package me.voltual.pyrolysis.core.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("pyrolysis.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
    .setDriver(BundledSQLiteDriver()) // 使用捆绑的 SQLite 驱动以保证跨平台一致性
    .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
    .setQueryCoroutineContext(Dispatchers.IO)
}