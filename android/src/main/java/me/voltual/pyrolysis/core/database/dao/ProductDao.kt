/*
 * This file is adapted from Neo Store (https://github.com/NeoApplications/Neo-Store)
 * Modified by Voltual to fit Pyrolysis architecture.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 */
package me.voltual.pyrolysis.core.database.dao

import android.os.Build
import androidx.room3.Dao
import androidx.room3.MapColumn
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.Transaction
import androidx.room3.RoomRawQuery
import me.voltual.pyrolysis.*
import me.voltual.pyrolysis.data.content.Preferences
import me.voltual.pyrolysis.core.database.QueryBuilder
import me.voltual.pyrolysis.core.database.entity.*
import me.voltual.pyrolysis.data.entity.*
import me.voltual.pyrolysis.core.utils.extension.android.Android
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao : BaseDao<Product> {
    @Query("SELECT COUNT(*) FROM $TABLE_PRODUCT WHERE repositoryId = :id")
    suspend fun countForRepository(id: Long): Long

    @Query("SELECT COUNT(*) FROM $TABLE_PRODUCT WHERE repositoryId = :id")
    fun countForRepositoryFlow(id: Long): Flow<Long>

    @Transaction
    @Query("SELECT * FROM $TABLE_PRODUCT WHERE repositoryId = :repoId ORDER BY label")
    fun productsForRepositoryFlow(repoId: Long): Flow<List<EmbeddedProduct>>

    @Query("SELECT EXISTS(SELECT 1 FROM $TABLE_PRODUCT WHERE packageName = :packageName)")
    suspend fun exists(packageName: String): Boolean

    @Transaction
    @Query("SELECT * FROM $TABLE_PRODUCT WHERE packageName = :packageName")
    suspend fun get(packageName: String): List<EmbeddedProduct>

    @Transaction
    @Query("SELECT * FROM $TABLE_PRODUCT WHERE packageName = :packageName")
    fun getFlow(packageName: String): Flow<List<EmbeddedProduct>>

    @Transaction
    @Query("SELECT * FROM $TABLE_PRODUCT WHERE packageName = :packageName AND repositoryId = :repoId")
    suspend fun get(packageName: String, repoId: Long): EmbeddedProduct?

    @Transaction
    @Query("SELECT * FROM $TABLE_PRODUCT WHERE packageName = :packageName AND repositoryId = :repoId")
    fun getFlow(packageName: String, repoId: Long): Flow<EmbeddedProduct?>

    @Query("SELECT * FROM producticondetails")
    fun getIconDetailsMapFlow(): Flow<Map<@MapColumn(columnName = ROW_PACKAGE_NAME) String, ProductIconDetails>>

    @Query("DELETE FROM $TABLE_PRODUCT WHERE repositoryId = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT DISTINCT licenses FROM $TABLE_PRODUCT")
    suspend fun getAllLicenses(): List<Licenses>

    @Query("SELECT DISTINCT licenses FROM $TABLE_PRODUCT")
    fun getAllLicensesFlow(): Flow<List<Licenses>>

    @Transaction
    @Query("SELECT * FROM $TABLE_PRODUCT WHERE author LIKE '%' || :author || '%' ")
    fun getAuthorPackagesFlow(author: String): Flow<List<EmbeddedProduct>>

    @RawQuery
    suspend fun queryObject(query: RoomRawQuery): List<EmbeddedProduct>

    @Transaction
    @RawQuery(observedEntities = [Product::class, Installed::class, Extras::class, Repository::class, Category::class])
    fun queryFlowList(query: RoomRawQuery): Flow<List<EmbeddedProduct>>

    // 补全缺失的方法
    suspend fun getInstalledProductsWithVulnerabilities(repoId: Long): List<EmbeddedProduct> {
        val sql = """
            SELECT $TABLE_PRODUCT.*
            FROM $TABLE_PRODUCT
            JOIN $TABLE_INSTALLED ON $TABLE_PRODUCT.$ROW_PACKAGE_NAME = $TABLE_INSTALLED.$ROW_PACKAGE_NAME
            LEFT JOIN $TABLE_EXTRAS ON $TABLE_PRODUCT.$ROW_PACKAGE_NAME = $TABLE_EXTRAS.$ROW_PACKAGE_NAME
            WHERE $TABLE_PRODUCT.$ROW_REPOSITORY_ID = ?
            AND $TABLE_PRODUCT.$ROW_ANTIFEATURES LIKE '%KnownVuln%'
            AND COALESCE($TABLE_EXTRAS.$ROW_IGNORE_VULNS, 0) = 0
            GROUP BY $TABLE_PRODUCT.$ROW_PACKAGE_NAME
            ORDER BY $TABLE_PRODUCT.$ROW_LABEL COLLATE LOCALIZED ASC
        """.trimIndent()
        return queryObject(RoomRawQuery(sql) { it.bindLong(1, repoId) })
    }

    // 补全缺失的方法
    fun queryFlowOfPackages(pkgs: Set<String>): Flow<List<EmbeddedProduct>> {
        val query = buildProductRoomRawQuery(
            installed = false,
            updates = false,
            section = Section.All,
            specificPackages = pkgs,
            order = Order.LAST_UPDATE,
            ascending = false,
            numberOfItems = pkgs.size,
        )
        return queryFlowList(query)
    }

    fun buildProductRoomRawQuery(
        installed: Boolean,
        updates: Boolean,
        section: Section,
        filteredOutRepos: Set<String> = emptySet(),
        category: String = FILTER_CATEGORY_ALL,
        filteredAntiFeatures: Set<String> = emptySet(),
        filteredLicenses: Set<String> = emptySet(),
        specificPackages: Set<String> = emptySet(),
        order: Order,
        ascending: Boolean = false,
        numberOfItems: Int = 0,
        updateCategory: UpdateCategory = UpdateCategory.ALL,
        author: String = "",
        targetSdkVersion: Int = 0,
        minSdkVersion: Int = 0,
    ): RoomRawQuery {
        val builder = QueryBuilder()
        if (section == Section.NONE) return RoomRawQuery("SELECT * FROM $TABLE_PRODUCT LIMIT 0")

        builder += """
        SELECT $TABLE_PRODUCT.*, 
        $TABLE_REPOSITORY.$ROW_ENABLED AS repo_enabled,
        $TABLE_EXTRAS.$ROW_FAVORITE AS is_favorite,
        $TABLE_INSTALLED.$ROW_VERSION_CODE AS installed_version_code,
        $TABLE_INSTALLED.$ROW_SIGNATURES AS installed_signature
        FROM $TABLE_PRODUCT
        JOIN (
            SELECT p2.$ROW_PACKAGE_NAME, p2.$ROW_REPOSITORY_ID,
            ROW_NUMBER() OVER (PARTITION BY p2.$ROW_PACKAGE_NAME ORDER BY COALESCE((SELECT MAX(rel.$ROW_VERSION_CODE) FROM $TABLE_RELEASE rel WHERE rel.$ROW_PACKAGE_NAME = p2.$ROW_PACKAGE_NAME AND rel.$ROW_REPOSITORY_ID = p2.$ROW_REPOSITORY_ID), 0) DESC) as rn
            FROM $TABLE_PRODUCT p2 JOIN $TABLE_REPOSITORY r2 ON p2.$ROW_REPOSITORY_ID = r2.$ROW_ID
            WHERE r2.$ROW_ENABLED = 1
            ${if (filteredOutRepos.isNotEmpty()) "AND p2.$ROW_REPOSITORY_ID NOT IN (${filteredOutRepos.joinToString(",")})" else ""}
        ) ranked_products ON $TABLE_PRODUCT.$ROW_PACKAGE_NAME = ranked_products.$ROW_PACKAGE_NAME AND $TABLE_PRODUCT.$ROW_REPOSITORY_ID = ranked_products.$ROW_REPOSITORY_ID AND ranked_products.rn = 1
        JOIN $TABLE_REPOSITORY ON $TABLE_PRODUCT.$ROW_REPOSITORY_ID = $TABLE_REPOSITORY.$ROW_ID
        ${if (!installed && !updates) "LEFT " else ""}JOIN $TABLE_INSTALLED ON $TABLE_PRODUCT.$ROW_PACKAGE_NAME = $TABLE_INSTALLED.$ROW_PACKAGE_NAME
        LEFT JOIN $TABLE_EXTRAS ON $TABLE_PRODUCT.$ROW_PACKAGE_NAME = $TABLE_EXTRAS.$ROW_PACKAGE_NAME
        LEFT JOIN $TABLE_CATEGORY ON $TABLE_PRODUCT.$ROW_PACKAGE_NAME = $TABLE_CATEGORY.$ROW_PACKAGE_NAME
        """

        val where = mutableListOf<String>()
        if (specificPackages.isNotEmpty()) where.add("$TABLE_PRODUCT.$ROW_PACKAGE_NAME IN (${specificPackages.joinToString(",", transform = { "'$it'" })})")
        if (author.isNotEmpty()) { where.add("$TABLE_PRODUCT.$ROW_AUTHOR = ?"); builder.addArgument(author) }
        if (category != FILTER_CATEGORY_ALL) { where.add("$TABLE_CATEGORY.$ROW_NAME = ?"); builder.addArgument(category) }
        if (section == Section.FAVORITE) where.add("COALESCE($TABLE_EXTRAS.$ROW_FAVORITE, 0) != 0")
        
        if (updates) {
            where.add("COALESCE($TABLE_EXTRAS.$ROW_IGNORE_UPDATES, 0) = 0 AND EXISTS (SELECT 1 FROM $TABLE_RELEASE WHERE $TABLE_RELEASE.$ROW_PACKAGE_NAME = $TABLE_PRODUCT.$ROW_PACKAGE_NAME AND $TABLE_RELEASE.$ROW_REPOSITORY_ID = $TABLE_PRODUCT.$ROW_REPOSITORY_ID AND $TABLE_RELEASE.$ROW_SELECTED = 1 AND $TABLE_RELEASE.$ROW_VERSION_CODE > COALESCE($TABLE_INSTALLED.$ROW_VERSION_CODE, -1) AND $TABLE_RELEASE.$ROW_IS_COMPATIBLE = 1)")
        }

        if (where.isNotEmpty()) builder += "WHERE ${where.joinToString(" AND ")}"
        builder += "GROUP BY $TABLE_PRODUCT.$ROW_PACKAGE_NAME"
        val orderBy = when (order) {
            Order.NAME -> "$TABLE_PRODUCT.$ROW_LABEL COLLATE LOCALIZED ${if (ascending) "ASC" else "DESC"}"
            Order.DATE_ADDED -> "$TABLE_PRODUCT.$ROW_ADDED ${if (ascending) "ASC" else "DESC"}"
            Order.LAST_UPDATE -> "$TABLE_PRODUCT.$ROW_UPDATED ${if (ascending) "ASC" else "DESC"}"
        }
        builder += "ORDER BY $orderBy"
        if (numberOfItems > 0) builder += "LIMIT $numberOfItems"

        return RoomRawQuery(builder.build()) { stmt ->
            builder.arguments.forEachIndexed { index, arg -> stmt.bindText(index + 1, arg) }
        }
    }

    @Query("DELETE FROM $TABLE_PRODUCT")
    suspend fun emptyTable()
}

@Dao
interface ProductTempDao : BaseDao<ProductTemp> {
    @Query("SELECT * FROM $TABLE_PRODUCT_TEMP")
    suspend fun getAll(): Array<ProductTemp>

    @Query("DELETE FROM $TABLE_PRODUCT_TEMP")
    suspend fun emptyTable()
}