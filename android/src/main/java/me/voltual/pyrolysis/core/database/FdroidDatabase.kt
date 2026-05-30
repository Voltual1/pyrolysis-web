/*
 * This file is adapted from Neo Store (https://github.com/NeoApplications/Neo-Store)
 * Modified by Voltual to fit Pyrolysis architecture.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 */
package me.voltual.pyrolysis.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteConnection
import me.voltual.pyrolysis.core.database.dao.*
import me.voltual.pyrolysis.core.database.entity.*

@Database(
    entities = [
        Repository::class, Product::class, Release::class, ReleaseTemp::class,
        ProductTemp::class, Category::class, CategoryTemp::class,
        RepoCategory::class, RepoCategoryTemp::class, Installed::class,
        Extras::class, ExodusInfo::class, Tracker::class, AntiFeature::class,
        AntiFeatureTemp::class, RBLog::class, InstallTask::class,
        Downloaded::class, DownloadStats::class, DownloadStatsFileMetadata::class,
    ],
    views = [
        PackageSum::class, ClientPackageSum::class, MonthlyPackageSum::class,
        ProductIconDetails::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
@ConstructedBy(FdroidDatabaseConstructor::class)
abstract class FdroidDatabase : RoomDatabase() {

    abstract fun getRepositoryDao(): RepositoryDao
    abstract fun getProductDao(): ProductDao
    abstract fun getReleaseDao(): ReleaseDao
    abstract fun getReleaseTempDao(): ReleaseTempDao
    abstract fun getProductTempDao(): ProductTempDao
    abstract fun getCategoryDao(): CategoryDao
    abstract fun getCategoryTempDao(): CategoryTempDao
    abstract fun getRepoCategoryDao(): RepoCategoryDao
    abstract fun getRepoCategoryTempDao(): RepoCategoryTempDao
    abstract fun getAntiFeatureDao(): AntiFeatureDao
    abstract fun getAntiFeatureTempDao(): AntiFeatureTempDao
    abstract fun getInstalledDao(): InstalledDao
    abstract fun getExtrasDao(): ExtrasDao
    abstract fun getExodusInfoDao(): ExodusInfoDao
    abstract fun getTrackerDao(): TrackerDao
    abstract fun getRBLogDao(): RBLogDao
    abstract fun getInstallTaskDao(): InstallTaskDao
    abstract fun getDownloadedDao(): DownloadedDao
    abstract fun getDownloadStatsDao(): DownloadStatsDao
    abstract fun getDownloadStatsFileDao(): DownloadStatsFileDao

    suspend fun cleanUp(vararg pairs: Pair<Long, Boolean>) {
        this.withWriteTransaction {
            pairs.forEach { (id, enabled) ->
                getProductDao().deleteById(id)
                getCategoryDao().deleteById(id)
                getReleaseDao().deleteById(id)
                if (enabled) getRepositoryDao().deleteById(id)
            }
        }
    }

    suspend fun finishTemporary(repository: Repository, success: Boolean) {
        this.withWriteTransaction {
            if (success) {
                getProductDao().deleteById(repository.id)
                getCategoryDao().deleteById(repository.id)
                getRepoCategoryDao().deleteByRepoId(repository.id)
                getAntiFeatureDao().deleteByRepoId(repository.id)
                getReleaseDao().deleteById(repository.id)
                
                getProductDao().insert(*(getProductTempDao().getAll()))
                getCategoryDao().insert(*(getCategoryTempDao().getAll()))
                getRepoCategoryDao().insert(*(getRepoCategoryTempDao().getAll()))
                getAntiFeatureDao().insert(*(getAntiFeatureTempDao().getAll()))
                getReleaseDao().insert(*(getReleaseTempDao().getAll()))
                getRepositoryDao().put(repository)
            }
            getProductTempDao().emptyTable()
            getCategoryTempDao().emptyTable()
            getRepoCategoryTempDao().emptyTable()
            getAntiFeatureTempDao().emptyTable()
            getReleaseTempDao().emptyTable()
        }
    }

    companion object {
        const val TAG = "fdroid.db"
    }
}

@Suppress("KotlinNoActualForExpect")
expect object FdroidDatabaseConstructor : RoomDatabaseConstructor<FdroidDatabase> {
    override fun initialize(): FdroidDatabase
}