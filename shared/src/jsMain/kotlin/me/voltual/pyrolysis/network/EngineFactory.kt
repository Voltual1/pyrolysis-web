package me.voltual.pyrolysis.network

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

actual fun getPlatformEngine(): HttpClientEngineFactory<*> = Js