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
    .setDriver(BundledSQLiteDriver())
    .addMigrations(*AppDatabase.ALL_MIGRATIONS) // 现在可以正确引用 AppDatabase 内部的数组
    .setQueryCoroutineContext(Dispatchers.IO)
}