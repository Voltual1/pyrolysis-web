// shared/src/commonMain/kotlin/me/voltual/pyrolysis/core/database/Database.kt
package me.voltual.pyrolysis.core.database

import androidx.room3.RoomDatabase

fun createDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder.build()
}