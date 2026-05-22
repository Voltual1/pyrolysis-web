//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

// 这是一个纯 Kotlin 的单例，完全不依赖 Android 任何包
object FakeR {
    object string {
        const val copied_link = "copied_link"
        const val comment_copied = "comment_copied"
    }
}

object FakeContext {
    // 将复刻出来的字符串放入 Map 中管理
    private val stringMap = mapOf(
        FakeR.string.copied_link to "链接已复制",
        FakeR.string.comment_copied to "评论已复制",
    )

    // 完美模拟原生的 getString 方法
    fun getString(resourceId: String): String {
        return stringMap[resourceId] ?: "未知文本[$resourceId]"
    }
}

/*
// 原代码（依赖 Android Context）
fun doSomething(context: Context) {
    val toastText = context.getString(R.string.copied_link)
}
// 新代码（完全断开 Android 依赖，可在纯 Kotlin/KMP 环境运行）
fun doSomething() {
    // 这里的 FakeR 和 FakeContext 都是你自己的纯 Kotlin 类
    val toastText = FakeContext.getString(FakeR.string.copied_link)
}
*/