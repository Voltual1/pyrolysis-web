package me.voltual.pyrolysis.network

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

actual fun getPlatformEngine(): HttpClientEngineFactory<*> = OkHttp