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
import coil3.request.CachePolicy

internal val platformContext: PlatformContext = PlatformContext.INSTANCE

fun initCoil() {
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(platformContext)
            .components {
                add(UniversalImageProxyInterceptor())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(platformContext, 0.25)
                    .build()
            }
            // 模仿官方示例，直接传递一个明确返回 null 的闭包！
            // 确保触发 .diskCache(factory: () -> DiskCache?) 重载，
            // 从而彻底、干净地抹除掉 Coil 内部隐式自动生成的默认磁盘缓存工厂！
            .diskCache { 
                null 
            }
            // 双重保险
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
                    .diskCachePolicy(CachePolicy.DISABLED) // 请求级别再次声明不写磁盘
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