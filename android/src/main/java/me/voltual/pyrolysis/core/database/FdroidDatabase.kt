/*
 * This file is adapted from Neo Store (https://github.com/NeoApplications/Neo-Store)
 * Modified by Voltual to fit Pyrolysis architecture.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 */
package me.voltual.pyrolysis.core.database

import android.util.Log
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteConnection
import me.voltual.pyrolysis.core.database.dao.*
import me.voltual.pyrolysis.core.database.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.dsl.module
import java.io.File

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

    suspend fun cleanUp(pairs: Set<Pair<Long, Boolean>>) = cleanUp(*pairs.toTypedArray())

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
                
                getProductDao().insert(*(getProductTempDao().getAll() ?: emptyArray()))
                getCategoryDao().insert(*(getCategoryTempDao().getAll() ?: emptyArray()))
                getRepoCategoryDao().insert(*(getRepoCategoryTempDao().getAll() ?: emptyArray()))
                getAntiFeatureDao().insert(*(getAntiFeatureTempDao().getAll() ?: emptyArray()))
                getReleaseDao().insert(*(getReleaseTempDao().getAll() ?: emptyArray()))
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

        // 适配 Room 3.0 异步 SQLiteConnection 的数据库回调
        val dbCreateCallback = object : RoomDatabase.Callback() {
            override suspend fun onOpen(connection: SQLiteConnection) {
                super.onOpen(connection)
                
                // 使用 IO 协程进行异步数据填充，避免阻塞主线程和数据库初始化
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = org.koin.java.KoinJavaComponent.get<RepositoryDao>(RepositoryDao::class.java)
                        
                        // 只有数据库为空时才初始化默认仓库
                        if (dao.getCount() == 0) {
                            val allRepos = (Repository.defaultRepositories + loadPresetRepos())
                                .distinctBy { it.address }
                                .toTypedArray()
                            
                            dao.put(*allRepos)
                            Log.d(TAG, "Database initialized with ${allRepos.size} default repositories.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to seed initial repositories", e)
                    }
                }
            }
        }

        /**
         * 从系统不同分区加载 OEM 预置仓库
         */
        private fun loadPresetRepos(): List<Repository> {
            val roots = listOf("/system", "/product", "/vendor", "/odm", "/oem")
            return roots.mapNotNull { root ->
                val additionalReposFile = File("$root/etc/org.fdroid.fdroid/additional_repos.xml")
                if (additionalReposFile.exists() && additionalReposFile.isFile) {
                    Repository.parsePresetReposXML(additionalReposFile)
                } else {
                    null
                }
            }.flatten()
        }
    }
}

// 补全并修复后的 Koin 模块：正确挂载了 dbCreateCallback 并使用 Bundled 驱动
val databaseModule = module {
    single<FdroidDatabase> {
        androidx.room3.Room.databaseBuilder(
            get(),
            FdroidDatabase::class.java,
            "main_fdroid_database.db"
        )
        .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver()) // 必须显式设置 Room3 驱动
        .addCallback(FdroidDatabase.dbCreateCallback) // 修复：挂载初始化回调
        .fallbackToDestructiveMigration(true)
        .build()
    }
    
    single { get<FdroidDatabase>().getRepositoryDao() }
    single { get<FdroidDatabase>().getProductDao() }
    single { get<FdroidDatabase>().getReleaseDao() }
    single { get<FdroidDatabase>().getReleaseTempDao() }
    single { get<FdroidDatabase>().getProductTempDao() }
    single { get<FdroidDatabase>().getCategoryDao() }
    single { get<FdroidDatabase>().getCategoryTempDao() }
    single { get<FdroidDatabase>().getRepoCategoryDao() }
    single { get<FdroidDatabase>().getRepoCategoryTempDao() }
    single { get<FdroidDatabase>().getAntiFeatureDao() }
    single { get<FdroidDatabase>().getAntiFeatureTempDao() }
    single { get<FdroidDatabase>().getInstalledDao() }
    single { get<FdroidDatabase>().getExtrasDao() }
    single { get<FdroidDatabase>().getExodusInfoDao() }
    single { get<FdroidDatabase>().getTrackerDao() }
    single { get<FdroidDatabase>().getDownloadedDao() }
    single { get<FdroidDatabase>().getInstallTaskDao() }
    single { get<FdroidDatabase>().getRBLogDao() }
    single { get<FdroidDatabase>().getDownloadStatsDao() }
    single { get<FdroidDatabase>().getDownloadStatsFileDao() }
}