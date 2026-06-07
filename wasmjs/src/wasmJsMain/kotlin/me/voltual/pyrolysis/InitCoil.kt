package me.voltual.pyrolysis

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.util.DebugLogger

internal val platformContext: PlatformContext = PlatformContext.INSTANCE

fun initCoil() {
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(platformContext)
            .components {
                // 挂载万能图片中转拦截器
                add(UniversalImageProxyInterceptor())
            }
            .logger(DebugLogger())
            .build()
    }
}

/**
 * 万能图片代理拦截器
 * 自动对所有的网络图片执行同源代理转换
 */
private class UniversalImageProxyInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val originalRequest = chain.request
        val data = originalRequest.data

        val newChain = if (data is String && (data.startsWith("http://") || data.startsWith("https://"))) {
            // 过滤：如果图片链接已经被套过壳（比如重复进入拦截器），则不再处理
            val isAlreadyProxied = data.contains("/proxy-img/")
            
            if (!isAlreadyProxied) {
                // 原地转为相对路径，浏览器会将其视为百分之百安全的同源请求，直接绕过 require-corp 的审查！
                val newUri = "/proxy-img/$data"
                val newRequest = originalRequest.newBuilder().data(newUri).build()
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