//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Inflater

actual fun decompressDanmaku(data: ByteArray): ByteArray {
    val decompresser = Inflater(true)
    decompresser.reset()
    decompresser.setInput(data)
    val o = ByteArrayOutputStream(data.size)
    return try {
        val buf = ByteArray(2048)
        while (!decompresser.finished()) {
            val i = decompresser.inflate(buf)
            if (i == 0) break
            o.write(buf, 0, i)
        }
        o.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        data // 失败时返回原数据
    } finally {
        try {
            o.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        decompresser.end()
    }
}