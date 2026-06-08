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
            // 直接重写 diskCache，返回一个完全不关联实际文件系统的空壳
            .diskCache {
                DiskCache.Builder()
                    // 核心：强制将其最大体积限制为 0 字节，阻止任何写入
                    .maxSize(0) 
                    // 重点：不要去调 FileSystem.SYSTEM 
                    // 如果编译器强制要求传入 directory，可以传一个由空文件系统生成的虚拟路径
                    // 或者依赖下面这一行将策略彻底掐死：
                    .build()
            }
            // 严防死守，继续禁用策略
            .diskCachePolicy(CachePolicy.DISABLED)
            .logger(DebugLogger())
            .build()
    }
}