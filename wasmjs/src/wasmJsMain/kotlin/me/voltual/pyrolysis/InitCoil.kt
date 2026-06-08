//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.util.DebugLogger
import coil3.memory.MemoryCache
import coil3.disk.DiskCache
import coil3.request.CachePolicy
import okio.Path

internal val platformContext: PlatformContext = PlatformContext.INSTANCE

fun initCoil() {
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(platformContext)
            .components {
                // 挂载万能图片中转拦截器
                add(UniversalImageProxyInterceptor())
            }
            // 显式配置内存缓存
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(platformContext, 0.25)
                    .build()
            }
            // 塞给它一个“完全不依赖文件系统”的空 DiskCache 实现！
            .diskCache {
                object : DiskCache {
                    override val fileSystem get() = throw UnsupportedOperationException("WASM环境下不启用文件系统")
                    override val directory: Path get() = throw UnsupportedOperationException("WASM环境下不启用路径")
                    override val maxSize: Long get() = 0L
                    override val size: Long get() = 0L
                    
                    override fun clear() {}
                    override fun get(key: String): DiskCache.Snapshot? = null
                    override fun openEditor(key: String): DiskCache.Editor? = null
                    override fun openSnapshot(key: String): DiskCache.Snapshot? = null
                    override fun remove(key: String): Boolean = false
                }
            }
            // 策略也全面禁用
            .diskCachePolicy(CachePolicy.DISABLED)
            .logger(DebugLogger())
            .build()
    }
}

/**
 * 万能图片代理拦截器
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