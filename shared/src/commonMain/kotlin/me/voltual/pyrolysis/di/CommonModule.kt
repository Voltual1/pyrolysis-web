package me.voltual.pyrolysis.di

import me.voltual.pyrolysis.network.KtorClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    // 注入 API 实现
    single { KtorClient.ApiServiceImpl }
}