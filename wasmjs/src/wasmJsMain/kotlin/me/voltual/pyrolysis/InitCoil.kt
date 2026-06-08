package me.voltual.pyrolysis

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.util.DebugLogger
import coil3.request.CachePolicy

internal val platformContext: PlatformContext = PlatformContext.INSTANCE

fun initCoil() {
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(platformContext)
            .components {
                // 挂载万能图片中转拦截器
                add(UniversalImageProxyInterceptor())
            }
            // 显式配置内存缓存（可选，调大一点可以提升流畅度）
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(platformContext, 0.25) // 占用可用内存的 25%
                    .build()
            }
            .logger(DebugLogger())
            .build()
    }
}

/**
 * 万能图片代理拦截器（保持不变）
 */
private class UniversalImageProxyInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val originalRequest = chain.request
        val data = originalRequest.data

        val newChain = if (data is String && (data.startsWith("http://") || data.startsWith("https://"))) {
            val isAlreadyProxied = data.contains("/proxy-img/")
            
            if (!isAlreadyProxied) {
                val newUri = "/proxy-img/$data"
                val newRequest = originalRequest.newBuilder()
                    .data(newUri)
                    // 此显式禁用当前请求的磁盘行为
                    .diskCachePolicy(CachePolicy.DISABLED) 
                    .build()
                chain.withRequest(newRequest)
            } else {
                chain
            }
        } else {
            chain
        }

        return newChain.proceed()
    }
}