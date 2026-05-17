package me.voltual.pyrolysis.network

import io.ktor.client.engine.*

expect fun getPlatformEngine(): HttpClientEngineFactory<*>